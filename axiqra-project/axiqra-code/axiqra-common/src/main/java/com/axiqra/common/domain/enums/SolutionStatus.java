package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Solution 状态枚举
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum SolutionStatus {

    DRAFT("draft", "草稿"),
    CANDIDATE("candidate", "候选"),
    REVIEWED("reviewed", "已审查"),
    VERIFIED("verified", "已验证"),
    STABLE("stable", "稳定"),
    CANONICAL("canonical", "权威"),
    DEPRECATED("deprecated", "已废弃");

    @EnumValue
    private final String code;
    private final String desc;

    SolutionStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SolutionStatus of(String code) {
        if (code == null) return null;
        for (SolutionStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

    /** 是否允许被引用 */
    public boolean isReferencable() {
        return this != DRAFT && this != DEPRECATED;
    }
}
