package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.SolutionEntity;
import com.axiqra.common.domain.entity.SolutionVersionEntity;
import com.axiqra.common.domain.enums.FeedbackType;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.SolutionStatus;
import com.axiqra.common.domain.enums.VerificationLevel;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.axiqra.common.domain.vo.SolutionDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.domain.vo.SolutionVersionVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.FeedbackMapper;
import com.axiqra.core.mapper.SolutionMapper;
import com.axiqra.core.mapper.SolutionVersionMapper;
import com.axiqra.core.service.RbacService;
import com.axiqra.core.service.SolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Solution 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolutionServiceImpl implements SolutionService {

    private final SolutionMapper solutionMapper;
    private final SolutionVersionMapper solutionVersionMapper;
    private final FeedbackMapper feedbackMapper;
    private final RbacService rbacService;

    @Override
    public SolutionDetailVO getDetail(Long userId, Long solutionId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (solutionId == null || solutionId <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "solutionId 不能为空且必须大于 0");
        }

        SolutionEntity solution = solutionMapper.selectActiveById(solutionId);
        if (solution == null) {
            throw new BizException(ErrorCode.SOLUTION_NOT_FOUND);
        }
        if (!canView(userId, solution)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该 Solution");
        }
        boolean isAuthor = userId.equals(solution.getAuthorId());
        if (solution.getStatus() == SolutionStatus.DEPRECATED) {
            throw new BizException(ErrorCode.SOLUTION_DEPRECATED);
        }
        if (solution.getStatus() == SolutionStatus.DRAFT && !isAuthor) {
            throw new BizException(ErrorCode.SOLUTION_NOT_VERIFIED, "Solution 仍处于草稿态，无法查看详情");
        }
        if (solution.getRiskLevel() != null && solution.getRiskLevel().getLevel() >= RiskLevel.R4.getLevel() && !isAuthor) {
            throw new BizException(ErrorCode.SOLUTION_QUARANTINED, "高风险 Solution 已隔离，无法查看详情");
        }
        if ((solution.getVerificationLevel() == null || solution.getVerificationLevel().getLevel() < VerificationLevel.L1.getLevel()) && !isAuthor) {
            throw new BizException(ErrorCode.VERIFICATION_LEVEL_TOO_LOW);
        }

        List<SolutionVersionEntity> versions = defaultIfNull(solutionVersionMapper.selectBySolutionId(solutionId)).stream()
                .filter(Objects::nonNull)
                .toList();
        List<SolutionVersionVO> versionViews = versions.stream().map(this::toVersionVO).toList();
        SolutionVersionVO activeVersion = versionViews.stream()
                .filter(SolutionVersionVO::isActive)
                .findFirst()
                .orElse(versionViews.isEmpty() ? null : versionViews.get(0));
        SolutionFeedbackStatsVO feedbackStats = buildFeedbackStats(solutionId);

        log.info("读取 Solution 详情: solutionId={}, userId={}, versionCount={}", solutionId, userId, versionViews.size());
        return SolutionDetailVO.builder()
                .id(solution.getId())
                .solutionCode(solution.getSolutionCode())
                .title(solution.getTitle())
                .domain(solution.getDomain())
                .techStack(solution.getTechStack())
                .authorId(solution.getAuthorId())
                .workspaceId(solution.getWorkspaceId())
                .projectId(solution.getProjectId())
                .verificationLevel(solution.getVerificationLevel() != null ? "L" + solution.getVerificationLevel().getLevel() : null)
                .riskLevel(solution.getRiskLevel() != null ? solution.getRiskLevel().getCode() : null)
                .status(solution.getStatus() != null ? solution.getStatus().getCode() : null)
                .visibilityScope(solution.getVisibilityScope() != null ? solution.getVisibilityScope().getCode() : null)
                .licenseScope(solution.getLicenseScope() != null ? solution.getLicenseScope().getCode() : null)
                .sourceCaseId(solution.getSourceCaseId())
                .gmtCreate(solution.getGmtCreate())
                .gmtModified(solution.getGmtModified())
                .activeVersion(activeVersion)
                .versions(versionViews)
                .feedbackStats(feedbackStats)
                .build();
    }

    private boolean canView(Long userId, SolutionEntity solution) {
        if (solution.getVisibilityScope() == null) {
            return false;
        }
        return switch (solution.getVisibilityScope()) {
            case PUBLIC -> true;
            case WORKSPACE, ENTERPRISE -> solution.getWorkspaceId() != null && rbacService.isMember(userId, solution.getWorkspaceId());
            case PRIVATE -> userId.equals(solution.getAuthorId());
        };
    }

    private SolutionFeedbackStatsVO buildFeedbackStats(Long solutionId) {
        List<FeedbackMapper.FeedbackStatRow> stats = defaultIfNull(feedbackMapper.selectFeedbackStatsBySolutionId(solutionId));
        long workedCount = countByType(stats, FeedbackType.WORKED);
        long partialCount = countByType(stats, FeedbackType.PARTIAL);
        long failedCount = countByType(stats, FeedbackType.FAILED);
        long notApplicableCount = countByType(stats, FeedbackType.NOT_APPLICABLE);
        return SolutionFeedbackStatsVO.builder()
                .workedCount(workedCount)
                .partialCount(partialCount)
                .failedCount(failedCount)
                .notApplicableCount(notApplicableCount)
                .totalCount(workedCount + partialCount + failedCount + notApplicableCount)
                .build();
    }

    private long countByType(List<FeedbackMapper.FeedbackStatRow> stats, FeedbackType targetType) {
        return stats.stream()
                .filter(Objects::nonNull)
                .filter(stat -> targetType == FeedbackType.of(stat.getFeedbackType()))
                .map(FeedbackMapper.FeedbackStatRow::getCount)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0L);
    }

    private SolutionVersionVO toVersionVO(SolutionVersionEntity entity) {
        return SolutionVersionVO.builder()
                .id(entity.getId())
                .versionNumber(entity.getVersionNumber())
                .steps(entity.getSteps())
                .applicableContext(entity.getApplicableContext())
                .nonApplicableContext(entity.getNonApplicableContext())
                .evidence(entity.getEvidence())
                .risk(entity.getRisk())
                .rollback(entity.getRollback())
                .active(entity.isActive())
                .build();
    }

    private <T> List<T> defaultIfNull(List<T> items) {
        return items != null ? items : Collections.emptyList();
    }
}
