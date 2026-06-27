package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

import java.util.Map;
import java.util.HashMap;

/**
 * Review 审核队列枚举
 *
 * <p>7类队列设计：
 * <ul>
 *   <li>AUTO_PASS - 自动通过（低风险内容自动放行）</li>
 *   <li>AUTO_REJECT - 自动拒绝（极高风险内容自动拦截）</li>
 *   <li>LOW_RISK_SAMPLING - 低风险抽检</li>
 *   <li>HUMAN_REVIEW - 普通人工审核</li>
 *   <li>CERTIFIED_REVIEW - 认证审核（需具备认证资质的审核人员）</li>
 *   <li>DOMAIN_REVIEW - 领域审核（特定领域专家审核）</li>
 *   <li>APPEAL - 申诉队列</li>
 * </ul>
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Getter
public enum ReviewQueue {

    // ========== 新7类队列 ==========
    AUTO_PASS("auto_pass", "自动通过"),
    AUTO_REJECT("auto_reject", "自动拒绝"),
    LOW_RISK_SAMPLING("low_risk_sampling", "低风险抽检"),
    HUMAN_REVIEW("human_review", "普通人工审核"),
    CERTIFIED_REVIEW("certified_review", "认证审核"),
    DOMAIN_REVIEW("domain_review", "领域审核"),
    APPEAL("appeal", "申诉队列"),

    // ========== 兼容旧值（保留但标记为废弃）==========
    /** @deprecated 使用 HUMAN_REVIEW 替代 */
    @Deprecated
    HUMAN("human", "人工审核队列（兼容旧值）"),
    /** @deprecated 使用 LOW_RISK_SAMPLING 或对应自动队列替代 */
    @Deprecated
    AUTO("auto", "自动审核队列（兼容旧值）"),
    /** @deprecated 使用 HUMAN_REVIEW 替代 */
    @Deprecated
    QUARANTINED("quarantined", "隔离审核队列（兼容旧值）");

    @EnumValue
    private final String code;
    private final String desc;

    private static final Map<String, ReviewQueue> CODE_MAP = new HashMap<>();

    static {
        for (ReviewQueue q : values()) {
            CODE_MAP.put(q.code, q);
        }
    }

    ReviewQueue(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据 code 查找队列枚举。
     * 同时支持新旧 code（human→HUMAN_REVIEW 等自动映射）。
     */
    public static ReviewQueue of(String code) {
        if (code == null) return null;
        String normalized = code.trim().toLowerCase();
        // 先精确匹配
        ReviewQueue exact = CODE_MAP.get(normalized);
        if (exact != null) return exact;
        // 兼容旧值映射
        return switch (normalized) {
            case "human" -> HUMAN_REVIEW;
            case "auto" -> LOW_RISK_SAMPLING;
            case "quarantined" -> HUMAN_REVIEW;
            default -> null;
        };
    }

    /**
     * 判断是否为自动处理队列（无需人工介入）。
     */
    public boolean isAutoQueue() {
        return this == AUTO_PASS || this == AUTO_REJECT;
    }

    /**
     * 判断是否需要人工审核。
     */
    public boolean requiresHumanReview() {
        return this == HUMAN_REVIEW
            || this == CERTIFIED_REVIEW
            || this == DOMAIN_REVIEW
            || this == APPEAL;
    }

    /**
     * 判断是否为申诉队列。
     */
    public boolean isAppealQueue() {
        return this == APPEAL;
    }
}
