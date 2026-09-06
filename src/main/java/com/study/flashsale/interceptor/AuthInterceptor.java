package com.study.flashsale.interceptor;

import com.study.flashsale.common.ErrorCode;
import com.study.flashsale.context.UserContext;
import com.study.flashsale.enums.UserRole;
import com.study.flashsale.exception.BusinessException;
import com.study.flashsale.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.AntPathMatcher;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final AdminApiRule[] ADMIN_API_RULES = {
            new AdminApiRule("*", "/api/admin/**"),
            new AdminApiRule("POST", "/api/products"),
            new AdminApiRule("PUT", "/api/products/*"),
            new AdminApiRule("POST", "/api/activities"),
            new AdminApiRule("PUT", "/api/activities/*"),
            new AdminApiRule("POST", "/api/activities/*/prepare-stock"),
            new AdminApiRule("GET", "/api/logs"),
            new AdminApiRule("*", "/api/test/**")
    };

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String authorization = request.getHeader("Authorization");

        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String token = authorization.substring(7);

        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            UserContext.set(userId, username, role);
            if (isAdminApi(request) && !UserRole.isAdmin(UserContext.getRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }

            return true;
        } catch (Exception e) {
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                @Nullable Exception ex) {
        UserContext.clear();
    }

    private boolean isAdminApi(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        for (AdminApiRule rule : ADMIN_API_RULES) {
            if (matchesMethod(rule.method(), method) && pathMatcher.match(rule.pattern(), uri)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesMethod(String ruleMethod, String requestMethod) {
        return "*".equals(ruleMethod) || ruleMethod.equals(requestMethod);
    }

    private record AdminApiRule(String method, String pattern) {
    }
}
