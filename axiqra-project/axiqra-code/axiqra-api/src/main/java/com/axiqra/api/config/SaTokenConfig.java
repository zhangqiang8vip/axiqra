package com.axiqra.api.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.interceptor.ScopeCheckInterceptor;
import com.axiqra.api.interceptor.WorkspaceRoleCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 *
 * <p>集成 Sa-Token + Redis 实现分布式会话。
 * 注册两个权限拦截器：
 * <ul>
 *   <li>ScopeCheckInterceptor：校验 @RequireScope 注解</li>
 *   <li>WorkspaceRoleCheckInterceptor：校验 @RequireWorkspaceRole 注解</li>
 * </ul>
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private final ScopeCheckInterceptor scopeCheckInterceptor;
    private final WorkspaceRoleCheckInterceptor workspaceRoleCheckInterceptor;

    public SaTokenConfig(ScopeCheckInterceptor scopeCheckInterceptor,
                         WorkspaceRoleCheckInterceptor workspaceRoleCheckInterceptor) {
        this.scopeCheckInterceptor = scopeCheckInterceptor;
        this.workspaceRoleCheckInterceptor = workspaceRoleCheckInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Sa-Token 登录拦截器
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/internal/health",
                        "/internal/health/verify",
                        "/api/internal/health/**",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/auth/login",
                        "/auth/register",
                        "/auth/captcha"
                );

        // Scope 权限校验拦截器
        registry.addInterceptor(scopeCheckInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/internal/health/**",
                        "/api/internal/health/**",
                        "/actuator/health/**",
                        "/auth/login",
                        "/auth/register",
                        "/auth/logout",
                        "/auth/me"
                );

        // Workspace 角色校验拦截器
        registry.addInterceptor(workspaceRoleCheckInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/internal/health/**",
                        "/api/internal/health/**",
                        "/actuator/health/**",
                        "/auth/login",
                        "/auth/register",
                        "/auth/logout",
                        "/auth/me"
                );
    }
}
