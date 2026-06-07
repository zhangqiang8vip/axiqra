package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 策略决策结果枚举
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Getter
public enum PolicyDecision {

    ALLOW("allow", "允许"),
    DENY("deny", "拒绝"),
    DENY_CONDITION_NOT_MET("deny_condition_not_met", "条件不满足"),
    DENY_RISK_LEVEL_TOO_HIGH("deny_risk_level_too_high", "风险等级过高"),
    DENY_SCOPE_MISSING("deny_scope_missing", "缺少必要权限范围"),
    DENY_RATE_LIMITED("deny_rate_limited", "触发限流"),
    DENY_QUOTA_EXCEEDED("deny_quota_exceeded", "配额已用尽"),
    ABSTAIN("abstain", "弃权（无匹配策略）");

    @EnumValue
    private final String code;
    private final String desc;

    PolicyDecision(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public boolean isAllow() {
        return this == ALLOW;
    }

    public static PolicyDecision of(String code) {
        if (code == null) {
            return null;
        }
        for (PolicyDecision d : values()) {
            if (d.code.equals(code)) {
                return d;
            }
        }
        return null;
    }
}
