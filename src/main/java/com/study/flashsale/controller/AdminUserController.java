package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.annotation.PreventDuplicateSubmit;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.request.RegisterRequest;
import com.study.flashsale.service.UserService;
import com.study.flashsale.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-用户")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "创建管理员账号")
    @LogOperation("创建管理员")
    @PreventDuplicateSubmit
    @PostMapping
    public Result<UserVO> createAdmin(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.createAdmin(request));
    }
}
