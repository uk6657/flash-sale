package com.study.flashsale.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.message.SeckillOrderMessage;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.MqMessage;
import com.study.flashsale.vo.MqMessageVO;

import java.util.List;

public interface MqMessageService extends IService<MqMessage> {

    MqMessage createPendingOrderMessage(SeckillOrderMessage message);

    void markSent(String messageId);

    void markSendFailed(String messageId, String failReason);

    List<MqMessage> listRetryableMessages(int batchSize);

    List<MqMessage> listFinalFailedMessages(int batchSize);

    MqMessage markRetrying(Long id);

    void markFinalFailedAndCompensate(MqMessage mqMessage, String failReason);

    PageResponse<MqMessageVO> listMessages(PageRequest request);
}
