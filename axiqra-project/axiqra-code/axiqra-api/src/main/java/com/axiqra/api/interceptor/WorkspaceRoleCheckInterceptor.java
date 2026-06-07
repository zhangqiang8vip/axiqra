package com.axiqra.api.interceptor;

import com.axiqra.api.annotation.RequireWorkspaceRole;
import com.axiqra.api.annotation.WorkspaceRole;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.RbacService;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * Workspace 角色校验拦截器
 *
 * <p>在方法执行前校验当前用户是否为指定工作空间的管理员/所有者。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkspaceRoleCheckInterceptor implements HandlerInterceptor {

    private final RbacService rbacService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RequireWorkspaceRole annotation = method.getMethodAnnotation(RequireWorkspaceRole.class);
        if (annotation == null) {
            return true;
        }

        long userId = StpUtil.getLoginIdAsLong();
        Long workspaceId = resolveWorkspaceId(annotation.workspaceParam(), request);
        if (workspaceId == null) {
            throw new BizException(ErrorCode.PARAM_MISSING, "workspaceId 参数缺失");
        }

        WorkspaceRole requiredRole = annotation.role();
        boolean hasRole = switch (requiredRole) {
            case OWNER -> rbacService.isOwner(userId, workspaceId);
            case ADMIN -> rbacService.isAdmin(userId, workspaceId);
        };

        if (!hasRole) {
            log.warn("Workspace 角色校验失败: userId={}, workspaceId={}, requiredRole={}",
                    userId, workspaceId, requiredRole.name());
            throw new BizException(ErrorCode.FORBIDDEN,
                    "需要 " + requiredRole.getDesc() + " 角色");
        }

        return true;
    }

    private Long resolveWorkspaceId(String paramName, HttpServletRequest request) {
        // First try path-variable map (handles @PathVariable natively, no -parameters needed)
        Map<String, String> pathVars = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVars != null && pathVars.containsKey(paramName)) {
            try {
                return Long.parseLong(pathVars.get(paramName));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        // Fallback to request parameter
        String value = request.getParameter(paramName);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
