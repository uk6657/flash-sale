package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.response.ActivitySummaryResponse;
import com.study.flashsale.service.FlashSaleActivityService;
import com.study.flashsale.util.IdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-活动")
@RestController
@RequestMapping("/api/admin/activities")
@RequiredArgsConstructor
public class AdminActivityController {

    private final FlashSaleActivityService flashSaleActivityService;

    @Operation(summary = "查询活动汇总数据")
    @LogOperation("管理员查询活动汇总")
    @GetMapping("/{id}/summary")
    public Result<ActivitySummaryResponse> getActivitySummary(@PathVariable String id) {
        return Result.success(flashSaleActivityService.getActivitySummary(IdUtil.parseId(id)));
    }
}
