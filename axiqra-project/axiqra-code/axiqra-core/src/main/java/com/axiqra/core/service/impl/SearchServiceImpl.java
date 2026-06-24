package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.SearchRequest;
import com.axiqra.common.domain.entity.CandidateSeedEntity;
import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.entity.SolutionEntity;
import com.axiqra.common.domain.enums.MemberStatus;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.SolutionStatus;
import com.axiqra.common.domain.enums.VerificationLevel;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.axiqra.common.domain.vo.CandidateSeedVO;
import com.axiqra.common.domain.vo.SearchResponseVO;
import com.axiqra.common.domain.vo.SearchResultItemVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.SolutionMapper;
import com.axiqra.core.service.CandidateSeedService;
import com.axiqra.core.service.RbacService;
import com.axiqra.core.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Search 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SolutionMapper solutionMapper;
    private final CandidateSeedService candidateSeedService;
    private final RbacService rbacService;

    @Override
    @Transactional
    public SearchResponseVO searchBeforeAct(Long userId, SearchRequest request) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!rbacService.hasScope(userId, "search:read")) {
            throw new BizException(ErrorCode.SEARCH_NO_PERMISSION);
        }
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "query 不能为空");
        }

        int limit = normalizeLimit(request.getLimit());
        Integer minVerificationLevel = request.getMinVerificationLevel() != null
                ? request.getMinVerificationLevel() : 0;
        if (minVerificationLevel < 0 || minVerificationLevel > 5) {
            throw new BizException(ErrorCode.PARAM_INVALID, "minVerificationLevel 必须在 0 到 5 之间");
        }

        List<Long> visibleWorkspaceIds = resolveVisibleWorkspaceIds(userId);
        List<SolutionEntity> solutions = defaultIfNull(solutionMapper.searchVisibleSolutions(
                request.getQuery().trim(),
                request.getWorkspaceId(),
                visibleWorkspaceIds,
                request.getDomain(),
                request.getTechStack(),
                minVerificationLevel,
                limit
        ));

        List<SearchResultItemVO> items = solutions.stream()
                .filter(Objects::nonNull)
                .filter(this::isEligibleForSearch)
                .filter(solution -> canViewSolution(userId, solution, visibleWorkspaceIds))
                .map(solution -> toSearchResult(solution, request))
                .toList();

        if (!items.isEmpty()) {
            log.info("Search 命中结果: userId={}, query={}, hits={}", userId, request.getQuery(), items.size());
            return SearchResponseVO.builder()
                    .query(request.getQuery().trim())
                    .totalHits(items.size())
                    .returnedHits(items.size())
                    .empty(false)
                    .candidateSeedCreated(false)
                    .items(items)
                    .build();
        }

        CandidateSeedVO candidateSeed = null;
        boolean created = false;
        if (Boolean.TRUE.equals(request.getIncludeCandidateSeed()) && request.getWorkspaceId() != null) {
            CandidateSeedService.CandidateSeedCreationResult seedResult = candidateSeedService.createOrReuseCandidateSeed(userId, request);
            candidateSeed = toCandidateSeed(seedResult.seed());
            created = seedResult.created();
        }

        log.info("Search 无结果: userId={}, query={}, workspaceId={}, candidateSeedCreated={}",
                userId, request.getQuery(), request.getWorkspaceId(), created);
        return SearchResponseVO.builder()
                .query(request.getQuery().trim())
                .totalHits(0)
                .returnedHits(0)
                .empty(true)
                .candidateSeedCreated(created)
                .emptyReason("未找到可执行的 Solution，已返回空结果")
                .candidateSeed(candidateSeed)
                .items(List.of())
                .build();
    }

    @Override
    public SearchResponseVO searchPublic(SearchRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "query 不能为空");
        }

        int limit = normalizeLimit(request.getLimit());
        Integer minVerificationLevel = request.getMinVerificationLevel() != null
                ? request.getMinVerificationLevel() : VerificationLevel.L1.getLevel();
        if (minVerificationLevel < 0 || minVerificationLevel > VerificationLevel.L5.getLevel()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "minVerificationLevel 必须在 0 到 5 之间");
        }
        minVerificationLevel = Math.max(minVerificationLevel, VerificationLevel.L1.getLevel());

        List<SolutionEntity> solutions = defaultIfNull(solutionMapper.searchVisibleSolutions(
                request.getQuery().trim(),
                null,
                List.of(),
                request.getDomain(),
                request.getTechStack(),
                minVerificationLevel,
                limit
        ));

        List<SearchResultItemVO> items = solutions.stream()
                .filter(Objects::nonNull)
                .filter(this::isEligibleForSearch)
                .filter(this::isPublicWebVisible)
                .map(solution -> toSearchResult(solution, request))
                .toList();

        return SearchResponseVO.builder()
                .query(request.getQuery().trim())
                .totalHits(items.size())
                .returnedHits(items.size())
                .empty(items.isEmpty())
                .candidateSeedCreated(false)
                .emptyReason(items.isEmpty() ? "未找到公开可展示的 Solution" : null)
                .items(items)
                .build();
    }

    private SearchResultItemVO toSearchResult(SolutionEntity solution, SearchRequest request) {
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
                .score(calculateScore(solution, request))
                .scoreReason(buildScoreReason(solution))
                .build();
    }

    private CandidateSeedVO toCandidateSeed(CandidateSeedEntity entity) {
        if (entity == null) {
            return null;
        }
        return CandidateSeedVO.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspaceId())
                .authorId(entity.getAuthorId())
                .queryHash(entity.getQueryHash())
                .taskGoal(entity.getTaskGoal())
                .techStack(entity.getTechStack())
                .coverageGap(entity.getCoverageGap())
                .status(entity.getStatus())
                .assigneeId(entity.getAssigneeId())
                .solutionId(entity.getSolutionId())
                .build();
    }

    private List<Long> resolveVisibleWorkspaceIds(Long userId) {
        List<MembershipEntity> memberships = defaultIfNull(rbacService.getMemberships(userId));
        List<Long> workspaceIds = new ArrayList<>();
        for (MembershipEntity membership : memberships) {
            if (membership == null
                    || membership.getWorkspaceId() == null
                    || !MemberStatus.ACTIVE.getCode().equals(membership.getStatus())) {
                continue;
            }
            if (!workspaceIds.contains(membership.getWorkspaceId())) {
                workspaceIds.add(membership.getWorkspaceId());
            }
        }
        return workspaceIds;
    }

    private boolean isEligibleForSearch(SolutionEntity solution) {
        if (solution.getStatus() == null || solution.getVerificationLevel() == null) {
            return false;
        }
        if (solution.getStatus() == SolutionStatus.DRAFT || solution.getStatus() == SolutionStatus.DEPRECATED) {
            return false;
        }
        return solution.getVerificationLevel().getLevel() >= VerificationLevel.L1.getLevel();
    }

    private boolean canViewSolution(Long userId, SolutionEntity solution, List<Long> visibleWorkspaceIds) {
        VisibilityScope visibilityScope = solution.getVisibilityScope();
        if (visibilityScope == null) {
            return false;
        }
        return switch (visibilityScope) {
            case PUBLIC -> true;
            case PRIVATE -> userId.equals(solution.getAuthorId());
            case WORKSPACE, ENTERPRISE -> solution.getWorkspaceId() != null
                    && visibleWorkspaceIds.contains(solution.getWorkspaceId());
        };
    }

    private boolean isPublicWebVisible(SolutionEntity solution) {
        if (solution.getVisibilityScope() != VisibilityScope.PUBLIC) {
            return false;
        }
        RiskLevel riskLevel = riskLevelOf(solution);
        return riskLevel == null || riskLevel.getLevel() < RiskLevel.R4.getLevel();
    }

    private String buildSummary(SolutionEntity solution) {
        String verification = solution.getVerificationLevel() != null
                ? "L" + solution.getVerificationLevel().getLevel() : "L?";
        RiskLevel riskLevel = riskLevelOf(solution);
        String risk = riskLevel != null ? riskLevel.getCode() : "R?";
        return verification + " / " + risk + " / " + (solution.getDomain() != null ? solution.getDomain() : "unknown-domain");
    }

    private String buildScoreReason(SolutionEntity solution) {
        SolutionStatus status = solution.getStatus();
        VerificationLevel verificationLevel = solution.getVerificationLevel();
        RiskLevel riskLevel = riskLevelOf(solution);
        return "status=" + (status != null ? status.getCode() : "unknown")
                + ", verification=" + (verificationLevel != null ? verificationLevel.getLevel() : "?")
                + ", risk=" + (riskLevel != null ? riskLevel.getCode() : "?");
    }

    private double calculateScore(SolutionEntity solution, SearchRequest request) {
        double score = 0.0D;
        if (solution.getStatus() != null) {
            score += switch (solution.getStatus()) {
                case CANONICAL -> 60D;
                case STABLE -> 50D;
                case VERIFIED -> 40D;
                case REVIEWED -> 30D;
                case CANDIDATE -> 20D;
                case DRAFT, DEPRECATED -> 0D;
            };
        }
        if (solution.getVerificationLevel() != null) {
            score += solution.getVerificationLevel().getLevel() * 5D;
        }
        RiskLevel riskLevel = riskLevelOf(solution);
        if (riskLevel != null) {
            score += Math.max(0D, 10D - riskLevel.getLevel() * 2D);
        }
        if (request.getTechStack() != null && solution.getTechStack() != null
                && solution.getTechStack().toLowerCase().contains(request.getTechStack().trim().toLowerCase())) {
            score += 8D;
        }
        if (request.getDomain() != null && solution.getDomain() != null
                && solution.getDomain().equalsIgnoreCase(request.getDomain().trim())) {
            score += 5D;
        }
        return score;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 10;
        }
        if (limit < 1 || limit > 50) {
            throw new BizException(ErrorCode.PARAM_INVALID, "limit 必须在 1 到 50 之间");
        }
        return limit;
    }

    private RiskLevel riskLevelOf(SolutionEntity solution) {
        return solution.getRiskLevel() != null ? RiskLevel.of(solution.getRiskLevel()) : null;
    }

    private <T> List<T> defaultIfNull(List<T> items) {
        return items != null ? items : Collections.emptyList();
    }
}
