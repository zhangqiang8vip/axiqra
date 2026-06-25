package com.axiqra.core.service;

import com.axiqra.common.domain.dto.TracePathDTO;

/**
 * Trace 路径服务接口
 *
 * 用于记录和管理 Engineering Trace Package 的各种路径信息
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
public interface TracePathService {

    /**
     * 记录完整的 Trace 路径
     *
     * @param invocationId 调用记录 ID
     * @param tracePath 路径数据
     */
    void recordTracePath(Long invocationId, TracePathDTO tracePath);

    /**
     * 更新正向路径
     */
    void updateForwardPath(Long invocationId, TracePathDTO.ForwardPath forwardPath);

    /**
     * 更新决策路径
     */
    void updateDecisionPath(Long invocationId, TracePathDTO.DecisionStep decisionStep);

    /**
     * 更新回滚路径
     */
    void updateRollbackPath(Long invocationId, TracePathDTO.RollbackPath rollbackPath);

    /**
     * 更新演化提示
     */
    void updateEvolutionHint(Long invocationId, String evolutionHint);

    /**
     * 更新反向路径
     */
    void updateReversePath(Long invocationId, TracePathDTO.ReversePath reversePath);

    /**
     * 获取完整的路径数据
     */
    TracePathDTO getTracePath(Long invocationId);
}
