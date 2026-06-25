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
import com.axiqra.core.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Search 服务实现 - 支持向量搜索的混合召回
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SolutionMapper solutionMapper;
    private final CandidateSeedService candidateSeedService;
    private final RbacService rbacService;
    private final VectorSearchService vectorSearchService;

    private static final double VECTOR_WEIGHT_DEFAULT = 0.6;
    private static final double KEYWORD_WEIGHT_DEFAULT = 0.4;

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
        List<SolutionEntity> keywordResults = defaultIfNull(solutionMapper.searchVisibleSolutions(
                request.getQuery().trim(),
                request.getWorkspaceId(),
                visibleWorkspaceIds,
                request.getDomain(),
                request.getTechStack(),
                minVerificationLevel,
                limit
        ));

        int keywordHits = keywordResults.size();
        List<VectorSearchService.VectorSearchResult> vectorResults = Collections.emptyList();
        int vectorHits = 0;
        int hybridHits = 0;

        // 向量搜索
        boolean enableVectorSearch = request.getEnableVectorSearch() != null 
                ? request.getEnableVectorSearch() : true;
        double vectorWeight = request.getVectorSearchWeight() != null 
                ? request.getVectorSearchWeight() : VECTOR_WEIGHT_DEFAULT;
        double keywordWeight = KEYWORD_WEIGHT_DEFAULT;

        if (enableVectorSearch) {
            try {
                vectorResults = vectorSearchService.searchByVector(request.getQuery().trim(), limit);
                vectorHits = vectorResults.size();
                log.debug("Vector search returned {} results for query: {}", vectorHits, request.getQuery());
            } catch (Exception e) {
                log.warn("Vector search failed, falling back to keyword search only: {}", e.getMessage());
                enableVectorSearch = false;
            }
        }

        // 合并结果
        List<SearchResultItemVO> items;
        if (enableVectorSearch && !vectorResults.isEmpty()) {
            // 混合搜索
            items = mergeAndRankResults(keywordResults, vectorResults, request, keywordWeight, vectorWeight, limit);
            hybridHits = items.size();
        } else {
            // 仅关键词搜索
            items = keywordResults.stream()
                    .filter(Objects::nonNull)
                    .filter(this::isEligibleForSearch)
                    .filter(solution -> canViewSolution(userId, solution, visibleWorkspaceIds))
                    .map(solution -> toSearchResult(solution, request, "keyword"))
                    .toList();
        }

        log.info("Search completed: userId={}, query={}, keywordHits={}, vectorHits={}, hybridHits={}",
                userId, request.getQuery(), keywordHits, vectorHits, hybridHits);

        if (!items.isEmpty()) {
            return SearchResponseVO.builder()
                    .query(request.getQuery().trim())
                    .totalHits(items.size())
                    .returnedHits(items.size())
                    .empty(false)
                    .candidateSeedCreated(false)
                    .vectorSearchEnabled(enableVectorSearch)
                    .keywordSearchHits(keywordHits)
                    .vectorSearchHits(vectorHits)
                    .hybridSearchHits(hybridHits)
                    .items(items)
                    .build();
        }

        // 无结果时创建 Candidate Seed
        CandidateSeedVO candidateSeed = null;
        boolean created = false;
        if (Boolean.TRUE.equals(request.getIncludeCandidateSeed()) && request.getWorkspaceId() != null) {
            CandidateSeedService.CandidateSeedCreationResult seedResult = candidateSeedService.createOrReuseCandidateSeed(userId, request);
            candidateSeed = toCandidateSeed(seedResult.seed());
            created = seedResult.created();
        }

        log.info("Search no results: userId={}, query={}, workspaceId={}, candidateSeedCreated={}",
                userId, request.getQuery(), request.getWorkspaceId(), created);
        return SearchResponseVO.builder()
                .query(request.getQuery().trim())
                .totalHits(0)
                .returnedHits(0)
                .empty(true)
                .candidateSeedCreated(created)
                .emptyReason("未找到可执行的 Solution，已返回空结果")
                .vectorSearchEnabled(enableVectorSearch)
                .keywordSearchHits(keywordHits)
                .vectorSearchHits(vectorHits)
                .candidateSeed(candidateSeed)
                .items(List.of())
                .build();
    }

    /**
     * 合并关键词和向量搜索结果，并进行混合排序
     */
    private List<SearchResultItemVO> mergeAndRankResults(
            List<SolutionEntity> keywordResults,
            List<VectorSearchService.VectorSearchResult> vectorResults,
            SearchRequest request,
            double keywordWeight,
            double vectorWeight,
            int limit) {

        // 构建向量结果的 map
        Map<Long, VectorSearchService.VectorSearchResult> vectorResultMap = vectorResults.stream()
                .collect(Collectors.toMap(VectorSearchService.VectorSearchResult::solutionId, Function.identity()));

        // 获取向量命中结果的完整实体
        Set<Long> vectorSolutionIds = vectorResultMap.keySet();
        List<SolutionEntity> vectorSolutions = solutionMapper.selectBatchIds(vectorSolutionIds)
                .stream()
                .filter(Objects::nonNull)
                .filter(this::isEligibleForSearch)
                .toList();

        // 合并结果
        Map<Long, SolutionEntity> allSolutions = new LinkedHashMap<>();
        
        // 先添加关键词结果
        for (SolutionEntity solution : keywordResults) {
            if (solution != null && isEligibleForSearch(solution)) {
                allSolutions.put(solution.getId(), solution);
            }
        }
        
        // 添加向量结果
        for (SolutionEntity solution : vectorSolutions) {
            allSolutions.putIfAbsent(solution.getId(), solution);
        }

        // 计算最终分数并排序
        return allSolutions.values().stream()
                .map(solution -> {
                    double vectorSimilarity = 0.0;
                    double keywordScore = 1.0;
                    String matchSource = "keyword";

                    // 检查是否有向量匹配
                    VectorSearchService.VectorSearchResult vectorResult = vectorResultMap.get(solution.getId());
                    if (vectorResult != null) {
                        vectorSimilarity = vectorResult.vectorSimilarity();
                        matchSource = "hybrid";
                    }

                    // 计算最终分数
                    double finalScore = keywordWeight * keywordScore + vectorWeight * vectorSimilarity;
                    
                    // 构建搜索结果
                    SearchResultItemVO item = toSearchResult(solution, request, matchSource);
                    item.setVectorSimilarity(vectorSimilarity);
                    item.setVectorMatched(vectorResult != null);
                    item.setScore(finalScore);
                    
                    return item;
                })
                .sorted((a, b) -> Double.compare(
                        b.getScore() != null ? b.getScore() : 0,
                        a.getScore() != null ? a.getScore() : 0))
                .limit(limit)
                .toList();
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
                .map(solution -> toSearchResult(solution, request, "keyword"))
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

    private SearchResultItemVO toSearchResult(SolutionEntity solution, SearchRequest request, String matchSource) {
        RiskLevel riskLevel = riskLevelOf(solution);
        String riskLevelCode = riskLevel != null ? riskLevel.getCode() : null;
        String verificationCode = solution.getVerificationLevel() != null ? "L" + solution.getVerificationLevel().getLevel() : null;
        double score = calculateScore(solution, request);
        
        return SearchResultItemVO.builder()
                .solutionId(solution.getId())
                .solutionCode(solution.getSolutionCode())
                .title(solution.getTitle())
                .summary(buildSummary(solution))
                .domain(solution.getDomain())
                .techStack(solution.getTechStack())
                .verificationLevel(verificationCode)
                .riskLevel(riskLevelCode)
                .status(solution.getStatus() != null ? solution.getStatus().getCode() : null)
                .visibilityScope(solution.getVisibilityScope() != null ? solution.getVisibilityScope().getCode() : null)
                .workspaceId(solution.getWorkspaceId())
                .score(score)
                .scoreReason(buildScoreReason(solution))
                .matchSource(matchSource)
                // D12 §7/§14 新增字段
                .resultType("Solution")
                .matchedTerms(buildMatchedTerms(solution, request))
                .fitReason(buildFitReason(solution, request))
                .cautionReason(buildCautionReason(solution, riskLevel))
                .evidenceSummary(buildEvidenceSummary(solution))
                .verificationExplanation(buildVerificationExplanation(solution))
                .riskExplanation(buildRiskExplanation(riskLevel))
                .spaceSource(determineSpaceSource(solution))
                .recommendedNextAction(determineRecommendedAction(riskLevel, solution))
                .failurePaths(solution.getFailurePaths())
                .build();
    }
    
    private String buildMatchedTerms(SolutionEntity solution, SearchRequest request) {
        StringBuilder matched = new StringBuilder();
        if (request.getQuery() != null) {
            matched.append("query:").append(request.getQuery());
        }
        if (request.getTechStack() != null && solution.getTechStack() != null 
                && solution.getTechStack().toLowerCase().contains(request.getTechStack().toLowerCase())) {
            matched.append(", techStack:").append(request.getTechStack());
        }
        if (request.getErrorSignature() != null) {
            matched.append(", error:").append(request.getErrorSignature());
        }
        return matched.length() > 0 ? matched.toString() : null;
    }
    
    private String buildFitReason(SolutionEntity solution, SearchRequest request) {
        StringBuilder reason = new StringBuilder();
        if (solution.getVerificationLevel() != null) {
            reason.append("验证等级 L").append(solution.getVerificationLevel().getLevel());
        }
        if (solution.getStatus() != null) {
            reason.append(", 状态 ").append(solution.getStatus().getCode());
        }
        if (request.getTechStack() != null && solution.getTechStack() != null 
                && solution.getTechStack().toLowerCase().contains(request.getTechStack().toLowerCase())) {
            reason.append(", 技术栈匹配");
        }
        return reason.toString();
    }
    
    private String buildCautionReason(SolutionEntity solution, RiskLevel riskLevel) {
        if (riskLevel == null) {
            return null;
        }
        StringBuilder caution = new StringBuilder();
        if (riskLevel.getLevel() >= RiskLevel.R3.getLevel()) {
            caution.append("高风险操作，需要人工确认");
        }
        if (solution.getVerificationLevel() != null && solution.getVerificationLevel().getLevel() < VerificationLevel.L3.getLevel()) {
            if (caution.length() > 0) caution.append("; ");
            caution.append("验证等级较低，需谨慎参考");
        }
        return caution.length() > 0 ? caution.toString() : null;
    }
    
    private String buildEvidenceSummary(SolutionEntity solution) {
        if (solution.getEvidenceCount() != null && solution.getEvidenceCount() > 0) {
            return "包含 " + solution.getEvidenceCount() + " 个证据";
        }
        return null;
    }
    
    private String buildVerificationExplanation(SolutionEntity solution) {
        if (solution.getVerificationLevel() == null) {
            return "未验证";
        }
        return switch (solution.getVerificationLevel()) {
            case L0 -> "未验证草稿";
            case L1 -> "用户确认有效";
            case L2 -> "有客观证据验证";
            case L3 -> "可复现验证 / 多次有效";
            case L4 -> "多上下文稳定验证";
            case L5 -> "跨上下文长期稳定验证";
        };
    }
    
    private String buildRiskExplanation(RiskLevel riskLevel) {
        if (riskLevel == null) {
            return "风险等级未知";
        }
        return switch (riskLevel) {
            case R0 -> "无风险，可放心使用";
            case R1 -> "低风险，局部配置或容易回滚的修改";
            case R2 -> "中风险，影响服务行为，需确认";
            case R3 -> "高风险，涉及生产/数据/安全，需人工确认";
            case R4 -> "极高风险，可能造成重大影响，禁止自动执行";
        };
    }
    
    private String determineSpaceSource(SolutionEntity solution) {
        if (solution.getWorkspaceId() == null) {
            return "public";
        }
        VisibilityScope visibilityScope = solution.getVisibilityScope();
        if (visibilityScope == null) {
            return "unknown";
        }
        return switch (visibilityScope) {
            case PRIVATE -> "project";
            case WORKSPACE -> "team";
            case ENTERPRISE -> "enterprise";
            case PUBLIC -> "public";
        };
    }
    
    private String determineRecommendedAction(RiskLevel riskLevel, SolutionEntity solution) {
        if (solution.getVerificationLevel() != null && solution.getVerificationLevel().getLevel() >= VerificationLevel.L3.getLevel()) {
            if (riskLevel == null || riskLevel.getLevel() <= RiskLevel.R1.getLevel()) {
                return "execute";
            }
            if (riskLevel.getLevel() <= RiskLevel.R2.getLevel()) {
                return "confirm";
            }
        }
        return "learn";
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
        
        // D12 §13 排序因素: status 权重
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
        
        // D12 §13: verification_level 权重
        if (solution.getVerificationLevel() != null) {
            score += solution.getVerificationLevel().getLevel() * 5D;
        }
        
        // D12 §13: risk_penalty
        RiskLevel riskLevel = riskLevelOf(solution);
        if (riskLevel != null) {
            score += Math.max(0D, 10D - riskLevel.getLevel() * 2D);
        }
        
        // D12 §13: tech_stack_match
        if (request.getTechStack() != null && solution.getTechStack() != null
                && solution.getTechStack().toLowerCase().contains(request.getTechStack().trim().toLowerCase())) {
            score += 8D;
        }
        
        // D12 §13: domain match
        if (request.getDomain() != null && solution.getDomain() != null
                && solution.getDomain().equalsIgnoreCase(request.getDomain().trim())) {
            score += 5D;
        }
        
        // D12 §13: error_signature 精确匹配加权
        if (request.getErrorSignature() != null && solution.getErrorSignature() != null
                && solution.getErrorSignature().toLowerCase().contains(request.getErrorSignature().toLowerCase())) {
            score += 15D; // 精确错误匹配大幅加权
        }
        
        // D12 §13: problem_type 匹配
        if (request.getProblemType() != null && solution.getProblemType() != null
                && solution.getProblemType().equalsIgnoreCase(request.getProblemType())) {
            score += 6D;
        }
        
        // D12 §13: environment 匹配
        if (request.getEnvironment() != null && solution.getEnvironment() != null
                && solution.getEnvironment().toLowerCase().contains(request.getEnvironment().toLowerCase())) {
            score += 4D;
        }
        
        // D12 §13: recency_score (最近更新时间越近越高)
        if (solution.getGmtModified() != null) {
            long daysSinceModified = java.time.Duration.between(solution.getGmtModified(), java.time.Instant.now()).toDays();
            if (daysSinceModified <= 30) {
                score += 3D; // 30天内更新
            } else if (daysSinceModified <= 90) {
                score += 1D; // 90天内更新
            }
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
