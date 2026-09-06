package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.request.LoginRequest;
import com.study.flashsale.dto.request.RegisterRequest;
import com.study.flashsale.dto.response.LoginResponse;
import com.study.flashsale.service.UserService;
import com.study.flashsale.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证", description = "注册、登录、当前用户")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @LogOperation("用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @LogOperation("用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    @Operation(summary = "获取当前用户信息")
    @LogOperation("获取当前用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        UserVO userVO = userService.getCurrentUser();
        return Result.success(userVO);
    }
}
