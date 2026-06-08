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

            Severity severity = PolicyDecision.DENY_RISK_LEVEL_TOO_HIGH.getCode().equals(result.getReasonCode())
                    ? Severity.CRITICAL : Severity.WARNING;

            alertPort.sendAlert(AlertEvent.builder()
                    .type(AlertType.POLICY_DENIED)
                    .severity(severity)
                    .actorId(request.getSubjectId())
                    .actorType("user")
                    .objectType(request.getObjectType())
                    .objectId(request.getObjectId())
                    .workspaceId(request.getContext() != null ? (Long) request.getContext().get("workspaceId") : null)
                    .message("策略拒绝: " + result.getMessage())
                    .reasonCode(result.getReasonCode())
                    .metadata(Map.of(
                            "action", request.getAction() != null ? request.getAction() : "",
                            "policyCode", result.getPolicyCode() != null ? result.getPolicyCode() : ""
                    ))
                    .build());

            throw new BizException(ErrorCode.FORBIDDEN,
                    "策略拒绝: " + result.getMessage());
        }
        log.debug("策略评估通过: userId={}, action={}, policyCode={}",
                request.getSubjectId(), request.getAction(), result.getPolicyCode());
    }
}
