package com.study.flashsale.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.message.SeckillOrderMessage;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.MqConsumeLog;
import com.study.flashsale.vo.MqConsumeLogVO;

public interface MqConsumeLogService extends IService<MqConsumeLog> {

    boolean hasConsumed(String messageId);

    Long consumeSeckillOrderMessage(SeckillOrderMessage message, String queueName);

    PageResponse<MqConsumeLogVO> listConsumeLogs(PageRequest request);
}
