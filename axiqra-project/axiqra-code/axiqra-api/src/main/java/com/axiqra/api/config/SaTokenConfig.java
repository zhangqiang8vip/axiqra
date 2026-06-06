package com.axiqra.api.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 * <p>
 * 集成 Sa-Token + Redis 实现分布式会话。
 * Sa-Token 的 cookie/header 读取已在 application.yml 中配置。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/internal/health/**",
                        "/api/internal/health/**",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/auth/login",
                        "/auth/register",
                        "/auth/captcha"
                );
    }
}
