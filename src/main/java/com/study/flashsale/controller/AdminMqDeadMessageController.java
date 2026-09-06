package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.service.MqDeadMessageService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.vo.MqDeadMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-MQ死信")
@RestController
@RequestMapping("/api/admin/mq-dead-messages")
@RequiredArgsConstructor
public class AdminMqDeadMessageController {

    private final MqDeadMessageService mqDeadMessageService;

    @Operation(summary = "分页查询死信消息")
    @LogOperation("查询MQ死信消息列表")
    @GetMapping
    public Result<PageResponse<MqDeadMessageVO>> listDeadMessages(@Valid PageRequest request) {
        return Result.success(mqDeadMessageService.listDeadMessages(request));
    }

    @Operation(summary = "重新投递死信消息")
    @LogOperation("重新投递MQ死信消息")
    @PostMapping("/{id}/requeue")
    public Result<Void> requeueDeadMessage(@PathVariable String id) {
        mqDeadMessageService.requeue(IdUtil.parseId(id));
        return Result.success();
    }

    @Operation(summary = "标记死信消息已处理")
    @LogOperation("标记MQ死信消息已处理")
    @PostMapping("/{id}/handled")
    public Result<Void> markDeadMessageHandled(@PathVariable String id) {
        mqDeadMessageService.markHandled(IdUtil.parseId(id));
        return Result.success();
    }
}
