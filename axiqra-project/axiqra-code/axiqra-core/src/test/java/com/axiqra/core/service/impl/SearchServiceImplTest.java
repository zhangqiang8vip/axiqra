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
