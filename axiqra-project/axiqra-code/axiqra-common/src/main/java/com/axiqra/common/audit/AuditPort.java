package com.axiqra.common.audit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 审计日志端口接口
 * <p>
 * 所有需要记录审计事件的模块通过此接口写入 PostgreSQL 审计库。
 * 底层实现为 JdbcTemplate，事务由调用方控制。
 * <p>
 * 使用方式：
 * <pre>
 * {@code
 * @Autowired
 * private AuditPort auditPort;
 *
 * auditPort.log(AuditEvent.builder()
 *     .actorId(userId)
 *     .action("user.login")
 *     .result("success")
 *     .build());
 * }
 * </pre>
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public interface AuditPort {

    /**
     * 记录通用审计事件
     */
    void log(AuditEvent event);

    /**
     * 记录策略决策日志
     */
    void logPolicyDecision(PolicyDecisionEvent event);

    /**
     * 记录调用日志
     */
    void logInvocation(InvocationEvent event);

    /**
     * 记录审核决策日志
     */
    void logReviewDecision(ReviewDecisionEvent event);

    /**
     * 记录 Quota 事件
     */
    void logQuotaEvent(QuotaEvent event);

    /**
     * 记录限流事件
     */
    void logRateLimitEvent(RateLimitEvent event);

    /**
     * 记录授权变更日志
     */
    void logAuthorizationChange(AuthorizationEvent event);

    /**
     * 记录工具模型归因日志
     */
    void logToolModelAttribution(ToolModelAttributionEvent event);

    // ==================== 事件对象 ====================

    record AuditEvent(
            String requestId,
            Long actorId,
            String actorType,
            String action,
            String objectType,
            Long objectId,
            String result,
            String ipAddress,
            String userAgent,
            Map<String, Object> payload,
            Long tenantId
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String requestId;
            private Long actorId;
            private String actorType;
            private String action;
            private String objectType;
            private Long objectId;
            private String result;
            private String ipAddress;
            private String userAgent;
            private Map<String, Object> payload;
            private Long tenantId;

            public Builder requestId(String v) { this.requestId = v; return this; }
            public Builder actorId(Long v) { this.actorId = v; return this; }
            public Builder actorType(String v) { this.actorType = v; return this; }
            public Builder action(String v) { this.action = v; return this; }
            public Builder objectType(String v) { this.objectType = v; return this; }
            public Builder objectId(Long v) { this.objectId = v; return this; }
            public Builder result(String v) { this.result = v; return this; }
            public Builder ipAddress(String v) { this.ipAddress = v; return this; }
            public Builder userAgent(String v) { this.userAgent = v; return this; }
            public Builder payload(Map<String, Object> v) { this.payload = v; return this; }
            public Builder tenantId(Long v) { this.tenantId = v; return this; }

            public AuditEvent build() {
                return new AuditEvent(
                        requestId, actorId, actorType, action, objectType,
                        objectId, result, ipAddress, userAgent, payload, tenantId);
            }
        }
    }

    record PolicyDecisionEvent(
            String requestId,
            Map<String, Object> subject,
            Map<String, Object> object,
            String action,
            String decision,
            String policyCode,
            String policyVersion,
            String reasonCode,
            Long tenantId
    ) {}

    record InvocationEvent(
            String requestId,
            String channel,
            String toolType,
            Long callerId,
            String targetType,
            Long targetId,
            Long workspaceId,
            String riskLevel,
            Boolean confirmationObtained,
            String result,
            Long latencyMs,
            String status
    ) {}

    record ReviewDecisionEvent(
            String requestId,
            Long reviewId,
            Long reviewerId,
            String decision,
            String riskLevel,
            String reasonCode,
            Map<String, Object> evidenceSnapshot,
            Long tenantId
    ) {}

    record QuotaEvent(
            String requestId,
            String subjectKey,
            String quotaType,
            Integer amount,
            Integer consumed,
            Integer remaining,
            OffsetDateTime resetAt,
            String result
    ) {}

    record RateLimitEvent(
            String requestId,
            String limiterKey,
            String windowKey,
            Integer limitValue,
            Integer currentCount,
            String result,
            Integer retryAfter
    ) {}

    record AuthorizationEvent(
            String requestId,
            Long authorizationId,
            String fromState,
            String toState,
            Long actorId,
            String licenseScope
    ) {}

    record ToolModelAttributionEvent(
            String requestId,
            Long solutionId,
            String toolName,
            String reportedModelName,
            String source,
            String result
    ) {}
}
