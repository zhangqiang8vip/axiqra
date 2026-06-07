package com.axiqra.common.domain.vo;

import com.axiqra.common.domain.enums.PolicyDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 策略评估响应 VO（ABAC 策略引擎出参）
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyEvaluationVO {

    private PolicyDecision decision;
    private String policyCode;
    private String reasonCode;
    private String message;

    public boolean isAllowed() {
        return decision != null && decision.isAllow();
    }
}
