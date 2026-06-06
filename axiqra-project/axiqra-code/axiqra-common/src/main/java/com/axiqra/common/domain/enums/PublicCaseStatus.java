package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Public Case 状态枚举
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum PublicCaseStatus {

    CANDIDATE("candidate", "候选"),
    REVIEWING("reviewing", "审核中"),
    VERIFIED("verified", "已验证"),
    STABLE("stable", "稳定"),
    CANONICAL("canonical", "权威"),
    ARCHIVED("archived", "已归档");

    @EnumValue
    private final String code;
    private final String desc;

    PublicCaseStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PublicCaseStatus of(String code) {
        if (code == null) return null;
        for (PublicCaseStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

    /** 是否可被公开搜索 */
    public boolean isPubliclySearchable() {
        return this == VERIFIED || this == STABLE || this == CANONICAL;
    }
}
