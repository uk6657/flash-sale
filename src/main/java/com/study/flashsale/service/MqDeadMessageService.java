package com.study.flashsale.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.MqDeadMessage;
import com.study.flashsale.vo.MqDeadMessageVO;

public interface MqDeadMessageService extends IService<MqDeadMessage> {

    void record(MqDeadMessage deadMessage);

    PageResponse<MqDeadMessageVO> listDeadMessages(PageRequest request);

    void requeue(Long id);

    void markHandled(Long id);
}
