package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.service.MqMessageService;
import com.study.flashsale.vo.MqMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-MQ本地消息")
@RestController
@RequestMapping("/api/admin/mq-messages")
@RequiredArgsConstructor
public class AdminMqMessageController {

    private final MqMessageService mqMessageService;

    @Operation(summary = "分页查询本地消息表")
    @LogOperation("查询MQ本地消息列表")
    @GetMapping
    public Result<PageResponse<MqMessageVO>> listMessages(@Valid PageRequest request) {
        return Result.success(mqMessageService.listMessages(request));
    }
}
