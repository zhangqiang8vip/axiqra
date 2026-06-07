package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.PolicyEvaluationRequest;
import com.axiqra.common.domain.enums.PolicyDecision;
import com.axiqra.common.domain.vo.PolicyEvaluationVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.port.PolicyEnginePort;
import com.axiqra.core.service.PolicyEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            throw new BizException(ErrorCode.FORBIDDEN,
                    "策略拒绝: " + result.getMessage());
        }
        log.debug("策略评估通过: userId={}, action={}, policyCode={}",
                request.getSubjectId(), request.getAction(), result.getPolicyCode());
    }
}
