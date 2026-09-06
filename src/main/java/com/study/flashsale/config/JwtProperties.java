package com.study.flashsale.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank(message = "JWT密钥不能为空")
    private String secret;

    @Min(value = 300, message = "JWT过期时间不能小于300秒")
    private Long expireSeconds = 7200L;
}
