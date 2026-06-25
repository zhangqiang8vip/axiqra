package com.axiqra.core.event;

import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * 业务事件类型枚举
 */
@Getter
@FieldDefaults(makeFinal = true)
public enum EventType {

    // Trace 相关
    TRACE_CREATED("trace.created", "Trace 创建"),
    TRACE_CONFIRMED("trace.confirmed", "Trace 已确认"),
    TRACE_SUBMITTED("trace.submitted", "Trace 已提交"),

    // Solution 相关
    SOLUTION_CREATED("solution.created", "Solution 创建"),
    SOLUTION_STATUS_CHANGED("solution.status_changed", "Solution 状态变更"),
    SOLUTION_REVIEW_APPROVED("solution.review_approved", "Solution 审核通过"),
    SOLUTION_REVIEW_REJECTED("solution.review_rejected", "Solution 审核拒绝"),

    // Feedback 相关
    FEEDBACK_SUBMITTED("feedback.submitted", "反馈已提交"),

    // Review 相关
    REVIEW_CREATED("review.created", "审核任务创建"),
    REVIEW_APPROVED("review.approved", "审核通过"),
    REVIEW_REJECTED("review.rejected", "审核拒绝"),

    // Contribution 相关
    CONTRIBUTION_ADDED("contribution.added", "贡献积分增加"),

    // User 相关
    USER_LOGIN("user.login", "用户登录"),
    USER_REGISTERED("user.registered", "用户注册");

    private final String code;
    private final String description;

    EventType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
