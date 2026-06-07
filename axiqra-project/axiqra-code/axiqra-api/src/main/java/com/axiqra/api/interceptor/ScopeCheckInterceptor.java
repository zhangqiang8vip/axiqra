package com.axiqra.api.interceptor;

import com.axiqra.api.annotation.RequireScope;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.PolicyEngineService;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Scope 权限校验拦截器
 *
 * <p>在方法执行前校验当前用户是否持有注解声明的 scope。
 * 未登录用户直接放行（Sa-Token 拦截器会处理）。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScopeCheckInterceptor implements HandlerInterceptor {

    private final PolicyEngineService policyEngineService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RequireScope annotation = method.getMethodAnnotation(RequireScope.class);
        if (annotation == null) {
            return true;
        }

        long userId = StpUtil.getLoginIdAsLong();
        String[] requiredScopes = annotation.value();

        boolean satisfied = switch (annotation.mode()) {
            case ANY -> {
                for (String scope : requiredScopes) {
                    if (policyEngineService.hasScope(userId, scope)) {
                        yield true;
                    }
                }
                yield false;
            }
            case ALL -> {
                for (String scope : requiredScopes) {
                    if (!policyEngineService.hasScope(userId, scope)) {
                        yield false;
                    }
                }
                yield true;
            }
        };

        if (!satisfied) {
            log.warn("Scope 校验失败: userId={}, required={}, method={}",
                    userId, String.join(",", requiredScopes), method.getMethod().getName());
            throw new BizException(ErrorCode.FORBIDDEN,
                    "权限不足，需要 scope: " + String.join(",", requiredScopes));
        }

        return true;
    }
}
