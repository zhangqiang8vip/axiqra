package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * Engineering Trace 风险等级枚举
 *
 * <p>风险等级与审核队列路由映射：
 * <ul>
 *   <li>R0 (level=0) → AUTO_PASS（无风险，自动通过）</li>
 *   <li>R1 (level=1) → LOW_RISK_SAMPLING（低风险，抽检）</li>
 *   <li>R2 (level=2) → HUMAN_REVIEW（中风险，普通人工审核）</li>
 *   <li>R3 (level=3) → CERTIFIED_REVIEW（高风险，认证审核）</li>
 *   <li>R4 (level=4) → AUTO_REJECT（极高风险，自动拒绝）</li>
 * </ul>
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

    /**
     * 根据风险等级路由到对应的审核队列。
     *
     * @return 建议进入的审核队列
     */
    public ReviewQueue routeToReviewQueue() {
        return switch (this) {
            case R0 -> ReviewQueue.AUTO_PASS;
            case R1 -> ReviewQueue.LOW_RISK_SAMPLING;
            case R2 -> ReviewQueue.HUMAN_REVIEW;
            case R3 -> ReviewQueue.CERTIFIED_REVIEW;
            case R4 -> ReviewQueue.AUTO_REJECT;
        };
    }

    /**
     * 根据风险等级判断是否应自动通过。
     */
    public boolean shouldAutoPass() {
        return this == R0;
    }

    /**
     * 根据风险等级判断是否应自动拒绝。
     */
    public boolean shouldAutoReject() {
        return this == R4;
    }
}
