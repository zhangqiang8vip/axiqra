package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Trace Package 状态枚举
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum TraceStatus {

    DRAFT("draft", "草稿"),
    USER_CONFIRMED("user_confirmed", "用户已确认"),
    SUBMITTED("submitted", "已提交"),
    NEEDS_REVIEW("needs_review", "待审核"),
    REVIEWED("reviewed", "已审核"),
    APPROVED("approved", "已批准"),
    QUARANTINED("quarantined", "已隔离");

    @EnumValue
    private final String code;
    private final String desc;

    TraceStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TraceStatus of(String code) {
        if (code == null) return null;
        for (TraceStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

    /** 是否已提交审核 */
    public boolean isSubmitted() {
        return this != DRAFT && this != USER_CONFIRMED;
    }
}
