package com.study.flashsale.aspect;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.context.TraceContext;
import com.study.flashsale.context.UserContext;
import com.study.flashsale.entity.OperationLog;
import com.study.flashsale.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @AfterReturning("@annotation(logOperation)")
    public void recordOperationLog(JoinPoint joinPoint, LogOperation logOperation) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();

            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(UserContext.getUserId());
            operationLog.setOperation(logOperation.value());
            operationLog.setRequestUri(request.getRequestURI());
            operationLog.setRequestMethod(request.getMethod());
            operationLog.setIp(request.getRemoteAddr());
            operationLog.setTraceId(TraceContext.getTraceId());

            operationLogService.record(operationLog);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
}
