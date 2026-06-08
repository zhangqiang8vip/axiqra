package com.axiqra.common.port;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 告警端口接口
 *
 * <p>当系统发生需要人工关注的高风险事件时，通过此接口发送告警。
 * 当前实现为 HTTP Webhook（可扩展为邮件、Slack、DingTalk 等）。
 *
 * <p>触发场景：
 * <ul>
 *   <li>高风险操作被策略引擎拒绝</li>
 *   <li>用户配额用尽</li>
 *   <li>内容审核被拒绝</li>
 *   <li>授权撤回</li>
 * </ul>
 *
 * @author Axiqra Team
 * @date 2026-06-09
 */
public interface AlertPort {

    /**
     * 发送告警事件
     *
     * <p>实现类应保证告警发送的可靠性：
     * 失败时应记录日志，不应影响主业务流程。
     *
     * @param event 告警事件
     */
    void sendAlert(AlertEvent event);

    // ==================== 告警事件 ====================

    record AlertEvent(
            /** 告警类型 */
            AlertType type,
            /** 告警级别 */
            Severity severity,
            /** 触发时间 */
            OffsetDateTime occurredAt,
            /** 触发人 ID */
            Long actorId,
            /** 触发人类型 */
            String actorType,
            /** 关联对象类型 */
            String objectType,
            /** 关联对象 ID */
            Long objectId,
            /** 关联工作空间 ID（可为 null） */
            Long workspaceId,
            /** 告警消息（人类可读） */
            String message,
            /** 告警原因码 */
            String reasonCode,
            /** 租户 ID（可为 null） */
            Long tenantId,
            /** 额外上下文；POLICY_DENIED 约定包含 action、policyCode */
            Map<String, Object> metadata
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AlertType type;
            private Severity severity = Severity.INFO;
            private OffsetDateTime occurredAt = OffsetDateTime.now();
            private Long actorId;
            private String actorType;
            private String objectType;
            private Long objectId;
            private Long workspaceId;
            private String message;
            private String reasonCode;
            private Long tenantId;
            private Map<String, Object> metadata;

            public Builder type(AlertType v) { this.type = v; return this; }
            public Builder severity(Severity v) { this.severity = v; return this; }
            public Builder occurredAt(OffsetDateTime v) { this.occurredAt = v; return this; }
            public Builder actorId(Long v) { this.actorId = v; return this; }
            public Builder actorType(String v) { this.actorType = v; return this; }
            public Builder objectType(String v) { this.objectType = v; return this; }
            public Builder objectId(Long v) { this.objectId = v; return this; }
            public Builder workspaceId(Long v) { this.workspaceId = v; return this; }
            public Builder message(String v) { this.message = v; return this; }
            public Builder reasonCode(String v) { this.reasonCode = v; return this; }
            public Builder tenantId(Long v) { this.tenantId = v; return this; }
            public Builder metadata(Map<String, Object> v) { this.metadata = v; return this; }

            public AlertEvent build() {
                if (type == null) {
                    throw new IllegalArgumentException("AlertType is required");
                }
                return new AlertEvent(
                        type, severity, occurredAt, actorId, actorType,
                        objectType, objectId, workspaceId, message,
                        reasonCode, tenantId, metadata);
            }
        }
    }

    /** 告警类型 */
    enum AlertType {
        /** 策略拒绝 */
        POLICY_DENIED,
        /** 配额耗尽 */
        QUOTA_EXCEEDED,
        /** 配额告警 */
        QUOTA_WARNING,
        /** 审核拒绝 */
        REVIEW_REJECTED,
        /** 授权撤回 */
        AUTHORIZATION_REVOKED,
        /** 高风险操作被用户确认 */
        HIGH_RISK_CONFIRMED,
        /** 安全异常 */
        SECURITY_ANOMALY
    }

    /** 告警级别 */
    enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }
}
