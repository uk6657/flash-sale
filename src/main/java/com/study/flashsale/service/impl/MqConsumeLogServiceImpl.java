package com.study.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.message.SeckillOrderMessage;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.MqConsumeLog;
import com.study.flashsale.mapper.MqConsumeLogMapper;
import com.study.flashsale.service.FlashSaleResultService;
import com.study.flashsale.service.MqConsumeLogService;
import com.study.flashsale.service.OrderService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.vo.MqConsumeLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqConsumeLogServiceImpl extends ServiceImpl<MqConsumeLogMapper, MqConsumeLog> implements MqConsumeLogService {

    private static final String MESSAGE_TYPE_SECKILL_ORDER_CREATE = "SECKILL_ORDER_CREATE";

    private static final int STATUS_SUCCESS = 1;

    private final OrderService orderService;

    private final FlashSaleResultService flashSaleResultService;

    @Override
    public boolean hasConsumed(String messageId) {
        LambdaQueryWrapper<MqConsumeLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MqConsumeLog::getMessageId, messageId);
        return count(queryWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long consumeSeckillOrderMessage(SeckillOrderMessage message, String queueName) {
        if (hasConsumed(message.getMessageId())) {
            log.info("MQ message already consumed, messageId={}", message.getMessageId());
            return null;
        }

        Long orderId = orderService.createFlashSaleOrder(message.getUserId(), message.getActivityId());
        flashSaleResultService.markSuccess(message.getUserId(), message.getActivityId(), orderId);

        MqConsumeLog consumeLog = new MqConsumeLog();
        consumeLog.setMessageId(message.getMessageId());
        consumeLog.setQueueName(queueName);
        consumeLog.setMessageType(MESSAGE_TYPE_SECKILL_ORDER_CREATE);
        consumeLog.setOrderId(orderId);
        consumeLog.setStatus(STATUS_SUCCESS);
        consumeLog.setConsumeTime(LocalDateTime.now());

        try {
            save(consumeLog);
        } catch (DuplicateKeyException e) {
            log.info("MQ consume log already exists, messageId={}", message.getMessageId());
        }

        log.info("MQ message consumed and logged, messageId={}, orderId={}", message.getMessageId(), orderId);
        return orderId;
    }

    @Override
    public PageResponse<MqConsumeLogVO> listConsumeLogs(PageRequest request) {
        LambdaQueryWrapper<MqConsumeLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(MqConsumeLog::getId);

        Page<MqConsumeLog> page = page(new Page<>(request.getCurrent(), request.getSize()), queryWrapper);

        List<MqConsumeLogVO> records = page.getRecords().stream()
                .map(this::toMqConsumeLogVO)
                .toList();

        return new PageResponse<>(
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                records
        );
    }

    private MqConsumeLogVO toMqConsumeLogVO(MqConsumeLog consumeLog) {
        return new MqConsumeLogVO(
                IdUtil.toString(consumeLog.getId()),
                consumeLog.getMessageId(),
                consumeLog.getQueueName(),
                consumeLog.getMessageType(),
                IdUtil.toString(consumeLog.getOrderId()),
                consumeLog.getStatus(),
                consumeLog.getConsumeTime(),
                consumeLog.getCreatedAt()
        );
    }
}
