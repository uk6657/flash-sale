package com.study.flashsale.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.request.ActivityCreateRequest;
import com.study.flashsale.dto.request.ActivityUpdateRequest;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.dto.response.ActivitySummaryResponse;
import com.study.flashsale.dto.response.SeckillResponse;
import com.study.flashsale.dto.response.SeckillResultResponse;
import com.study.flashsale.entity.FlashSaleActivity;
import com.study.flashsale.vo.ActivityVO;

public interface FlashSaleActivityService extends IService<FlashSaleActivity> {

    ActivityVO createActivity(ActivityCreateRequest request);

    ActivityVO updateActivity(Long id, ActivityUpdateRequest request);

    PageResponse<ActivityVO> listActivities(PageRequest request);

    ActivityVO getActivityDetail(Long id);

    void prepareStock(Long id);

    SeckillResponse seckill(Long activityId);

    SeckillResultResponse getSeckillResult(Long activityId);

    ActivitySummaryResponse getActivitySummary(Long activityId);
}
