package com.study.flashsale.controller;

import com.study.flashsale.annotation.PreventDuplicateSubmit;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.request.TestCreateUsersRequest;
import com.study.flashsale.dto.request.TestResetSeckillRequest;
import com.study.flashsale.dto.response.TestResetSeckillResponse;
import com.study.flashsale.dto.response.TestUserResponse;
import com.study.flashsale.service.TestSupportService;
import com.study.flashsale.util.IdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "测试辅助", description = "仅 dev/local 环境可用")
@Profile({"default", "local", "dev"})
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestSupportController {

    private final TestSupportService testSupportService;

    @Operation(summary = "重置秒杀活动状态（压测用）")
    @PostMapping("/activities/{activityId}/reset-seckill")
    @PreventDuplicateSubmit
    public Result<TestResetSeckillResponse> resetSeckill(
            @PathVariable String activityId,
            @Valid @RequestBody TestResetSeckillRequest request
    ) {
        return Result.success(testSupportService.resetSeckill(IdUtil.parseId(activityId), request));
    }

    @Operation(summary = "批量创建测试用户")
    @PostMapping("/users")
    @PreventDuplicateSubmit
    public Result<List<TestUserResponse>> createUsers(@Valid @RequestBody TestCreateUsersRequest request) {
        return Result.success(testSupportService.createUsers(request));
    }
}
