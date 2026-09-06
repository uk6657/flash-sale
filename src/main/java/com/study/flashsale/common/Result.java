package com.study.flashsale.common;

import com.study.flashsale.context.TraceContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    private String traceId;

    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null, TraceContext.getTraceId());
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, TraceContext.getTraceId());
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null, TraceContext.getTraceId());
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null, TraceContext.getTraceId());
    }
}
