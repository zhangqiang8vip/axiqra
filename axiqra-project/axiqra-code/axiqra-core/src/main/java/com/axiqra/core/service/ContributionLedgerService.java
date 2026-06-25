package com.axiqra.core.service;

import com.axiqra.common.domain.vo.ContributionRecordVO;
import com.axiqra.common.domain.vo.ContributionStatsVO;

import java.util.List;

/**
 * 贡献账本服务接口
 *
 * 记录和管理用户的贡献记录
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
public interface ContributionLedgerService {

    /**
     * 记录贡献
     */
    ContributionRecordVO recordContribution(Long userId, ContributionRecordRequest request);

    /**
     * 获取用户贡献记录
     */
    List<ContributionRecordVO> getUserContributions(Long userId, int limit);

    /**
     * 获取用户贡献统计
     */
    ContributionStatsVO getUserStats(Long userId);

    /**
     * 获取贡献排行榜
     */
    List<ContributionRecordVO> getLeaderboard(int limit);

    /**
     * 获取贡献排行榜（带用户信息）
     */
    List<ContributionRankingItem> getLeaderboardWithUsers(int limit);

    record ContributionRankingItem(Long userId, long totalPoints, int contributionCount) {}

    /**
     * 获取贡献记录详情
     */
    ContributionRecordVO getContribution(String contributionId);

    /**
     * 贡献记录请求
     */
    record ContributionRecordRequest(
            String contributionType,
            Long targetId,
            String targetType,
            int points,
            String description
    ) {}
}
