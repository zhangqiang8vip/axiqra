package com.axiqra.core.service;

import com.axiqra.common.domain.vo.ToolModelLeaderboardVO;

import java.util.List;

/**
 * ToolModel Service 接口
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
public interface ToolModelService {

    /**
     * 获取工具模型排行榜
     *
     * @param scopeType  范围类型: global/workspace/enterprise
     * @param scopeId    范围 ID
     * @param toolName   工具名称（可选）
     * @param limit      数量限制
     * @return 排行榜
     */
    List<ToolModelLeaderboardVO> getLeaderboard(String scopeType, Long scopeId, String toolName, int limit);

    /**
     * 回填工具模型归因记录
     *
     * @param solutionId    Solution ID
     * @param requestId     请求 ID
     * @param toolName      工具名称
     * @param modelProvider 模型提供商
     * @param modelName     模型名称
     * @param modelVersion  模型版本
     * @param confidence    置信度
     */
    void backfillAttribution(Long solutionId, String requestId, String toolName, String modelProvider,
                             String modelName, String modelVersion, String confidence);
}
