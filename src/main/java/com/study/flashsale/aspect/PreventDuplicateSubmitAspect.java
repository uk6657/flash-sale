package com.study.flashsale.aspect;

import com.study.flashsale.annotation.PreventDuplicateSubmit;
import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.context.UserContext;
import com.study.flashsale.exception.BusinessException;
import com.study.flashsale.infrastructure.redis.RedisService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PreventDuplicateSubmitAspect {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(preventDuplicateSubmit)")
    public Object preventDuplicateSubmit(ProceedingJoinPoint joinPoint,
                                         PreventDuplicateSubmit preventDuplicateSubmit) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        Long userId = UserContext.getUserId();
        if (userId == null) {
            return joinPoint.proceed();
        }

        String paramHash = hash(buildParamText(joinPoint));
        String duplicateKey = RedisKeyConstants.duplicateSubmit(
                userId,
                request.getMethod(),
                request.getRequestURI(),
                paramHash
        );

        Boolean firstSubmit = redisService.setIfAbsent(
                duplicateKey,
                "1",
                Duration.ofSeconds(preventDuplicateSubmit.seconds())
        );

        if (Boolean.FALSE.equals(firstSubmit)) {
            log.warn("重复提交被拦截，userId={}, method={}, uri={}",
                    userId, request.getMethod(), request.getRequestURI());
            throw new BusinessException(preventDuplicateSubmit.message());
        }

        return joinPoint.proceed();
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private String buildParamText(ProceedingJoinPoint joinPoint) {
        List<Object> args = Arrays.stream(joinPoint.getArgs())
                .filter(arg -> !(arg instanceof ServletRequest))
                .filter(arg -> !(arg instanceof ServletResponse))
                .toList();

        try {
            return objectMapper.writeValueAsString(args);
        } catch (JacksonException e) {
            log.warn("重复提交参数序列化失败，使用参数toString结果降级生成摘要", e);
            return args.toString();
        }
    }

    private String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("生成重复提交摘要失败");
        }
    }
}

