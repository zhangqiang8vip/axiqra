package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Review 审核结果枚举
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum ReviewResult {

    PENDING("pending", "待审核"),
    IN_PROGRESS("in_progress", "审核中"),
    APPROVED("approved", "审核通过"),
    REJECTED("rejected", "审核拒绝"),
    QUARANTINED("quarantined", "隔离"),
    APPEAL_REJECTED("appeal_rejected", "申诉驳回"),
    APPEAL_IN_PROGRESS("appeal_in_progress", "申诉中");

    @EnumValue
    private final String code;
    private final String desc;

    ReviewResult(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReviewResult of(String code) {
        if (code == null) return null;
        for (ReviewResult r : values()) {
            if (r.code.equals(code)) return r;
        }
        return null;
    }

    /** 是否为终态（不可再变更） */
    public boolean isFinal() {
        return this == APPROVED || this == REJECTED || this == QUARANTINED || this == APPEAL_REJECTED;
    }
}
