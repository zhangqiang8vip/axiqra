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
    NEEDS_REVIEW("needs_review", "待审核"),
    REJECTED("rejected", "已拒绝"),
    REVIEWED("reviewed", "已审查"),
    VERIFIED("verified", "已验证"),
    STABLE("stable", "稳定"),
    CANONICAL("canonical", "权威"),
    DEPRECATED("deprecated", "已废弃"),
    QUARANTINED("quarantined", "已隔离"),
    ARCHIVED("archived", "已归档");

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
        return this != DRAFT && this != DEPRECATED && this != REJECTED && this != QUARANTINED;
    }

    /** 是否需要审核才能推荐 */
    public boolean requiresReview() {
        return this == NEEDS_REVIEW;
    }

    /** 是否是公开可见状态 */
    public boolean isPublicVisible() {
        return switch (this) {
            case REVIEWED, VERIFIED, STABLE, CANONICAL -> true;
            default -> false;
        };
    }

    /** 是否是已废弃状态 */
    public boolean isDeprecated() {
        return this == DEPRECATED || this == ARCHIVED;
    }

    /** 是否是隔离状态 */
    public boolean isQuarantined() {
        return this == QUARANTINED;
    }
}
