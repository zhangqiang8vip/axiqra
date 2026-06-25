package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 审核原因码枚举
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Getter
public enum ReasonCode {

    // 通过原因
    APPROVED("approved", "审核通过"),
    APPROVED_WITH_NOTES("approved_with_notes", "通过但需备注"),

    // 拒绝原因
    INCOMPLETE("incomplete", "内容不完整"),
    INCORRECT("incorrect", "内容不正确"),
    DUPLICATE("duplicate", "内容重复"),
    SENSITIVE_INFO("sensitive_info", "包含敏感信息"),
    NO_AUTHORIZATION("no_authorization", "缺少授权"),
    VIOLATION("violation", "违反规则"),
    LOW_QUALITY("low_quality", "质量过低"),
    SPAM("spam", "垃圾内容"),

    // 隔离原因
    QUARANTINE_HIGH_RISK("quarantine_high_risk", "高风险内容隔离"),
    QUARANTINE_SUSPICIOUS("quarantine_suspicious", "可疑内容隔离"),
    QUARANTINE_REPORTED("quarantine_reported", "被举报内容隔离"),

    // 其他
    ESCALATE("escalate", "需要升级处理"),
    NEEDS_MORE_INFO("needs_more_info", "需要更多信息"),
    CANCELLED("cancelled", "已取消");

    @EnumValue
    private final String code;
    private final String desc;

    ReasonCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReasonCode of(String code) {
        if (code == null) return null;
        for (ReasonCode rc : values()) {
            if (rc.code.equals(code)) return rc;
        }
        return null;
    }

    public boolean isRejectCode() {
        return this == INCOMPLETE || this == INCORRECT || this == DUPLICATE
                || this == SENSITIVE_INFO || this == NO_AUTHORIZATION || this == VIOLATION
                || this == LOW_QUALITY || this == SPAM;
    }

    public boolean isQuarantineCode() {
        return this == QUARANTINE_HIGH_RISK || this == QUARANTINE_SUSPICIOUS || this == QUARANTINE_REPORTED;
    }

    public boolean isApproveCode() {
        return this == APPROVED || this == APPROVED_WITH_NOTES;
    }
}
