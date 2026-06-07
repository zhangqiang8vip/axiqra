package com.axiqra.api.annotation;

import java.lang.annotation.*;

/**
 * Workspace 角色校验注解
 *
 * <p>用于 Controller 方法上，校验当前用户是否为指定工作空间的管理员/所有者。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireWorkspaceRole {

    /** 要求的角色 */
    WorkspaceRole role();

    /** 工作空间 ID 的参数名（从 @PathVariable 读取） */
    String workspaceParam() default "workspaceId";
}
