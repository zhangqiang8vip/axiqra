package com.axiqra.core.service;

import com.axiqra.common.domain.entity.ContributionLedgerEntity;
import com.axiqra.common.domain.vo.ContributionRankingVO;
import com.axiqra.common.domain.vo.UserContributionVO;

import java.util.List;

/**
 * Contribution 积分服务接口
 */
public interface ContributionService {

    /**
     * 计算并记录贡献积分
     *
     * @param userId 用户 ID
     * @param workspaceId 工作空间 ID
     * @param contributionType 贡献类型
     * @param referenceId 关联 ID（如 solutionId、reviewId 等）
     * @param points 积分
     */
    void recordContribution(Long userId, Long workspaceId, String contributionType, Long referenceId, int points);

    /**
     * 获取用户贡献统计
     *
     * @param userId 用户 ID
     * @param workspaceId 工作空间 ID（可选，null 表示全局）
     * @return 用户贡献统计
     */
    UserContributionVO getUserContribution(Long userId, Long workspaceId);

    /**
     * 获取贡献排行榜
     *
     * @param workspaceId 工作空间 ID（可选，null 表示全局）
     * @param limit 返回数量
     * @return 排行榜
     */
    List<ContributionRankingVO> getContributionRankings(Long workspaceId, int limit);

    /**
     * 获取用户最近贡献记录
     *
     * @param userId 用户 ID
     * @param limit 返回数量
     * @return 贡献记录列表
     */
    List<ContributionLedgerEntity> getRecentContributions(Long userId, int limit);
}
