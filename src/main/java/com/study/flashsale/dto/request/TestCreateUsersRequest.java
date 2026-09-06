package com.study.flashsale.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestCreateUsersRequest {

    @NotBlank(message = "用户名前缀不能为空")
    private String usernamePrefix = "test_user_";

    @Min(value = 1, message = "创建用户数量不能小于1")
    @Max(value = 1000, message = "单次最多创建1000个用户")
    private Integer count = 100;

    @NotBlank(message = "密码不能为空")
    private String password = "Password123";

    private String nicknamePrefix = "Test User ";
}
