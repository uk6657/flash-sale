package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.service.MqConsumeLogService;
import com.study.flashsale.vo.MqConsumeLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-MQ消费日志")
@RestController
@RequestMapping("/api/admin/mq-consume-logs")
@RequiredArgsConstructor
public class AdminMqConsumeLogController {

    private final MqConsumeLogService mqConsumeLogService;

    @Operation(summary = "分页查询消费幂等日志")
    @LogOperation("查询MQ消费幂等日志列表")
    @GetMapping
    public Result<PageResponse<MqConsumeLogVO>> listConsumeLogs(@Valid PageRequest request) {
        return Result.success(mqConsumeLogService.listConsumeLogs(request));
    }
}
