package com.study.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.study.flashsale.common.ErrorCode;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.MqDeadMessage;
import com.study.flashsale.enums.MqDeadMessageStatus;
import com.study.flashsale.exception.BusinessException;
import com.study.flashsale.mapper.MqDeadMessageMapper;
import com.study.flashsale.mq.producer.SeckillOrderProducer;
import com.study.flashsale.service.MqDeadMessageService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.vo.MqDeadMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MqDeadMessageServiceImpl extends ServiceImpl<MqDeadMessageMapper, MqDeadMessage> implements MqDeadMessageService {

    private final SeckillOrderProducer seckillOrderProducer;

    @Override
    public void record(MqDeadMessage deadMessage) {
        save(deadMessage);
    }

    @Override
    public PageResponse<MqDeadMessageVO> listDeadMessages(PageRequest request) {
        LambdaQueryWrapper<MqDeadMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(MqDeadMessage::getId);

        Page<MqDeadMessage> page = page(new Page<>(request.getCurrent(), request.getSize()), queryWrapper);

        List<MqDeadMessageVO> records = page.getRecords().stream()
                .map(this::toMqDeadMessageVO)
                .toList();

        return new PageResponse<>(
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                records
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requeue(Long id) {
        MqDeadMessage deadMessage = getUnhandledDeadMessage(id);
        seckillOrderProducer.resendDeadOrderMessage(deadMessage);
        markHandled(id, "管理员手动重投死信消息");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markHandled(Long id) {
        getUnhandledDeadMessage(id);
        markHandled(id, "管理员手动标记死信消息已处理");
    }

    private MqDeadMessageVO toMqDeadMessageVO(MqDeadMessage deadMessage) {
        return new MqDeadMessageVO(
                IdUtil.toString(deadMessage.getId()),
                deadMessage.getMessageId(),
                deadMessage.getQueueName(),
                deadMessage.getExchangeName(),
                deadMessage.getRoutingKey(),
                deadMessage.getMessageBody(),
                deadMessage.getFailReason(),
                deadMessage.getRetryCount(),
                deadMessage.getStatus(),
                MqDeadMessageStatus.getTextByCode(deadMessage.getStatus()),
                deadMessage.getCreatedAt(),
                deadMessage.getUpdatedAt()
        );
    }

    private MqDeadMessage getUnhandledDeadMessage(Long id) {
        MqDeadMessage deadMessage = getById(id);
        if (deadMessage == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "死信消息不存在");
        }

        if (!MqDeadMessageStatus.UNHANDLED.getCode().equals(deadMessage.getStatus())) {
            throw new BusinessException("死信消息已处理，不能重复操作");
        }

        return deadMessage;
    }

    private void markHandled(Long id, String reason) {
        LambdaUpdateWrapper<MqDeadMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MqDeadMessage::getId, id);
        updateWrapper.eq(MqDeadMessage::getStatus, MqDeadMessageStatus.UNHANDLED.getCode());
        updateWrapper.set(MqDeadMessage::getStatus, MqDeadMessageStatus.HANDLED.getCode());
        updateWrapper.set(MqDeadMessage::getFailReason, limitReason(reason));

        int updateRows = baseMapper.update(null, updateWrapper);
        if (updateRows == 0) {
            throw new BusinessException("死信消息状态已变化，请刷新后重试");
        }
    }

    private String limitReason(String reason) {
        if (reason == null) {
            return "";
        }
        return reason.length() <= 512 ? reason : reason.substring(0, 512);
    }
}
