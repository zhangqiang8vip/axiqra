package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.PolicyEvaluationRequest;
import com.axiqra.common.domain.enums.PolicyDecision;
import com.axiqra.common.domain.vo.PolicyEvaluationVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.port.AlertPort;
import com.axiqra.common.port.AlertPort.AlertEvent;
import com.axiqra.common.port.AlertPort.AlertType;
import com.axiqra.common.port.AlertPort.Severity;
import com.axiqra.common.port.PolicyEnginePort;
import com.axiqra.core.service.PolicyEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * ABAC 策略引擎服务实现
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyEngineServiceImpl implements PolicyEngineService {

    private static final String METADATA_ACTION = "action";
    private static final String METADATA_POLICY_CODE = "policyCode";

    private final PolicyEnginePort policyEnginePort;
    private final AlertPort alertPort;

    @Override
    public PolicyEvaluationVO evaluate(PolicyEvaluationRequest request) {
        return policyEnginePort.evaluate(request);
    }

    @Override
    public boolean hasScope(Long userId, String scope) {
        return policyEnginePort.hasScope(userId, scope);
    }

    @Override
    public void enforce(PolicyEvaluationRequest request) {
        PolicyEvaluationVO result = policyEnginePort.evaluate(request);
        if (!result.isAllowed()) {
            log.warn("策略强制拒绝: userId={}, action={}, policyCode={}, reason={}",
                    request.getSubjectId(), request.getAction(),
                    result.getPolicyCode(), result.getReasonCode());

            sendPolicyDeniedAlert(request, result);

            throw new BizException(resolveErrorCode(result),
                    "策略拒绝: " + result.getMessage());
        }
        log.debug("策略评估通过: userId={}, action={}, policyCode={}",
                request.getSubjectId(), request.getAction(), result.getPolicyCode());
    }

    private void sendPolicyDeniedAlert(PolicyEvaluationRequest request, PolicyEvaluationVO result) {
        Long workspaceId = resolveWorkspaceId(request.getContext());
        try {
            alertPort.sendAlert(buildPolicyDeniedAlert(request, result, workspaceId));
        } catch (RuntimeException e) {
            log.warn("策略拒绝告警发送失败: userId={}, workspaceId={}, action={}, message={}",
                    request.getSubjectId(), workspaceId, request.getAction(), messageOrUnknown(e));
        }
    }

    private AlertEvent buildPolicyDeniedAlert(PolicyEvaluationRequest request, PolicyEvaluationVO result,
                                              Long workspaceId) {
        return AlertEvent.builder()
                .type(AlertType.POLICY_DENIED)
                .severity(alertSeverity(result))
                .actorId(request.getSubjectId())
                .actorType("user")
                .objectType(request.getObjectType())
                .objectId(request.getObjectId())
                .workspaceId(workspaceId)
                .message("策略拒绝: " + result.getMessage())
                .reasonCode(result.getReasonCode())
                .metadata(Map.of(
                        METADATA_ACTION, request.getAction() != null ? request.getAction() : "",
                        METADATA_POLICY_CODE, result.getPolicyCode() != null ? result.getPolicyCode() : ""
                ))
                .build();
    }

    private Severity alertSeverity(PolicyEvaluationVO result) {
        return result.getDecision() == PolicyDecision.DENY_RISK_LEVEL_TOO_HIGH
                ? Severity.CRITICAL : Severity.WARNING;
    }

    private ErrorCode resolveErrorCode(PolicyEvaluationVO result) {
        PolicyDecision decision = result.getDecision();
        if (decision == null) {
            return ErrorCode.FORBIDDEN;
        }
        return switch (decision) {
            case DENY_RISK_LEVEL_TOO_HIGH -> ErrorCode.RISK_LEVEL_TOO_HIGH;
            case DENY_SCOPE_MISSING -> ErrorCode.PERMISSION_DENIED;
            case DENY_RATE_LIMITED -> ErrorCode.RATE_LIMITED;
            case DENY_QUOTA_EXCEEDED -> ErrorCode.QUOTA_EXCEEDED;
            case DENY, DENY_CONDITION_NOT_MET, ABSTAIN -> ErrorCode.FORBIDDEN;
            case ALLOW -> ErrorCode.FORBIDDEN;
        };
    }

    private Long resolveWorkspaceId(Map<String, Object> context) {
        if (context == null || !context.containsKey("workspaceId")) {
            return null;
        }
        Object value = context.get("workspaceId");
        if (value == null) {
            return null;
        }
        if (value instanceof Long workspaceId) {
            return workspaceId;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                log.debug("忽略非法 workspaceId 上下文值: {}", text);
            }
        }
        return null;
    }

    private String messageOrUnknown(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "unknown";
    }
}
