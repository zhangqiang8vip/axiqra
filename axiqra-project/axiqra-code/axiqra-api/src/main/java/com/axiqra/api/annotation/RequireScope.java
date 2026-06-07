package com.axiqra.api.annotation;

import java.lang.annotation.*;

/**
 * Scope 权限校验注解
 *
 * <p>用于 Controller 方法上，校验当前用户是否持有指定 scope。
 * 底层由 {@link com.axiqra.api.interceptor.ScopeCheckInterceptor} 处理。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireScope {

    /**
     * 必需的 scope，支持多个。
     * 匹配行为由 {@link RequireMode} 决定：
     * <ul>
     *   <li>{@link RequireMode#ANY}：任意一个 scope 满足即可（默认）</li>
     *   <li>{@link RequireMode#ALL}：全部 scope 都必须满足</li>
     * </ul>
     */
    String[] value();

    /** scope 之间的关系：ANY（任意一个满足）或 ALL（全部满足），默认 ANY */
    RequireMode mode() default RequireMode.ANY;

    enum RequireMode {
        ANY,   // 任意一个满足即可
        ALL    // 全部满足
    }
}
