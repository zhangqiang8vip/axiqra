package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.ContributionLedgerEntity;
import com.axiqra.common.domain.vo.ContributionRankingVO;
import com.axiqra.common.domain.vo.ContributionRecordVO;
import com.axiqra.common.domain.vo.UserContributionVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.UserMapper;
import com.axiqra.core.service.ContributionLedgerService;
import com.axiqra.core.service.ContributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Contribution 积分服务实现
 *
 * @author Axiqra Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContributionServiceImpl implements ContributionService {

    private final ContributionLedgerService contributionLedgerService;

    @Override
    public void recordContribution(Long userId, Long workspaceId, String contributionType, Long referenceId, int points) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (contributionType == null || contributionType.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "贡献类型不能为空");
        }

        ContributionLedgerService.ContributionRecordRequest request =
                new ContributionLedgerService.ContributionRecordRequest(
                        contributionType,
                        referenceId,
                        "contribution",
                        points > 0 ? points : 0,
                        null
                );
        contributionLedgerService.recordContribution(userId, request);
        log.info("记录贡献积分: userId={}, type={}, points={}", userId, contributionType, points);
    }

    @Override
    public UserContributionVO getUserContribution(Long userId, Long workspaceId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        var stats = contributionLedgerService.getUserStats(userId);
        return UserContributionVO.builder()
                .userId(userId)
                .totalPoints(stats.getTotalPoints())
                .monthlyPoints(stats.getMonthlyPoints())
                .weeklyPoints(stats.getWeeklyPoints())
                .rank(stats.getRank())
                .contributionCount(stats.getContributionCount())
                .contributionByType(stats.getContributionByType())
                .build();
    }

    @Override
    public List<ContributionRankingVO> getContributionRankings(Long workspaceId, int limit) {
        var rankingItems = contributionLedgerService.getLeaderboardWithUsers(limit > 0 ? limit : 20);
        int[] index = {0};
        return rankingItems.stream()
                .map(item -> ContributionRankingVO.builder()
                        .rank(++index[0])
                        .userId(item.userId())
                        .username("User-" + item.userId())
                        .totalPoints(item.totalPoints())
                        .contributionCount((long) item.contributionCount())
                        .build())
                .toList();
    }

    @Override
    public List<ContributionLedgerEntity> getRecentContributions(Long userId, int limit) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        var records = contributionLedgerService.getUserContributions(userId, limit > 0 ? limit : 10);
        return records.stream()
                .map(this::toEntity)
                .toList();
    }

    private ContributionLedgerEntity toEntity(ContributionRecordVO record) {
        ContributionLedgerEntity entity = new ContributionLedgerEntity();
        entity.setActorId(record.getUserId());
        entity.setEventType(record.getContributionType());
        entity.setPoints(record.getPoints());
        return entity;
    }
}
