package com.study.flashsale.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.FlashSaleResult;
import com.study.flashsale.vo.FlashSaleResultVO;

public interface FlashSaleResultService extends IService<FlashSaleResult> {

    void createQueuedResult(Long userId, Long activityId);

    FlashSaleResult getByUserAndActivity(Long userId, Long activityId);

    PageResponse<FlashSaleResultVO> listAllResults(PageRequest request);

    void markSuccess(Long userId, Long activityId, Long orderId);

    void markFailed(Long userId, Long activityId, String message);
}
