package com.axiqra.common.port;

import com.axiqra.common.domain.dto.PolicyEvaluationRequest;
import com.axiqra.common.domain.vo.PolicyEvaluationVO;

/**
 * ABAC 策略引擎 Port 接口
 *
 * <p>评估访问控制策略，返回决策结果。
 * 策略决策日志通过 AuditPort 异步写入审计库。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
public interface PolicyEnginePort {

    /**
     * 评估访问控制策略
     *
     * @param request 策略评估请求
     * @return 策略评估响应（包含决策、策略码、原因码）
     */
    PolicyEvaluationVO evaluate(PolicyEvaluationRequest request);

    /**
     * 快速检查用户是否具备某 Scope
     *
     * @param userId 用户 ID
     * @param scope  权限范围编码
     * @return true 表示具备该权限
     */
    boolean hasScope(Long userId, String scope);
}
