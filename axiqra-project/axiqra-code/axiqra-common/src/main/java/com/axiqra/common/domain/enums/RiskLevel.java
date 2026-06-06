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

    R0(0, "R0", "无风险", "无需审核"),
    R1(1, "R1", "低风险", "自动审核"),
    R2(2, "R2", "中风险", "人工抽检"),
    R3(3, "R3", "高风险", "人工必检"),
    R4(4, "R4", "极高风险", "禁止执行");

    private final int level;

    @EnumValue
    private final String code;
    private final String desc;
    private final String reviewPolicy;

    RiskLevel(int level, String code, String desc, String reviewPolicy) {
        this.level = level;
        this.code = code;
        this.desc = desc;
        this.reviewPolicy = reviewPolicy;
    }

    public static RiskLevel of(int level) {
        for (RiskLevel r : values()) {
            if (r.level == level) return r;
        }
        return null;
    }

    /**
     * 优先按精确 code 匹配（如 "R0"），fallback 按数字 level 解析。
     * 兼容 MyBatis-Flex @EnumValue 映射及旧数据。
     */
    public static RiskLevel ofLevel(String levelStr) {
        if (levelStr == null) return null;
        String trimmed = levelStr.trim();
        for (RiskLevel r : values()) {
            if (r.code.equalsIgnoreCase(trimmed)) return r;
        }
        try {
            return of(Integer.parseInt(trimmed));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 是否需要人工审核 */
    public boolean requiresHumanReview() {
        return level >= 2;
    }
}
