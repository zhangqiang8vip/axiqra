package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 可见范围枚举（visibility_scope 字段）
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum VisibilityScope {

    PRIVATE("private", "私有"),
    WORKSPACE("workspace", "工作空间内可见"),
    PUBLIC("public", "公开");

    @EnumValue
    private final String code;
    private final String desc;

    VisibilityScope(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static VisibilityScope of(String code) {
        if (code == null) return null;
        for (VisibilityScope v : values()) {
            if (v.code.equals(code)) return v;
        }
        return null;
    }

    /**
     * 当前可见范围是否包含对方（两人是否互相可见）。
     * 注意：WORKSPACE 互访需要 workspace ID 相同，PRIVATE 仅自己可见。
     * 此方法仅做类型级别的粗粒度判断，精确判断需在 service 层加入 workspace ID 参数。
     */
    public boolean canView(VisibilityScope other) {
        if (this == PUBLIC && other == PUBLIC) return true;
        if (other == PUBLIC) return this == PUBLIC;
        if (this == WORKSPACE && other == WORKSPACE) return true;
        if (other == WORKSPACE) return this == WORKSPACE;
        if (other == PRIVATE) return this == PRIVATE;
        return false;
    }
}
