package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.enums.ContributionType;
import com.axiqra.common.domain.vo.ContributionRecordVO;
import com.axiqra.common.domain.vo.ContributionStatsVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.ContributionLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 贡献账本服务实现
 *
 * 注意：当前实现使用内存存储，生产环境应使用数据库持久化
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContributionLedgerServiceImpl implements ContributionLedgerService {

    private final AuditPort auditPort;

    // 内存存储（生产环境应使用数据库）
    private final Map<String, ContributionRecordVO> contributions = new ConcurrentHashMap<>();
    private final Map<Long, List<String>> userContributions = new ConcurrentHashMap<>();
    private long idCounter = System.currentTimeMillis();

    @Override
    public ContributionRecordVO recordContribution(Long userId, ContributionRecordRequest request) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (request == null || request.contributionType() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "贡献类型不能为空");
        }

        ContributionType contributionType = ContributionType.of(request.contributionType());
        if (contributionType == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "无效的贡献类型: " + request.contributionType());
        }

        String contributionId = generateContributionId();
        Instant now = Instant.now();

        ContributionRecordVO record = ContributionRecordVO.builder()
                .contributionId(contributionId)
                .userId(userId)
                .contributionType(contributionType.getCode())
                .targetType(request.targetType())
                .targetId(request.targetId())
                .points(request.points() > 0 ? request.points() : contributionType.getBasePoints())
                .description(request.description() != null ? request.description() : contributionType.getDesc())
                .contributedAt(now)
                .build();

        contributions.put(contributionId, record);
        userContributions.computeIfAbsent(userId, k -> new ArrayList<>()).add(contributionId);

        auditPort.log(AuditPort.AuditEvent.builder()
                .requestId(contributionId)
                .actorId(userId)
                .actorType("user")
                .action("contribution.record")
                .objectType("contribution")
                .result("success")
                .payload(Map.of(
                        "contributionId", contributionId,
                        "contributionType", contributionType.getCode(),
                        "points", record.getPoints()
                ))
                .tenantId(null)
                .build());

        log.info("Contribution recorded: contributionId={}, userId={}, type={}, points={}",
                contributionId, userId, contributionType.getCode(), record.getPoints());

        return record;
    }

    @Override
    public List<ContributionRecordVO> getUserContributions(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<String> userContributionIds = userContributions.getOrDefault(userId, Collections.emptyList());
        return userContributionIds.stream()
                .map(contributions::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ContributionRecordVO::getContributedAt).reversed())
                .limit(limit > 0 ? limit : 50)
                .collect(Collectors.toList());
    }

    @Override
    public ContributionStatsVO getUserStats(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "userId 不能为空");
        }

        List<ContributionRecordVO> userRecords = getUserContributions(userId, Integer.MAX_VALUE);
        
        long totalPoints = userRecords.stream().mapToLong(ContributionRecordVO::getPoints).sum();
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant monthAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        long weeklyPoints = userRecords.stream()
                .filter(r -> r.getContributedAt().isAfter(weekAgo))
                .mapToLong(ContributionRecordVO::getPoints)
                .sum();

        long monthlyPoints = userRecords.stream()
                .filter(r -> r.getContributedAt().isAfter(monthAgo))
                .mapToLong(ContributionRecordVO::getPoints)
                .sum();

        Map<String, Long> contributionByType = userRecords.stream()
                .collect(Collectors.groupingBy(
                        ContributionRecordVO::getContributionType,
                        Collectors.summingLong(ContributionRecordVO::getPoints)
                ));

        // 计算排名
        Map<Long, Long> userPointsMap = new HashMap<>();
        for (Map.Entry<String, ContributionRecordVO> entry : contributions.entrySet()) {
            ContributionRecordVO record = entry.getValue();
            userPointsMap.merge(record.getUserId(), (long) record.getPoints(), Long::sum);
        }

        List<Long> sortedUserIds = userPointsMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        int rank = sortedUserIds.indexOf(userId) + 1;

        return ContributionStatsVO.builder()
                .userId(userId)
                .totalPoints(totalPoints)
                .monthlyPoints(monthlyPoints)
                .weeklyPoints(weeklyPoints)
                .rank(rank)
                .contributionByType(contributionByType)
                .contributionCount(userRecords.size())
                .generatedAt(System.currentTimeMillis())
                .build();
    }

    @Override
    public List<ContributionRecordVO> getLeaderboard(int limit) {
        Map<Long, Long> userPointsMap = new HashMap<>();
        for (ContributionRecordVO record : contributions.values()) {
            userPointsMap.merge(record.getUserId(), (long) record.getPoints(), Long::sum);
        }

        return userPointsMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit > 0 ? limit : 100)
                .map(entry -> ContributionRecordVO.builder()
                        .userId(entry.getKey())
                        .points((int) entry.getValue().longValue())
                        .contributionCount(getUserContributions(entry.getKey(), Integer.MAX_VALUE).size())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ContributionRankingItem> getLeaderboardWithUsers(int limit) {
        Map<Long, Long> userPointsMap = new HashMap<>();
        for (ContributionRecordVO record : contributions.values()) {
            userPointsMap.merge(record.getUserId(), (long) record.getPoints(), Long::sum);
        }

        return userPointsMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit > 0 ? limit : 100)
                .map(entry -> new ContributionRankingItem(
                        entry.getKey(),
                        entry.getValue(),
                        getUserContributions(entry.getKey(), Integer.MAX_VALUE).size()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public ContributionRecordVO getContribution(String contributionId) {
        if (contributionId == null || contributionId.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "contributionId 不能为空");
        }
        return contributions.get(contributionId);
    }

    private String generateContributionId() {
        return "C-" + (idCounter++);
    }
}
