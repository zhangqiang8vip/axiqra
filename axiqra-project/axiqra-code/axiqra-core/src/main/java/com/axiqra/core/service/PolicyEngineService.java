package com.axiqra.core.service;

import com.axiqra.common.domain.dto.PolicyEvaluationRequest;
import com.axiqra.common.domain.vo.PolicyEvaluationVO;

/**
 * ABAC 策略引擎服务接口
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
public interface PolicyEngineService {

    /**
     * 评估访问控制策略
     */
    PolicyEvaluationVO evaluate(PolicyEvaluationRequest request);

    /**
     * 快速检查用户是否具备 scope
     */
    boolean hasScope(Long userId, String scope);

    /**
     * 强制策略评估（用于高风险操作前的最终校验，决策为 DENY 时抛出异常）
     */
    void enforce(PolicyEvaluationRequest request);
}
