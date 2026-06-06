package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Engineering Trace 风险等级枚举
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum RiskLevel {

    R0(0, "无风险", "无需审核"),
    R1(1, "低风险", "自动审核"),
    R2(2, "中风险", "人工抽检"),
    R3(3, "高风险", "人工必检"),
    R4(4, "极高风险", "禁止执行");

    @EnumValue
    private final int level;
    private final String desc;
    private final String reviewPolicy;

    RiskLevel(int level, String desc, String reviewPolicy) {
        this.level = level;
        this.desc = desc;
        this.reviewPolicy = reviewPolicy;
    }

    public static RiskLevel of(int level) {
        for (RiskLevel r : values()) {
            if (r.level == level) return r;
        }
        return null;
    }

    public static RiskLevel ofLevel(String levelStr) {
        if (levelStr == null) return null;
        try {
            return of(Integer.parseInt(levelStr.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 是否需要人工审核 */
    public boolean requiresHumanReview() {
        return level >= 2;
    }
}
