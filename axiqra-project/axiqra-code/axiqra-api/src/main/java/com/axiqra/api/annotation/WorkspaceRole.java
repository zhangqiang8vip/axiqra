package com.axiqra.api.annotation;

import lombok.Getter;

/**
 * Workspace 角色注解枚举
 *
 * <p>用于 {@link RequireWorkspaceRole} 注解，类型安全地指定所需的工作空间角色。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Getter
public enum WorkspaceRole {

    ADMIN("admin", "管理员"),
    OWNER("owner", "所有者");

    private final String code;
    private final String desc;

    WorkspaceRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
