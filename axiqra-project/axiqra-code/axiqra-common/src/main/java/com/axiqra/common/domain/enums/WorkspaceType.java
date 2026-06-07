package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 工作空间类型枚举
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum WorkspaceType {

    PERSONAL("personal", "个人空间"),
    ORGANIZATION("organization", "组织空间"),
    ENTERPRISE("enterprise", "企业空间"),
    GOVERNMENT("government", "政府空间");

    @EnumValue
    private final String code;
    private final String desc;

    WorkspaceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static WorkspaceType of(String code) {
        if (code == null) return null;
        String lower = code.trim().toLowerCase(java.util.Locale.ROOT);
        for (WorkspaceType t : values()) {
            if (t.code.equals(lower)) return t;
        }
        return null;
    }
}
