package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.SolutionCreateFromProjectCaseRequest;
import com.axiqra.common.domain.entity.ProjectCaseEntity;
import com.axiqra.common.domain.entity.SolutionEntity;
import com.axiqra.common.domain.entity.SolutionVersionEntity;
import com.axiqra.common.domain.enums.FeedbackType;
import com.axiqra.common.domain.enums.LicenseScope;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.ScopeEnum;
import com.axiqra.common.domain.enums.SolutionStatus;
import com.axiqra.common.domain.enums.VerificationLevel;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.axiqra.common.domain.vo.SearchResultItemVO;
import com.axiqra.common.domain.vo.SolutionDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.domain.vo.SolutionVersionVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.FeedbackMapper;
import com.axiqra.core.mapper.ProjectCaseMapper;
import com.axiqra.core.mapper.SolutionMapper;
import com.axiqra.core.mapper.SolutionVersionMapper;
import com.axiqra.core.service.RbacService;
import com.axiqra.core.service.SolutionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    private static final int DEFAULT_LIST_LIMIT = 10;
    private static final int MAX_LIST_LIMIT = 50;

    private final SolutionMapper solutionMapper;
    private final SolutionVersionMapper solutionVersionMapper;
    private final ProjectCaseMapper projectCaseMapper;
    private final FeedbackMapper feedbackMapper;
    private final RbacService rbacService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SolutionDetailVO createFromProjectCase(Long userId, SolutionCreateFromProjectCaseRequest request) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!rbacService.hasScope(userId, ScopeEnum.SOLUTION_WRITE.getCode())) {
            throw new BizException(ErrorCode.FORBIDDEN, "缺少 solution:write 权限");
        }
        validateCreateRequest(request);

        SolutionEntity existing = solutionMapper.selectBySourceCaseId(request.getProjectCaseId());
        if (existing != null && userId.equals(existing.getAuthorId())) {
            return buildDetail(existing);
        }

        ProjectCaseEntity projectCase = projectCaseMapper.selectActiveById(request.getProjectCaseId());
        if (projectCase == null) {
            throw new BizException(ErrorCode.PROJECT_CASE_NOT_FOUND);
        }
        if (!userId.equals(projectCase.getAuthorId()) && !rbacService.isMember(userId, projectCase.getWorkspaceId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权基于该 Project Case 生成 Solution");
        }

        VisibilityScope visibilityScope = normalizeVisibilityScope(request.getVisibilityScope());
        LicenseScope licenseScope = LicenseScope.of(projectCase.getLicenseScope());
        if (visibilityScope == VisibilityScope.PUBLIC && (licenseScope == null || !licenseScope.allowsPublicRelease())) {
            throw new BizException(ErrorCode.LICENSE_SCOPE_MISSING, "公开 Solution 需要 Project Case 使用允许公开的 licenseScope");
        }

        Instant now = Instant.now();
        SolutionEntity solution = new SolutionEntity()
                .setAuthorId(userId)
                .setWorkspaceId(projectCase.getWorkspaceId())
                .setProjectId(projectCase.getProjectId())
                .setSolutionCode("SOL-PC-" + projectCase.getId())
                .setTitle(request.getTitle().trim())
                .setDomain(trimToNull(request.getDomain()))
                .setTechStack(trimToNull(request.getTechStack()))
                .setVerificationLevel(VerificationLevel.L1)
                .setRiskLevel(RiskLevel.R1.getLevel())
                .setStatus(SolutionStatus.REVIEWED)
                .setVisibilityScope(visibilityScope)
                .setLicenseScope(licenseScope)
                .setSourceCaseId(projectCase.getId())
                .setDeleted(false);
        solution.setGmtCreate(now);
        solution.setGmtModified(now);
        solution.setVersion(0L);
        solutionMapper.insert(solution);

        SolutionVersionEntity version = new SolutionVersionEntity()
                .setSolutionId(solution.getId())
                .setVersionNumber(1)
                .setSteps(toJsonArray(request.getSteps()))
                .setApplicableContext(trimToNull(request.getApplicableContext()))
                .setNonApplicableContext(trimToNull(request.getNonApplicableContext()))
                .setEvidence(toJsonArray(request.getEvidenceRefs()))
                .setRisk(trimToNull(request.getRisk()))
                .setRollback(trimToNull(request.getRollback()))
                .setActive(true);
        version.setGmtCreate(now);
        version.setGmtModified(now);
        version.setVersion(0L);
        solutionVersionMapper.insert(version);

        log.info("从 Project Case 生成 Solution: solutionId={}, projectCaseId={}, userId={}, visibility={}",
                solution.getId(), projectCase.getId(), userId, visibilityScope.getCode());
        return buildDetail(solution);
    }

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
        RiskLevel riskLevel = riskLevelOf(solution);
        if (riskLevel != null && riskLevel.getLevel() >= RiskLevel.R4.getLevel() && !isAuthor) {
            throw new BizException(ErrorCode.SOLUTION_QUARANTINED, "高风险 Solution 已隔离，无法查看详情");
        }
        if ((solution.getVerificationLevel() == null || solution.getVerificationLevel().getLevel() < VerificationLevel.L1.getLevel()) && !isAuthor) {
            throw new BizException(ErrorCode.VERIFICATION_LEVEL_TOO_LOW);
        }

        SolutionDetailVO detail = buildDetail(solution);
        log.info("读取 Solution 详情: solutionId={}, userId={}, versionCount={}",
                solutionId, userId, detail.getVersions() != null ? detail.getVersions().size() : 0);
        return detail;
    }

    @Override
    public SolutionDetailVO getPublicDetail(Long solutionId) {
        if (solutionId == null || solutionId <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "solutionId 不能为空且必须大于 0");
        }

        SolutionEntity solution = solutionMapper.selectActiveById(solutionId);
        if (solution == null || !isPubliclyReadableSolution(solution)) {
            throw new BizException(ErrorCode.SOLUTION_NOT_FOUND);
        }

        List<SolutionVersionEntity> versions = defaultIfNull(solutionVersionMapper.selectBySolutionId(solutionId)).stream()
                .filter(Objects::nonNull)
                .toList();
        List<SolutionVersionVO> versionViews = versions.stream().map(this::toVersionVO).toList();
        SolutionVersionVO activeVersion = versionViews.stream()
                .filter(SolutionVersionVO::isActive)
                .findFirst()
                .orElse(versionViews.isEmpty() ? null : versionViews.get(0));

        return toDetailVO(solution, activeVersion, versionViews, buildFeedbackStats(solutionId));
    }

    @Override
    public List<SearchResultItemVO> listPublicSolutions(String query,
                                                        String domain,
                                                        String techStack,
                                                        Integer minVerificationLevel,
                                                        Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        int normalizedMinVerificationLevel = normalizeMinVerificationLevel(minVerificationLevel);
        String normalizedQuery = query == null ? "" : query.trim();

        return defaultIfNull(solutionMapper.searchVisibleSolutions(
                normalizedQuery,
                null,
                List.of(),
                domain,
                techStack,
                normalizedMinVerificationLevel,
                normalizedLimit
        )).stream()
                .filter(Objects::nonNull)
                .filter(this::isPubliclyReadableSolution)
                .map(solution -> toSearchResult(solution, null))
                .toList();
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

    private boolean isPubliclyReadableSolution(SolutionEntity solution) {
        if (solution == null || solution.isDeleted()) {
            return false;
        }
        if (solution.getVisibilityScope() != VisibilityScope.PUBLIC) {
            return false;
        }
        if (solution.getStatus() == null || !solution.getStatus().isReferencable()) {
            return false;
        }
        RiskLevel riskLevel = riskLevelOf(solution);
        if (riskLevel != null && riskLevel.getLevel() >= RiskLevel.R4.getLevel()) {
            return false;
        }
        return solution.getVerificationLevel() != null
                && solution.getVerificationLevel().getLevel() >= VerificationLevel.L1.getLevel();
    }

    private void validateCreateRequest(SolutionCreateFromProjectCaseRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "request 不能为空");
        }
        if (request.getProjectCaseId() == null || request.getProjectCaseId() <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "projectCaseId 不能为空且必须大于 0");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "title 不能为空");
        }
    }

    private VisibilityScope normalizeVisibilityScope(String value) {
        if (value == null || value.isBlank()) {
            return VisibilityScope.WORKSPACE;
        }
        VisibilityScope visibilityScope = VisibilityScope.of(value);
        if (visibilityScope == null || visibilityScope == VisibilityScope.ENTERPRISE) {
            throw new BizException(ErrorCode.PARAM_INVALID, "visibilityScope 仅支持 private/workspace/public");
        }
        return visibilityScope;
    }

    private SolutionDetailVO buildDetail(SolutionEntity solution) {
        List<SolutionVersionEntity> versions = defaultIfNull(solutionVersionMapper.selectBySolutionId(solution.getId())).stream()
                .filter(Objects::nonNull)
                .toList();
        List<SolutionVersionVO> versionViews = versions.stream().map(this::toVersionVO).toList();
        SolutionVersionVO activeVersion = versionViews.stream()
                .filter(SolutionVersionVO::isActive)
                .findFirst()
                .orElse(versionViews.isEmpty() ? null : versionViews.get(0));
        return toDetailVO(solution, activeVersion, versionViews, buildFeedbackStats(solution.getId()));
    }

    private String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(values.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList());
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "JSON 序列化失败");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private SolutionDetailVO toDetailVO(SolutionEntity solution,
                                        SolutionVersionVO activeVersion,
                                        List<SolutionVersionVO> versionViews,
                                        SolutionFeedbackStatsVO feedbackStats) {
        RiskLevel riskLevel = riskLevelOf(solution);
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
                .riskLevel(riskLevel != null ? riskLevel.getCode() : null)
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

    private SearchResultItemVO toSearchResult(SolutionEntity solution, String scoreReasonPrefix) {
        String reason = buildScoreReason(solution);
        if (scoreReasonPrefix != null && !scoreReasonPrefix.isBlank()) {
            reason = scoreReasonPrefix + "; " + reason;
        }
        RiskLevel riskLevel = riskLevelOf(solution);
        return SearchResultItemVO.builder()
                .solutionId(solution.getId())
                .solutionCode(solution.getSolutionCode())
                .title(solution.getTitle())
                .summary(buildSummary(solution))
                .domain(solution.getDomain())
                .techStack(solution.getTechStack())
                .verificationLevel(solution.getVerificationLevel() != null ? "L" + solution.getVerificationLevel().getLevel() : null)
                .riskLevel(riskLevel != null ? riskLevel.getCode() : null)
                .status(solution.getStatus() != null ? solution.getStatus().getCode() : null)
                .visibilityScope(solution.getVisibilityScope() != null ? solution.getVisibilityScope().getCode() : null)
                .workspaceId(solution.getWorkspaceId())
                .score(publicScore(solution))
                .scoreReason(reason)
                .build();
    }

    private String buildSummary(SolutionEntity solution) {
        String verification = solution.getVerificationLevel() != null
                ? "L" + solution.getVerificationLevel().getLevel() : "L?";
        RiskLevel riskLevel = riskLevelOf(solution);
        String risk = riskLevel != null ? riskLevel.getCode() : "R?";
        return verification + " / " + risk + " / " + (solution.getDomain() != null ? solution.getDomain() : "unknown-domain");
    }

    private String buildScoreReason(SolutionEntity solution) {
        return "status=" + (solution.getStatus() != null ? solution.getStatus().getCode() : "unknown")
                + ", verification=" + (solution.getVerificationLevel() != null ? solution.getVerificationLevel().getLevel() : "?")
                + ", risk=" + (riskLevelOf(solution) != null ? riskLevelOf(solution).getCode() : "?");
    }

    private double publicScore(SolutionEntity solution) {
        double score = 0.0D;
        if (solution.getStatus() != null) {
            score += switch (solution.getStatus()) {
                case CANONICAL -> 60D;
                case STABLE -> 50D;
                case VERIFIED -> 40D;
                case REVIEWED -> 30D;
                case NEEDS_REVIEW -> 25D;
                case CANDIDATE -> 20D;
                case REJECTED, QUARANTINED, ARCHIVED, DRAFT, DEPRECATED -> 0D;
            };
        }
        if (solution.getVerificationLevel() != null) {
            score += solution.getVerificationLevel().getLevel() * 5D;
        }
        RiskLevel riskLevel = riskLevelOf(solution);
        if (riskLevel != null) {
            score += Math.max(0D, 10D - riskLevel.getLevel() * 2D);
        }
        return score;
    }

    private RiskLevel riskLevelOf(SolutionEntity solution) {
        return solution.getRiskLevel() != null ? RiskLevel.of(solution.getRiskLevel()) : null;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIST_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIST_LIMIT) {
            throw new BizException(ErrorCode.PARAM_INVALID, "limit 必须在 1 到 50 之间");
        }
        return limit;
    }

    private int normalizeMinVerificationLevel(Integer minVerificationLevel) {
        if (minVerificationLevel == null) {
            return VerificationLevel.L1.getLevel();
        }
        if (minVerificationLevel < 0 || minVerificationLevel > VerificationLevel.L5.getLevel()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "minVerificationLevel 必须在 0 到 5 之间");
        }
        return Math.max(minVerificationLevel, VerificationLevel.L1.getLevel());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transitionToNeedsReview(Long userId, Long solutionId) {
        if (!rbacService.hasScope(userId, ScopeEnum.SOLUTION_WRITE.getCode())) {
            throw new BizException(ErrorCode.FORBIDDEN, "缺少 solution:write 权限");
        }

        SolutionEntity solution = solutionMapper.selectActiveById(solutionId);
        if (solution == null) {
            throw new BizException(ErrorCode.SOLUTION_NOT_FOUND);
        }

        if (solution.getStatus() != SolutionStatus.DRAFT && solution.getStatus() != SolutionStatus.CANDIDATE) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID,
                    "只有 DRAFT 或 CANDIDATE 状态可以提交审核，当前状态: " + solution.getStatus().getCode());
        }

        solution.setStatus(SolutionStatus.NEEDS_REVIEW);
        int rows = solutionMapper.update(solution);
        if (rows == 0) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "乐观锁冲突");
        }

        log.info("Solution 提交审核: solutionId={}, userId={}", solutionId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transitionAfterReviewApproved(Long solutionId, Long reviewerId) {
        SolutionEntity solution = solutionMapper.selectActiveById(solutionId);
        if (solution == null) {
            throw new BizException(ErrorCode.SOLUTION_NOT_FOUND);
        }

        if (solution.getStatus() != SolutionStatus.NEEDS_REVIEW) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID,
                    "只有 NEEDS_REVIEW 状态可以审核，当前状态: " + solution.getStatus().getCode());
        }

        solution.setStatus(SolutionStatus.REVIEWED);
        int rows = solutionMapper.update(solution);
        if (rows == 0) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "乐观锁冲突");
        }

        log.info("Solution 审核通过: solutionId={}, reviewerId={}", solutionId, reviewerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transitionAfterReviewRejected(Long solutionId, Long reviewerId, String reasonCode) {
        SolutionEntity solution = solutionMapper.selectActiveById(solutionId);
        if (solution == null) {
            throw new BizException(ErrorCode.SOLUTION_NOT_FOUND);
        }

        if (solution.getStatus() != SolutionStatus.NEEDS_REVIEW) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID,
                    "只有 NEEDS_REVIEW 状态可以审核，当前状态: " + solution.getStatus().getCode());
        }

        solution.setStatus(SolutionStatus.REJECTED);
        int rows = solutionMapper.update(solution);
        if (rows == 0) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "乐观锁冲突");
        }

        log.info("Solution 审核拒绝: solutionId={}, reviewerId={}, reasonCode={}", solutionId, reviewerId, reasonCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transitionToQuarantined(Long solutionId, Long reviewerId, String reasonCode) {
        SolutionEntity solution = solutionMapper.selectActiveById(solutionId);
        if (solution == null) {
            throw new BizException(ErrorCode.SOLUTION_NOT_FOUND);
        }

        if (solution.getStatus().isQuarantined() || solution.getStatus().isDeprecated()) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "已是隔离或废弃状态");
        }

        solution.setStatus(SolutionStatus.QUARANTINED);
        int rows = solutionMapper.update(solution);
        if (rows == 0) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "乐观锁冲突");
        }

        log.info("Solution 已隔离: solutionId={}, reviewerId={}, reasonCode={}", solutionId, reviewerId, reasonCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transitionToArchived(Long solutionId, Long userId) {
        if (!rbacService.hasScope(userId, ScopeEnum.SOLUTION_MAINTAIN.getCode())) {
            throw new BizException(ErrorCode.FORBIDDEN, "缺少 solution:maintain 权限");
        }

        SolutionEntity solution = solutionMapper.selectActiveById(solutionId);
        if (solution == null) {
            throw new BizException(ErrorCode.SOLUTION_NOT_FOUND);
        }

        if (!solution.getStatus().isDeprecated()) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID,
                    "只有 DEPRECATED 状态可以归档，当前状态: " + solution.getStatus().getCode());
        }

        solution.setStatus(SolutionStatus.ARCHIVED);
        int rows = solutionMapper.update(solution);
        if (rows == 0) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "乐观锁冲突");
        }

        log.info("Solution 已归档: solutionId={}, userId={}", solutionId, userId);
    }
}
