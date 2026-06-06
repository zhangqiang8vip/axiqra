package com.axiqra.core.adapter;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.audit.AuditPort.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * PostgreSQL 审计日志适配器
 * <p>
 * 将审计事件写入 PostgreSQL 审计库（8 张 append-only 表）。
 * 使用 JdbcTemplate 实现，避免引入额外依赖。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditAdapter implements AuditPort {

    private final JdbcTemplate auditJdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void log(AuditEvent event) {
        String sql = """
            INSERT INTO axiqra_audit_event
                (request_id, actor_id, actor_type, action, object_type, object_id,
                 result, ip_address, user_agent, payload, tenant_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
            """;
        try {
            auditJdbcTemplate.update(sql,
                    event.requestId(),
                    event.actorId(),
                    event.actorType(),
                    event.action(),
                    event.objectType(),
                    event.objectId(),
                    event.result(),
                    event.ipAddress(),
                    event.userAgent(),
                    toJson(event.payload()),
                    event.tenantId()
            );
        } catch (Exception e) {
            log.error("【审计】写入通用审计事件失败，action={}, actorId={}",
                    event.action(), event.actorId(), e);
        }
    }

    @Override
    public void logPolicyDecision(PolicyDecisionEvent event) {
        String sql = """
            INSERT INTO axiqra_policy_decision_log
                (request_id, subject, object, action, decision,
                 policy_code, policy_version, reason_code, tenant_id)
            VALUES (?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?, ?)
            """;
        try {
            auditJdbcTemplate.update(sql,
                    event.requestId(),
                    toJson(event.subject()),
                    toJson(event.object()),
                    event.action(),
                    event.decision(),
                    event.policyCode(),
                    event.policyVersion(),
                    event.reasonCode(),
                    event.tenantId()
            );
        } catch (Exception e) {
            log.error("【审计】写入策略决策日志失败，requestId={}", event.requestId(), e);
        }
    }

    @Override
    public void logInvocation(InvocationEvent event) {
        String sql = """
            INSERT INTO axiqra_invocation_log
                (request_id, channel, tool_type, caller_id, target_type, target_id,
                 workspace_id, risk_level, confirmation_obtained, result, latency_ms, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try {
            auditJdbcTemplate.update(sql,
                    event.requestId(),
                    event.channel(),
                    event.toolType(),
                    event.callerId(),
                    event.targetType(),
                    event.targetId(),
                    event.workspaceId(),
                    event.riskLevel(),
                    event.confirmationObtained() != null && event.confirmationObtained() ? 1 : 0,
                    event.result(),
                    event.latencyMs(),
                    event.status()
            );
        } catch (Exception e) {
            log.error("【审计】写入调用日志失败，requestId={}", event.requestId(), e);
        }
    }

    @Override
    public void logReviewDecision(ReviewDecisionEvent event) {
        String sql = """
            INSERT INTO axiqra_review_decision_log
                (request_id, review_id, reviewer_id, decision, risk_level,
                 reason_code, evidence_snapshot, tenant_id)
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?)
            """;
        try {
            auditJdbcTemplate.update(sql,
                    event.requestId(),
                    event.reviewId(),
                    event.reviewerId(),
                    event.decision(),
                    event.riskLevel(),
                    event.reasonCode(),
                    toJson(event.evidenceSnapshot()),
                    event.tenantId()
            );
        } catch (Exception e) {
            log.error("【审计】写入审核决策日志失败，requestId={}", event.requestId(), e);
        }
    }

    @Override
    public void logQuotaEvent(QuotaEvent event) {
        String sql = """
            INSERT INTO axiqra_quota_event
                (request_id, subject_key, quota_type, amount, consumed, remaining, reset_at, result)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try {
            auditJdbcTemplate.update(sql,
                    event.requestId(),
                    event.subjectKey(),
                    event.quotaType(),
                    event.amount(),
                    event.consumed(),
                    event.remaining(),
                    event.resetAt(),
                    event.result()
            );
        } catch (Exception e) {
            log.error("【审计】写入 Quota 事件失败，requestId={}", event.requestId(), e);
        }
    }

    @Override
    public void logRateLimitEvent(RateLimitEvent event) {
        String sql = """
            INSERT INTO axiqra_rate_limit_event
                (request_id, limiter_key, window_key, limit_value,
                 current_count, result, retry_after)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try {
            auditJdbcTemplate.update(sql,
                    event.requestId(),
                    event.limiterKey(),
                    event.windowKey(),
                    event.limitValue(),
                    event.currentCount(),
                    event.result(),
                    event.retryAfter()
            );
        } catch (Exception e) {
            log.error("【审计】写入限流事件失败，requestId={}", event.requestId(), e);
        }
    }

    @Override
    public void logAuthorizationChange(AuthorizationEvent event) {
        String sql = """
            INSERT INTO axiqra_authorization_audit_log
                (request_id, authorization_id, from_state, to_state, actor_id, license_scope)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try {
            auditJdbcTemplate.update(sql,
                    event.requestId(),
                    event.authorizationId(),
                    event.fromState(),
                    event.toState(),
                    event.actorId(),
                    event.licenseScope()
            );
        } catch (Exception e) {
            log.error("【审计】写入授权变更日志失败，requestId={}", event.requestId(), e);
        }
    }

    @Override
    public void logToolModelAttribution(ToolModelAttributionEvent event) {
        String sql = """
            INSERT INTO axiqra_tool_model_attribution_log
                (request_id, solution_id, tool_name, reported_model_name, source, result)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try {
            auditJdbcTemplate.update(sql,
                    event.requestId(),
                    event.solutionId(),
                    event.toolName(),
                    event.reportedModelName(),
                    event.source(),
                    event.result()
            );
        } catch (Exception e) {
            log.error("【审计】写入工具模型归因日志失败，requestId={}", event.requestId(), e);
        }
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("【审计】JSON 序列化失败", e);
            return null;
        }
    }
}
