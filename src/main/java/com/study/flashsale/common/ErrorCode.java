package com.study.flashsale.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(1000, "业务异常"),
    SYSTEM_BUSY(1001, "系统繁忙，请稍后再试"),
    SERVICE_DEGRADED(1002, "服务暂时不可用，请稍后再试"),
    REDIS_UNAVAILABLE(1003, "缓存服务暂时不可用"),
    SYSTEM_ERROR(500, "系统异常");

    private final Integer code;

    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
