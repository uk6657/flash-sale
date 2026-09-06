package com.study.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.common.RabbitMqConstants;
import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.config.FlashSaleProperties;
import com.study.flashsale.dto.message.SeckillOrderMessage;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.MqMessage;
import com.study.flashsale.enums.MqMessageStatus;
import com.study.flashsale.infrastructure.redis.RedisService;
import com.study.flashsale.mapper.MqMessageMapper;
import com.study.flashsale.service.FlashSaleResultService;
import com.study.flashsale.service.MqMessageService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.vo.MqMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqMessageServiceImpl extends ServiceImpl<MqMessageMapper, MqMessage> implements MqMessageService {

    private static final String MESSAGE_TYPE_SECKILL_ORDER_CREATE = "SECKILL_ORDER_CREATE";

    private final ObjectMapper objectMapper;

    private final FlashSaleProperties flashSaleProperties;

    private final FlashSaleResultService flashSaleResultService;

    private final RedisService redisService;

    @Override
    public MqMessage createPendingOrderMessage(SeckillOrderMessage message) {
        try {
            MqMessage mqMessage = new MqMessage();
            mqMessage.setMessageId(message.getMessageId());
            mqMessage.setMessageType(MESSAGE_TYPE_SECKILL_ORDER_CREATE);
            mqMessage.setExchangeName(RabbitMqConstants.FLASH_SALE_EXCHANGE);
            mqMessage.setRoutingKey(RabbitMqConstants.FLASH_SALE_ORDER_ROUTING_KEY);
            mqMessage.setMessageBody(objectMapper.writeValueAsString(message));
            mqMessage.setStatus(MqMessageStatus.PENDING.getCode());
            mqMessage.setRetryCount(0);
            mqMessage.setFailReason("");
            mqMessage.setNextRetryTime(LocalDateTime.now());

            save(mqMessage);
            log.info("Local MQ message created, messageId={}, type={}", mqMessage.getMessageId(), mqMessage.getMessageType());
            return mqMessage;
        } catch (JacksonException e) {
            throw new RuntimeException("Create local MQ message failed", e);
        }
    }

    @Override
    public void markSent(String messageId) {
        LambdaUpdateWrapper<MqMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MqMessage::getMessageId, messageId);
        updateWrapper.eq(MqMessage::getStatus, MqMessageStatus.PENDING.getCode());
        updateWrapper.set(MqMessage::getStatus, MqMessageStatus.SENT.getCode());
        updateWrapper.set(MqMessage::getSentAt, LocalDateTime.now());
        updateWrapper.set(MqMessage::getFailReason, "");

        int updateRows = baseMapper.update(null, updateWrapper);
        log.info("Local MQ message marked sent, messageId={}, updateRows={}", messageId, updateRows);
    }

    @Override
    public void markSendFailed(String messageId, String failReason) {
        LambdaUpdateWrapper<MqMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MqMessage::getMessageId, messageId);
        updateWrapper.eq(MqMessage::getStatus, MqMessageStatus.PENDING.getCode());
        updateWrapper.set(MqMessage::getStatus, MqMessageStatus.SEND_FAILED.getCode());
        updateWrapper.set(MqMessage::getFailReason, limitReason(failReason));
        updateWrapper.set(MqMessage::getNextRetryTime, nextRetryTime());

        int updateRows = baseMapper.update(null, updateWrapper);
        log.warn("Local MQ message marked send failed, messageId={}, updateRows={}, reason={}",
                messageId, updateRows, failReason);
    }

    @Override
    public List<MqMessage> listRetryableMessages(int batchSize) {
        LambdaQueryWrapper<MqMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MqMessage::getStatus, MqMessageStatus.SEND_FAILED.getCode());
        queryWrapper.lt(MqMessage::getRetryCount, flashSaleProperties.getMq().getSendMaxRetryCount());
        queryWrapper.le(MqMessage::getNextRetryTime, LocalDateTime.now());
        queryWrapper.orderByAsc(MqMessage::getId);
        queryWrapper.last("LIMIT " + batchSize);

        return list(queryWrapper);
    }

    @Override
    public List<MqMessage> listFinalFailedMessages(int batchSize) {
        LambdaQueryWrapper<MqMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MqMessage::getStatus, MqMessageStatus.SEND_FAILED.getCode());
        queryWrapper.ge(MqMessage::getRetryCount, flashSaleProperties.getMq().getSendMaxRetryCount());
        queryWrapper.orderByAsc(MqMessage::getId);
        queryWrapper.last("LIMIT " + batchSize);

        return list(queryWrapper);
    }

    @Override
    public MqMessage markRetrying(Long id) {
        MqMessage mqMessage = getById(id);
        if (mqMessage == null || !MqMessageStatus.SEND_FAILED.getCode().equals(mqMessage.getStatus())) {
            return null;
        }

        int nextRetryCount = mqMessage.getRetryCount() + 1;
        LambdaUpdateWrapper<MqMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MqMessage::getId, id);
        updateWrapper.eq(MqMessage::getStatus, MqMessageStatus.SEND_FAILED.getCode());
        updateWrapper.set(MqMessage::getStatus, MqMessageStatus.PENDING.getCode());
        updateWrapper.set(MqMessage::getRetryCount, nextRetryCount);
        updateWrapper.set(MqMessage::getNextRetryTime, nextRetryTime());

        int updateRows = baseMapper.update(null, updateWrapper);
        if (updateRows == 0) {
            return null;
        }

        mqMessage.setStatus(MqMessageStatus.PENDING.getCode());
        mqMessage.setRetryCount(nextRetryCount);
        mqMessage.setNextRetryTime(nextRetryTime());
        return mqMessage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFinalFailedAndCompensate(MqMessage mqMessage, String failReason) {
        MqMessage latestMessage = getById(mqMessage.getId());
        if (latestMessage == null || !MqMessageStatus.SEND_FAILED.getCode().equals(latestMessage.getStatus())) {
            return;
        }

        SeckillOrderMessage orderMessage = parseOrderMessage(latestMessage.getMessageBody());
        flashSaleResultService.markFailed(orderMessage.getUserId(), orderMessage.getActivityId(), failReason);
        redisService.increment(RedisKeyConstants.flashSaleStock(orderMessage.getActivityId()));
        redisService.delete(RedisKeyConstants.flashSaleUser(orderMessage.getActivityId(), orderMessage.getUserId()));

        LambdaUpdateWrapper<MqMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MqMessage::getId, latestMessage.getId());
        updateWrapper.eq(MqMessage::getStatus, MqMessageStatus.SEND_FAILED.getCode());
        updateWrapper.set(MqMessage::getStatus, MqMessageStatus.FINAL_FAILED.getCode());
        updateWrapper.set(MqMessage::getFailReason, limitReason(failReason));

        baseMapper.update(null, updateWrapper);
        log.error("Local MQ message final failed and compensated, messageId={}, userId={}, activityId={}",
                latestMessage.getMessageId(), orderMessage.getUserId(), orderMessage.getActivityId());
    }

    @Override
    public PageResponse<MqMessageVO> listMessages(PageRequest request) {
        LambdaQueryWrapper<MqMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(MqMessage::getId);

        Page<MqMessage> page = page(new Page<>(request.getCurrent(), request.getSize()), queryWrapper);

        List<MqMessageVO> records = page.getRecords().stream()
                .map(this::toMqMessageVO)
                .toList();

        return new PageResponse<>(
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                records
        );
    }

    private SeckillOrderMessage parseOrderMessage(String messageBody) {
        try {
            return objectMapper.readValue(messageBody, SeckillOrderMessage.class);
        } catch (Exception e) {
            throw new RuntimeException("Parse local MQ message failed", e);
        }
    }

    private MqMessageVO toMqMessageVO(MqMessage mqMessage) {
        return new MqMessageVO(
                IdUtil.toString(mqMessage.getId()),
                mqMessage.getMessageId(),
                mqMessage.getMessageType(),
                mqMessage.getExchangeName(),
                mqMessage.getRoutingKey(),
                mqMessage.getMessageBody(),
                mqMessage.getStatus(),
                MqMessageStatus.getTextByCode(mqMessage.getStatus()),
                mqMessage.getRetryCount(),
                mqMessage.getFailReason(),
                mqMessage.getNextRetryTime(),
                mqMessage.getSentAt(),
                mqMessage.getCreatedAt(),
                mqMessage.getUpdatedAt()
        );
    }

    private String limitReason(String failReason) {
        if (failReason == null) {
            return "";
        }
        return failReason.length() <= 512 ? failReason : failReason.substring(0, 512);
    }

    private LocalDateTime nextRetryTime() {
        return LocalDateTime.now().plusNanos(flashSaleProperties.getMq().getSendRetryFixedDelayMs() * 1_000_000);
    }
}
