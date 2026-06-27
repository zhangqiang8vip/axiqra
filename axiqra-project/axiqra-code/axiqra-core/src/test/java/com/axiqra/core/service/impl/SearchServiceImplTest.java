package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.SearchRequest;
import com.axiqra.common.domain.entity.CandidateSeedEntity;
import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.entity.SolutionEntity;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.domain.enums.MemberStatus;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.SolutionStatus;
import com.axiqra.common.domain.enums.VerificationLevel;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.axiqra.common.domain.vo.SearchResponseVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.SolutionMapper;
import com.axiqra.core.service.CandidateSeedService;
import com.axiqra.core.service.RbacService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchServiceImpl 单元测试")
class SearchServiceImplTest {

    @Mock
    private SolutionMapper solutionMapper;

    @Mock
    private CandidateSeedService candidateSeedService;

    @Mock
    private RbacService rbacService;

    @Mock
    private com.axiqra.core.service.VectorSearchService vectorSearchService;

    @InjectMocks
    private SearchServiceImpl searchService;

    @Test
    @DisplayName("无 search:read scope 时应拒绝搜索")
    void shouldRejectWhenScopeMissing() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        when(rbacService.hasScope(1L, "search:read")).thenReturn(false);

        BizException ex = assertThrows(BizException.class,
                () -> searchService.searchBeforeAct(1L, request));

        assertEquals(ErrorCode.SEARCH_NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("有命中结果时应返回排序后的搜索结果")
    void shouldReturnSearchResults() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setWorkspaceId(100L);
        request.setLimit(5);
        request.setMinVerificationLevel(1);
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(solution(11L, "SOL-011", "Spring Search")));

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getItems().size());
        assertEquals("SOL-011", result.getItems().get(0).getSolutionCode());
        verify(candidateSeedService, never()).createOrReuseCandidateSeed(any(), any());
    }

    @Test
    @DisplayName("无结果且提供 workspaceId 时应创建 candidate seed")
    void shouldCreateCandidateSeedWhenSearchEmptyWithWorkspaceId() {
        SearchRequest request = SearchRequest.builder()
                .query("missing runbook")
                .workspaceId(100L)
                .includeCandidateSeed(true)
                .domain("backend")
                .techStack("spring")
                .minVerificationLevel(2)
                .build();
        CandidateSeedEntity createdSeed = new CandidateSeedEntity();
        createdSeed.setId(77L);
        createdSeed.setWorkspaceId(100L);
        createdSeed.setAuthorId(1L);
        createdSeed.setTaskGoal("missing runbook");
        createdSeed.setCoverageGap("search_empty;domain=backend;techStack=spring;minVerificationLevel=L2");
        createdSeed.setStatus("open");
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(null);
        when(candidateSeedService.createOrReuseCandidateSeed(1L, request))
                .thenReturn(new CandidateSeedService.CandidateSeedCreationResult(createdSeed, true));

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertTrue(result.isEmpty());
        assertTrue(result.isCandidateSeedCreated());
        assertNotNull(result.getCandidateSeed());
        assertTrue(result.getCandidateSeed().getCoverageGap().contains("domain=backend"));
        assertTrue(result.getCandidateSeed().getCoverageGap().contains("techStack=spring"));
        assertTrue(result.getCandidateSeed().getCoverageGap().contains("minVerificationLevel=L2"));
        verify(candidateSeedService).createOrReuseCandidateSeed(1L, request);
    }

    @Test
    @DisplayName("结果中包含不可见或低等级 solution 时应被过滤")
    void shouldFilterInvisibleOrLowVerificationSolutions() {
        SearchRequest request = SearchRequest.builder()
                .query("spring boot")
                .workspaceId(100L)
                .limit(5)
                .build();
        SolutionEntity visible = solution(11L, "SOL-011", "Visible Solution");
        SolutionEntity invisiblePrivate = solution(12L, "SOL-012", "Private Solution");
        invisiblePrivate.setVisibilityScope(VisibilityScope.PRIVATE);
        invisiblePrivate.setAuthorId(2L);
        SolutionEntity lowVerification = solution(13L, "SOL-013", "Low Verification");
        lowVerification.setVerificationLevel(VerificationLevel.L0);
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(visible, invisiblePrivate, lowVerification));

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getItems().size());
        assertEquals("SOL-011", result.getItems().get(0).getSolutionCode());
    }

    @Test
    @DisplayName("复用已有 candidate seed 时不应标记为 newly created")
    void shouldNotMarkCandidateSeedCreatedWhenExistingSeedReused() {
        SearchRequest request = SearchRequest.builder()
                .query("missing runbook")
                .workspaceId(100L)
                .includeCandidateSeed(true)
                .build();
        CandidateSeedEntity existingSeed = new CandidateSeedEntity();
        existingSeed.setId(99L);
        existingSeed.setWorkspaceId(100L);
        existingSeed.setAuthorId(1L);
        existingSeed.setQueryHash("existing-hash");
        existingSeed.setTaskGoal("missing runbook");
        existingSeed.setStatus("open");
        existingSeed.setDeleted(false);
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(candidateSeedService.createOrReuseCandidateSeed(1L, request))
                .thenReturn(new CandidateSeedService.CandidateSeedCreationResult(existingSeed, false));

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertTrue(result.isEmpty());
        assertFalse(result.isCandidateSeedCreated());
        assertNotNull(result.getCandidateSeed());
        assertEquals(99L, result.getCandidateSeed().getId());
        verify(candidateSeedService).createOrReuseCandidateSeed(1L, request);
    }

    @Test
    @DisplayName("无结果但未提供 workspaceId 时不应创建 candidate seed")
    void shouldNotCreateCandidateSeedWithoutWorkspaceId() {
        SearchRequest request = SearchRequest.builder()
                .query("missing runbook")
                .includeCandidateSeed(true)
                .build();
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertTrue(result.isEmpty());
        assertFalse(result.isCandidateSeedCreated());
        assertNull(result.getCandidateSeed());
        verify(candidateSeedService, never()).createOrReuseCandidateSeed(any(), any());
    }

    @Test
    @DisplayName("公开搜索不需要登录 scope 且只返回 public solution")
    void shouldSearchPublicSolutionsWithoutScope() {
        SearchRequest request = SearchRequest.builder()
                .query("spring boot")
                .workspaceId(100L)
                .includeCandidateSeed(true)
                .limit(5)
                .build();
        SolutionEntity publicSolution = solution(11L, "SOL-011", "Public Solution");
        publicSolution.setVisibilityScope(VisibilityScope.PUBLIC);
        SolutionEntity privateSolution = solution(12L, "SOL-012", "Private Solution");
        privateSolution.setVisibilityScope(VisibilityScope.PRIVATE);
        SolutionEntity r4Solution = solution(13L, "SOL-013", "R4 Solution");
        r4Solution.setVisibilityScope(VisibilityScope.PUBLIC);
        r4Solution.setRiskLevel(RiskLevel.R4.getLevel());
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(publicSolution, privateSolution, r4Solution));

        SearchResponseVO result = searchService.searchPublic(request);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getItems().size());
        assertEquals("SOL-011", result.getItems().get(0).getSolutionCode());
        assertFalse(result.isCandidateSeedCreated());
        verify(candidateSeedService, never()).createOrReuseCandidateSeed(any(), any());
    }

    @Test
    @DisplayName("公开搜索无结果时不创建 Candidate Seed")
    void shouldNotCreateCandidateSeedForPublicSearchEmptyResult() {
        SearchRequest request = SearchRequest.builder()
                .query("missing runbook")
                .workspaceId(100L)
                .includeCandidateSeed(true)
                .build();
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        SearchResponseVO result = searchService.searchPublic(request);

        assertTrue(result.isEmpty());
        assertFalse(result.isCandidateSeedCreated());
        assertNull(result.getCandidateSeed());
        verify(candidateSeedService, never()).createOrReuseCandidateSeed(any(), any());
    }

    // ========== 边界用例测试 ==========

    @Test
    @DisplayName("null userId 应抛 UNAUTHORIZED")
    void shouldRejectSearchWithNullUserId() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");

        BizException ex = assertThrows(BizException.class,
                () -> searchService.searchBeforeAct(null, request));

        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("空 query 应抛 PARAM_INVALID")
    void shouldRejectSearchWithEmptyQuery() {
        SearchRequest request = new SearchRequest();
        request.setQuery("");
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);

        BizException ex = assertThrows(BizException.class,
                () -> searchService.searchBeforeAct(1L, request));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("null query 应抛 PARAM_INVALID")
    void shouldRejectSearchWithNullQuery() {
        SearchRequest request = new SearchRequest();
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);

        BizException ex = assertThrows(BizException.class,
                () -> searchService.searchBeforeAct(1L, request));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("minVerificationLevel 超出范围应抛异常")
    void shouldRejectInvalidMinVerificationLevel() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setMinVerificationLevel(10);
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);

        BizException ex = assertThrows(BizException.class,
                () -> searchService.searchBeforeAct(1L, request));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("limit 超出范围应抛异常")
    void shouldRejectInvalidLimit() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setLimit(100);
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);

        BizException ex = assertThrows(BizException.class,
                () -> searchService.searchBeforeAct(1L, request));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("公开搜索空 query 应抛异常")
    void shouldRejectPublicSearchWithEmptyQuery() {
        SearchRequest request = new SearchRequest();
        request.setQuery("");

        BizException ex = assertThrows(BizException.class,
                () -> searchService.searchPublic(request));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("公开搜索 minVerificationLevel 超出范围应抛异常")
    void shouldRejectInvalidMinVerificationLevelForPublicSearch() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setMinVerificationLevel(10);

        BizException ex = assertThrows(BizException.class,
                () -> searchService.searchPublic(request));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("userId 为 null 且无 membership 时应返回空可见列表")
    void shouldHandleNullMembership() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setWorkspaceId(100L);
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(null);
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("非活跃成员的工作空间应被排除")
    void shouldExcludeInactiveMemberships() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setWorkspaceId(100L);
        MembershipEntity inactiveMembership = new MembershipEntity();
        inactiveMembership.setWorkspaceId(200L);
        inactiveMembership.setStatus("inactive");
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(inactiveMembership));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("solutionMapper 返回 null 应正常处理")
    void shouldHandleNullFromSolutionMapper() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setWorkspaceId(100L);
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(null);

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("搜索结果应包含正确的 matchSource")
    void shouldReturnCorrectMatchSource() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setWorkspaceId(100L);
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(solution(11L, "SOL-011", "Spring Search")));

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertFalse(result.isEmpty());
        assertEquals("keyword", result.getItems().get(0).getMatchSource());
    }

    @Test
    @DisplayName("无结果且 includeCandidateSeed 为 false 不应创建 seed")
    void shouldNotCreateSeedWhenFlagIsFalse() {
        SearchRequest request = SearchRequest.builder()
                .query("missing runbook")
                .workspaceId(100L)
                .includeCandidateSeed(false)
                .build();
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertTrue(result.isEmpty());
        assertFalse(result.isCandidateSeedCreated());
        verify(candidateSeedService, never()).createOrReuseCandidateSeed(any(), any());
    }

    @Test
    @DisplayName("搜索结果应包含 vectorSearchEnabled 标志")
    void shouldReturnVectorSearchEnabledFlag() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setWorkspaceId(100L);
        request.setEnableVectorSearch(true);
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("搜索结果应包含 keywordSearchHits 和 vectorSearchHits")
    void shouldReturnSearchHitCounts() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setWorkspaceId(100L);
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(solution(11L, "SOL-011", "Spring")));

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertEquals(1, result.getKeywordSearchHits());
        assertEquals(0, result.getVectorSearchHits());
    }

    @Test
    @DisplayName("搜索结果应包含 hybridSearchHits")
    void shouldReturnHybridSearchHits() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setWorkspaceId(100L);
        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(solution(11L, "SOL-011", "Spring")));

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertEquals(1, result.getHybridSearchHits());
    }

    // ========== 向量搜索测试 ==========

    @Test
    @DisplayName("启用向量搜索时应返回混合搜索结果")
    void shouldReturnHybridSearchResultsWhenVectorEnabled() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot configuration");
        request.setWorkspaceId(100L);
        request.setEnableVectorSearch(true);
        request.setVectorSearchWeight(0.6);
        SolutionEntity keywordSolution = solution(11L, "SOL-011", "Spring Config");
        SolutionEntity vectorSolution = solution(12L, "SOL-012", "Spring Boot");

        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(keywordSolution));
        when(vectorSearchService.searchByVector(anyString(), anyInt()))
                .thenReturn(List.of(
                        new com.axiqra.core.service.VectorSearchService.VectorSearchResult(12L, "SOL-012", 0.85, 0.9),
                        new com.axiqra.core.service.VectorSearchService.VectorSearchResult(11L, "SOL-011", 0.75, 0.8)));
        when(solutionMapper.selectBatchIds(anySet()))
                .thenReturn(List.of(vectorSolution, keywordSolution));

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertNotNull(result);
        assertEquals(true, result.getVectorSearchEnabled());
        assertTrue(result.getVectorSearchHits() > 0);
    }

    @Test
    @DisplayName("向量搜索失败时应回退到关键词搜索")
    void shouldFallbackToKeywordSearchWhenVectorFails() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setWorkspaceId(100L);
        request.setEnableVectorSearch(true);

        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(solution(11L, "SOL-011", "Spring")));
        when(vectorSearchService.searchByVector(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Vector service unavailable"));

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertNotNull(result);
        assertEquals(false, result.getVectorSearchEnabled());
    }

    @Test
    @DisplayName("禁用向量搜索时应只使用关键词搜索")
    void shouldUseKeywordOnlyWhenVectorDisabled() {
        SearchRequest request = new SearchRequest();
        request.setQuery("spring boot");
        request.setWorkspaceId(100L);
        request.setEnableVectorSearch(false);

        when(rbacService.hasScope(1L, "search:read")).thenReturn(true);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(activeMembership(100L)));
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(solution(11L, "SOL-011", "Spring")));

        SearchResponseVO result = searchService.searchBeforeAct(1L, request);

        assertNotNull(result);
        assertEquals(false, result.getVectorSearchEnabled());
        assertEquals(0, result.getVectorSearchHits());
    }

    private MembershipEntity activeMembership(Long workspaceId) {
        MembershipEntity membership = new MembershipEntity();
        membership.setWorkspaceId(workspaceId);
        membership.setRole(MemberRole.MEMBER.getCode());
        membership.setStatus(MemberStatus.ACTIVE.getCode());
        return membership;
    }

    private SolutionEntity solution(Long id, String code, String title) {
        SolutionEntity solution = new SolutionEntity();
        solution.setId(id);
        solution.setSolutionCode(code);
        solution.setTitle(title);
        solution.setWorkspaceId(100L);
        solution.setVerificationLevel(VerificationLevel.L3);
        solution.setRiskLevel(RiskLevel.R1.getLevel());
        solution.setStatus(SolutionStatus.VERIFIED);
        solution.setVisibilityScope(VisibilityScope.WORKSPACE);
        solution.setDomain("backend");
        solution.setTechStack("spring boot");
        return solution;
    }
}
