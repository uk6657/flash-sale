package com.study.flashsale.controller;

import com.study.flashsale.common.Result;
import com.study.flashsale.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "健康检查", description = "服务探活与异常演示")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "健康检查")
    @GetMapping
    public Result<Map<String, String>> health() {
        return Result.success(Map.of("status", "ok"));
    }

    @Operation(summary = "触发业务异常（联调/演示用）")
    @GetMapping("/error")
    public Result<Void> error() {
        throw new BusinessException("这是一条测试业务异常");
    }
}
