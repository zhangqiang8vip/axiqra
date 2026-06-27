package com.axiqra.core.adapter;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.audit.AuditPort.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 无操作审计适配器
 * <p>
 * 当审计功能禁用时（audit-datasource.enabled=false），提供空实现，
 * 确保系统可以正常运行而不依赖审计数据库。
 *
 * @author Axiqra Team
 * @date 2026-06-26
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "axiqra.audit-datasource", name = "enabled", havingValue = "false", matchIfMissing = false)
public class NoOpAuditAdapter implements AuditPort {

    public NoOpAuditAdapter() {
        log.info("[Audit] Using NoOpAuditAdapter - audit logging disabled");
    }

    @Override
    public void log(AuditEvent event) {
        log.debug("[Audit] Audit logging disabled - skipping event: {}", event.action());
    }

    @Override
    public void logPolicyDecision(PolicyDecisionEvent event) {
        log.debug("[Audit] Audit logging disabled - skipping policy decision: {}", event.requestId());
    }

    @Override
    public void logInvocation(InvocationEvent event) {
        log.debug("[Audit] Audit logging disabled - skipping invocation: {}", event.requestId());
    }

    @Override
    public void logReviewDecision(ReviewDecisionEvent event) {
        log.debug("[Audit] Audit logging disabled - skipping review decision: {}", event.requestId());
    }

    @Override
    public void logQuotaEvent(QuotaEvent event) {
        log.debug("[Audit] Audit logging disabled - skipping quota event: {}", event.requestId());
    }

    @Override
    public void logRateLimitEvent(RateLimitEvent event) {
        log.debug("[Audit] Audit logging disabled - skipping rate limit event: {}", event.requestId());
    }

    @Override
    public void logAuthorizationChange(AuthorizationEvent event) {
        log.debug("[Audit] Audit logging disabled - skipping authorization change: {}", event.requestId());
    }

    @Override
    public void logToolModelAttribution(ToolModelAttributionEvent event) {
        log.debug("[Audit] Audit logging disabled - skipping tool model attribution: {}", event.requestId());
    }
}
