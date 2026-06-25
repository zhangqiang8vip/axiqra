package com.axiqra.common.domain.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

/**
 * 贡献类型枚举
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Getter
public enum ContributionType {

    // Solution 相关
    SOLUTION_CREATE("solution_create", "创建 Solution", 10),
    SOLUTION_VERIFY("solution_verify", "验证 Solution", 5),
    SOLUTION_IMPROVE("solution_improve", "改进 Solution", 8),

    // Case 相关
    CASE_CREATE("case_create", "创建 Case", 15),
    CASE_PUBLISH("case_publish", "发布 Case", 20),
    CASE_REVIEW("case_review", "审核 Case", 10),

    // Trace 相关
    TRACE_SUBMIT("trace_submit", "提交 Trace", 5),
    TRACE_CONFIRM("trace_confirm", "确认 Trace", 3),

    // Feedback 相关
    FEEDBACK_WORKED("feedback_worked", "标记 worked", 2),
    FEEDBACK_FAILED("feedback_failed", "标记 failed", 2),
    FEEDBACK_PARTIAL("feedback_partial", "标记 partial", 2),

    // 审核相关
    REVIEW_APPROVE("review_approve", "审核通过", 8),
    REVIEW_REJECT("review_reject", "审核拒绝", 5),

    // 其他
    REPORT_ISSUE("report_issue", "报告问题", 3),
    INVITE_USER("invite_user", "邀请用户", 10);

    @EnumValue
    private final String code;
    private final String desc;
    private final int basePoints;

    ContributionType(String code, String desc, int basePoints) {
        this.code = code;
        this.desc = desc;
        this.basePoints = basePoints;
    }

    public static ContributionType of(String code) {
        if (code == null) return null;
        for (ContributionType ct : values()) {
            if (ct.code.equals(code)) return ct;
        }
        return null;
    }

    /**
     * 计算实际积分（考虑质量系数）
     */
    public int calculatePoints(double qualityFactor) {
        return (int) (basePoints * qualityFactor);
    }
}
