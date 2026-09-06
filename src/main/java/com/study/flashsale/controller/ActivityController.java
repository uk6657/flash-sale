package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.annotation.PreventDuplicateSubmit;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.request.ActivityCreateRequest;
import com.study.flashsale.dto.request.ActivityUpdateRequest;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.dto.response.SeckillResponse;
import com.study.flashsale.dto.response.SeckillResultResponse;
import com.study.flashsale.service.FlashSaleActivityService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.vo.ActivityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "秒杀活动", description = "活动管理、库存预热、参与秒杀与结果查询")
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final FlashSaleActivityService activityService;

    @Operation(summary = "创建秒杀活动")
    @LogOperation("创建秒杀活动")
    @PreventDuplicateSubmit
    @PostMapping
    public Result<ActivityVO> createActivity(@Valid @RequestBody ActivityCreateRequest request) {
        return Result.success(activityService.createActivity(request));
    }

    @Operation(summary = "修改秒杀活动")
    @LogOperation("修改秒杀活动")
    @PreventDuplicateSubmit
    @PutMapping("/{id}")
    public Result<ActivityVO> updateActivity(@PathVariable String id, @Valid @RequestBody ActivityUpdateRequest request) {
        return Result.success(activityService.updateActivity(IdUtil.parseId(id), request));
    }

    @Operation(summary = "查询秒杀活动列表")
    @LogOperation("查询秒杀活动列表")
    @GetMapping
    public Result<PageResponse<ActivityVO>> listActivities(@Valid PageRequest request) {
        return Result.success(activityService.listActivities(request));
    }

    @Operation(summary = "查询秒杀活动详情")
    @LogOperation("查询秒杀活动详情")
    @GetMapping("/{id}")
    public Result<ActivityVO> getActivityDetail(@PathVariable String id) {
        return Result.success(activityService.getActivityDetail(IdUtil.parseId(id)));
    }

    @Operation(summary = "预热秒杀库存到 Redis")
    @LogOperation("预热秒杀库存")
    @PreventDuplicateSubmit
    @PostMapping("/{id}/prepare-stock")
    public Result<Void> prepareStock(@PathVariable String id) {
        activityService.prepareStock(IdUtil.parseId(id));
        return Result.success();
    }

    @Operation(summary = "参与秒杀")
    @LogOperation("参与秒杀")
    @PreventDuplicateSubmit(seconds = 2, message = "秒杀请求提交过快，请稍后再试")
    @PostMapping("/{id}/seckill")
    public Result<SeckillResponse> seckill(@PathVariable String id) {
        SeckillResponse response = activityService.seckill(IdUtil.parseId(id));
        return Result.success(response);
    }

    @Operation(summary = "查询秒杀结果")
    @LogOperation("查询秒杀结果")
    @GetMapping("/{id}/result")
    public Result<SeckillResultResponse> getSeckillResult(@PathVariable String id) {
        SeckillResultResponse response = activityService.getSeckillResult(IdUtil.parseId(id));
        return Result.success(response);
    }
}
