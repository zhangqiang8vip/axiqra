package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.SolutionCreateFromProjectCaseRequest;
import com.axiqra.common.domain.entity.ProjectCaseEntity;
import com.axiqra.common.domain.entity.SolutionEntity;
import com.axiqra.common.domain.entity.SolutionVersionEntity;
import com.axiqra.common.domain.enums.FeedbackType;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.SolutionStatus;
import com.axiqra.common.domain.enums.VerificationLevel;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.FeedbackMapper;
import com.axiqra.core.mapper.ProjectCaseMapper;
import com.axiqra.core.mapper.SolutionMapper;
import com.axiqra.core.mapper.SolutionVersionMapper;
import com.axiqra.core.service.RbacService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolutionServiceImpl 单元测试")
class SolutionServiceImplTest {

    @Mock
    private SolutionMapper solutionMapper;

    @Mock
    private SolutionVersionMapper solutionVersionMapper;

    @Mock
    private ProjectCaseMapper projectCaseMapper;

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private RbacService rbacService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SolutionServiceImpl solutionService;

    @Test
    @DisplayName("不存在的 solution 应抛 SOLUTION_NOT_FOUND")
    void shouldThrowWhenSolutionMissing() {
        when(solutionMapper.selectActiveById(10L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getDetail(1L, 10L));

        assertEquals(ErrorCode.SOLUTION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("私有 solution 非作者访问应被拒绝")
    void shouldRejectPrivateSolutionForNonAuthor() {
        SolutionEntity solution = baseSolution();
        solution.setVisibilityScope(VisibilityScope.PRIVATE);
        solution.setAuthorId(2L);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getDetail(1L, 10L));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("草稿态 solution 非作者访问应抛 SOLUTION_NOT_VERIFIED")
    void shouldRejectDraftSolutionForNonAuthor() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.DRAFT);
        solution.setAuthorId(2L);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getDetail(1L, 10L));

        assertEquals(ErrorCode.SOLUTION_NOT_VERIFIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("草稿态 solution 作者本人可查看")
    void shouldAllowAuthorToViewDraftSolution() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.DRAFT);
        solution.setAuthorId(1L);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(solutionVersionMapper.selectBySolutionId(10L)).thenReturn(List.of());
        when(feedbackMapper.selectFeedbackStatsBySolutionId(10L)).thenReturn(List.of());

        var result = solutionService.getDetail(1L, 10L);

        assertNotNull(result);
        assertEquals("draft", result.getStatus());
    }

    @Test
    @DisplayName("高风险 solution 非作者访问应抛 SOLUTION_QUARANTINED")
    void shouldRejectHighRiskSolutionForNonAuthor() {
        SolutionEntity solution = baseSolution();
        solution.setRiskLevel(RiskLevel.R4.getLevel());
        solution.setAuthorId(2L);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getDetail(1L, 10L));

        assertEquals(ErrorCode.SOLUTION_QUARANTINED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("高风险 solution 作者本人可查看")
    void shouldAllowAuthorToViewHighRiskSolution() {
        SolutionEntity solution = baseSolution();
        solution.setRiskLevel(RiskLevel.R4.getLevel());
        solution.setAuthorId(1L);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(solutionVersionMapper.selectBySolutionId(10L)).thenReturn(List.of());
        when(feedbackMapper.selectFeedbackStatsBySolutionId(10L)).thenReturn(List.of());

        var result = solutionService.getDetail(1L, 10L);

        assertNotNull(result);
        assertEquals(RiskLevel.R4.getCode(), result.getRiskLevel());
    }

    @Test
    @DisplayName("可见 solution 应返回详情、版本与反馈统计")
    void shouldReturnDetailWhenVisible() {
        SolutionEntity solution = baseSolution();
        solution.setVisibilityScope(VisibilityScope.WORKSPACE);
        solution.setAuthorId(1L);

        SolutionVersionEntity version = new SolutionVersionEntity();
        version.setId(101L);
        version.setSolutionId(10L);
        version.setVersionNumber(3);
        version.setSteps("step-1");
        version.setActive(true);

        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(rbacService.isMember(1L, 100L)).thenReturn(true);
        when(solutionVersionMapper.selectBySolutionId(10L)).thenReturn(List.of(version));
        FeedbackMapper.FeedbackStatRow workedRow = mock(FeedbackMapper.FeedbackStatRow.class);
        when(workedRow.getFeedbackType()).thenReturn(FeedbackType.WORKED.getCode());
        when(workedRow.getCount()).thenReturn(2L);

        FeedbackMapper.FeedbackStatRow failedRow = mock(FeedbackMapper.FeedbackStatRow.class);
        when(failedRow.getFeedbackType()).thenReturn(FeedbackType.FAILED.getCode());
        when(failedRow.getCount()).thenReturn(1L);

        FeedbackMapper.FeedbackStatRow unknownRow = mock(FeedbackMapper.FeedbackStatRow.class);
        lenient().when(unknownRow.getFeedbackType()).thenReturn("unknown");
        lenient().when(unknownRow.getCount()).thenReturn(7L);

        FeedbackMapper.FeedbackStatRow notApplicableRow = mock(FeedbackMapper.FeedbackStatRow.class);
        when(notApplicableRow.getFeedbackType()).thenReturn(FeedbackType.NOT_APPLICABLE.getCode());
        when(notApplicableRow.getCount()).thenReturn(null);

        when(feedbackMapper.selectFeedbackStatsBySolutionId(10L)).thenReturn(List.of(
                workedRow, failedRow, unknownRow, notApplicableRow));

        var result = solutionService.getDetail(1L, 10L);

        assertNotNull(result);
        assertEquals("SOL-010", result.getSolutionCode());
        assertEquals(2, result.getFeedbackStats().getWorkedCount());
        assertEquals(1, result.getFeedbackStats().getFailedCount());
        assertEquals(0, result.getFeedbackStats().getNotApplicableCount());
        assertEquals(3, result.getFeedbackStats().getTotalCount());
        assertEquals(101L, result.getActiveVersion().getId());
    }

    @Test
    @DisplayName("废弃态 solution 应抛 SOLUTION_DEPRECATED")
    void shouldThrowWhenSolutionDeprecated() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.DEPRECATED);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getDetail(1L, 10L));

        assertEquals(ErrorCode.SOLUTION_DEPRECATED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("低验证等级 solution 非作者访问应抛 VERIFICATION_LEVEL_TOO_LOW")
    void shouldRejectLowVerificationLevelForNonAuthor() {
        SolutionEntity solution = baseSolution();
        solution.setVerificationLevel(VerificationLevel.L0);
        solution.setAuthorId(2L);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getDetail(1L, 10L));

        assertEquals(ErrorCode.VERIFICATION_LEVEL_TOO_LOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("null userId 应抛 UNAUTHORIZED")
    void shouldThrowWhenUserIdNull() {
        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getDetail(null, 10L));

        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("无效 solutionId 应抛 PARAM_INVALID")
    void shouldThrowWhenSolutionIdInvalid() {
        BizException exNull = assertThrows(BizException.class,
                () -> solutionService.getDetail(1L, null));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), exNull.getCode());

        BizException exZero = assertThrows(BizException.class,
                () -> solutionService.getDetail(1L, 0L));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), exZero.getCode());
    }

    @Test
    @DisplayName("公开详情应允许匿名读取 public 且 L1+ 的 solution")
    void shouldReturnPublicDetail() {
        SolutionEntity solution = baseSolution();
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(solutionVersionMapper.selectBySolutionId(10L)).thenReturn(List.of());
        when(feedbackMapper.selectFeedbackStatsBySolutionId(10L)).thenReturn(List.of());

        var result = solutionService.getPublicDetail(10L);

        assertNotNull(result);
        assertEquals("public", result.getVisibilityScope());
        assertEquals("SOL-010", result.getSolutionCode());
    }

    @Test
    @DisplayName("公开详情应隐藏 private solution")
    void shouldHidePrivateSolutionFromPublicDetail() {
        SolutionEntity solution = baseSolution();
        solution.setVisibilityScope(VisibilityScope.PRIVATE);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getPublicDetail(10L));

        assertEquals(ErrorCode.SOLUTION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("公开详情应隐藏 R4 solution")
    void shouldHideR4SolutionFromPublicDetail() {
        SolutionEntity solution = baseSolution();
        solution.setRiskLevel(RiskLevel.R4.getLevel());
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getPublicDetail(10L));

        assertEquals(ErrorCode.SOLUTION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("公开列表应只返回 public 且 L1+ 的 solution")
    void shouldListPublicSolutions() {
        SolutionEntity publicSolution = baseSolution();
        SolutionEntity privateSolution = baseSolution();
        privateSolution.setId(11L);
        privateSolution.setSolutionCode("SOL-011");
        privateSolution.setVisibilityScope(VisibilityScope.PRIVATE);
        SolutionEntity lowVerification = baseSolution();
        lowVerification.setId(12L);
        lowVerification.setSolutionCode("SOL-012");
        lowVerification.setVerificationLevel(VerificationLevel.L0);

        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(publicSolution, privateSolution, lowVerification));

        var result = solutionService.listPublicSolutions("spring", null, null, 0, 10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("SOL-010", result.get(0).getSolutionCode());
        assertEquals("public", result.get(0).getVisibilityScope());
    }

    @Test
    @DisplayName("个人 Project Case 可生成 workspace Solution")
    void shouldCreateWorkspaceSolutionFromPersonalProjectCase() throws Exception {
        ProjectCaseEntity projectCase = baseProjectCase();
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);
        when(solutionMapper.selectBySourceCaseId(77L)).thenReturn(null);
        when(projectCaseMapper.selectActiveById(77L)).thenReturn(projectCase);
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"step\"]");
        doAnswer(invocation -> {
            SolutionEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return 1;
        }).when(solutionMapper).insert(any(SolutionEntity.class));
        doAnswer(invocation -> {
            SolutionVersionEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        }).when(solutionVersionMapper).insert(any(SolutionVersionEntity.class));
        when(solutionVersionMapper.selectBySolutionId(10L)).thenReturn(List.of());
        when(feedbackMapper.selectFeedbackStatsBySolutionId(10L)).thenReturn(List.of());

        var result = solutionService.createFromProjectCase(1L, SolutionCreateFromProjectCaseRequest.builder()
                .projectCaseId(77L)
                .title("AI Agent 接入失败排查方案")
                .domain("backend")
                .techStack("spring boot")
                .steps(List.of("检查 token", "运行 doctor"))
                .visibilityScope("workspace")
                .build());

        assertNotNull(result);
        assertEquals("SOL-PC-77", result.getSolutionCode());
        assertEquals("workspace", result.getVisibilityScope());
        assertEquals("L1", result.getVerificationLevel());
        verify(solutionMapper).insert(any(SolutionEntity.class));
        verify(solutionVersionMapper).insert(any(SolutionVersionEntity.class));
    }

    @Test
    @DisplayName("公开 Solution 需要 Project Case 具备公开 license")
    void shouldRejectPublicSolutionWithoutPublicLicense() {
        ProjectCaseEntity projectCase = baseProjectCase();
        projectCase.setLicenseScope("proprietary");
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);
        when(solutionMapper.selectBySourceCaseId(77L)).thenReturn(null);
        when(projectCaseMapper.selectActiveById(77L)).thenReturn(projectCase);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.createFromProjectCase(1L, SolutionCreateFromProjectCaseRequest.builder()
                        .projectCaseId(77L)
                        .title("AI Agent 接入失败排查方案")
                        .visibilityScope("public")
                        .build()));

        assertEquals(ErrorCode.LICENSE_SCOPE_MISSING.getCode(), ex.getCode());
    }

    // ========== 状态转换测试 ==========

    @Test
    @DisplayName("transitionToNeedsReview 应将 DRAFT 转为 NEEDS_REVIEW")
    void shouldTransitionToNeedsReviewFromDraft() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.DRAFT);
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(solutionMapper.update(solution)).thenReturn(1);

        solutionService.transitionToNeedsReview(1L, 10L);

        assertEquals(SolutionStatus.NEEDS_REVIEW, solution.getStatus());
        verify(solutionMapper).update(solution);
    }

    @Test
    @DisplayName("transitionToNeedsReview 应将 CANDIDATE 转为 NEEDS_REVIEW")
    void shouldTransitionToNeedsReviewFromCandidate() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.CANDIDATE);
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(solutionMapper.update(solution)).thenReturn(1);

        solutionService.transitionToNeedsReview(1L, 10L);

        assertEquals(SolutionStatus.NEEDS_REVIEW, solution.getStatus());
    }

    @Test
    @DisplayName("transitionToNeedsReview 无 solution:write 权限应抛异常")
    void shouldRejectTransitionToNeedsReviewWithoutPermission() {
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(false);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.transitionToNeedsReview(1L, 10L));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("transitionToNeedsReview 非 DRAFT/CANDIDATE 状态应抛异常")
    void shouldRejectTransitionToNeedsReviewFromInvalidStatus() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.VERIFIED);
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.transitionToNeedsReview(1L, 10L));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("transitionAfterReviewApproved 应将 NEEDS_REVIEW 转为 REVIEWED")
    void shouldApproveReview() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.NEEDS_REVIEW);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(solutionMapper.update(solution)).thenReturn(1);

        solutionService.transitionAfterReviewApproved(10L, 2L);

        assertEquals(SolutionStatus.REVIEWED, solution.getStatus());
    }

    @Test
    @DisplayName("transitionAfterReviewApproved 非 NEEDS_REVIEW 状态应抛异常")
    void shouldRejectApproveReviewFromInvalidStatus() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.DRAFT);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.transitionAfterReviewApproved(10L, 2L));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("transitionAfterReviewRejected 应将 NEEDS_REVIEW 转为 REJECTED")
    void shouldRejectReview() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.NEEDS_REVIEW);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(solutionMapper.update(solution)).thenReturn(1);

        solutionService.transitionAfterReviewRejected(10L, 2L, "INVALID_FORMAT");

        assertEquals(SolutionStatus.REJECTED, solution.getStatus());
    }

    @Test
    @DisplayName("transitionToQuarantined 应将任意状态转为 QUARANTINED")
    void shouldQuarantineSolution() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.VERIFIED);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(solutionMapper.update(solution)).thenReturn(1);

        solutionService.transitionToQuarantined(10L, 2L, "MALICIOUS");

        assertEquals(SolutionStatus.QUARANTINED, solution.getStatus());
    }

    @Test
    @DisplayName("transitionToQuarantined 已隔离或废弃应抛异常")
    void shouldRejectQuarantineAlreadyQuarantined() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.QUARANTINED);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.transitionToQuarantined(10L, 2L, "ALREADY_QUARANTINED"));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("transitionToArchived 应将 DEPRECATED 转为 ARCHIVED")
    void shouldArchiveSolution() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.DEPRECATED);
        when(rbacService.hasScope(1L, "solution:maintain")).thenReturn(true);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(solutionMapper.update(solution)).thenReturn(1);

        solutionService.transitionToArchived(10L, 1L);

        assertEquals(SolutionStatus.ARCHIVED, solution.getStatus());
    }

    @Test
    @DisplayName("transitionToArchived 无 solution:maintain 权限应抛异常")
    void shouldRejectArchiveWithoutPermission() {
        when(rbacService.hasScope(1L, "solution:maintain")).thenReturn(false);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.transitionToArchived(10L, 1L));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("transitionToArchived 非 DEPRECATED 状态应抛异常")
    void shouldRejectArchiveFromInvalidStatus() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.VERIFIED);
        when(rbacService.hasScope(1L, "solution:maintain")).thenReturn(true);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.transitionToArchived(10L, 1L));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("乐观锁冲突应抛异常")
    void shouldThrowOnOptimisticLockConflict() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.NEEDS_REVIEW);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(solutionMapper.update(solution)).thenReturn(0);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.transitionAfterReviewApproved(10L, 2L));

        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), ex.getCode());
    }

    // ========== 边界用例测试 ==========

    @Test
    @DisplayName("createFromProjectCase null userId 应抛 UNAUTHORIZED")
    void shouldRejectCreateWithNullUserId() {
        BizException ex = assertThrows(BizException.class,
                () -> solutionService.createFromProjectCase(null, SolutionCreateFromProjectCaseRequest.builder()
                        .projectCaseId(77L)
                        .title("Test")
                        .build()));

        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFromProjectCase 无 solution:write 权限应抛 FORBIDDEN")
    void shouldRejectCreateWithoutWriteScope() {
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(false);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.createFromProjectCase(1L, SolutionCreateFromProjectCaseRequest.builder()
                        .projectCaseId(77L)
                        .title("Test")
                        .build()));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFromProjectCase request 为 null 应抛异常")
    void shouldRejectCreateWithNullRequest() {
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.createFromProjectCase(1L, null));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFromProjectCase projectCaseId 无效应抛异常")
    void shouldRejectCreateWithInvalidProjectCaseId() {
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.createFromProjectCase(1L, SolutionCreateFromProjectCaseRequest.builder()
                        .projectCaseId(null)
                        .title("Test")
                        .build()));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFromProjectCase title 为空应抛异常")
    void shouldRejectCreateWithEmptyTitle() {
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.createFromProjectCase(1L, SolutionCreateFromProjectCaseRequest.builder()
                        .projectCaseId(77L)
                        .title("")
                        .build()));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFromProjectCase ProjectCase 不存在应抛异常")
    void shouldRejectCreateWhenProjectCaseNotFound() {
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);
        when(solutionMapper.selectBySourceCaseId(77L)).thenReturn(null);
        when(projectCaseMapper.selectActiveById(77L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.createFromProjectCase(1L, SolutionCreateFromProjectCaseRequest.builder()
                        .projectCaseId(77L)
                        .title("Test")
                        .build()));

        assertEquals(ErrorCode.PROJECT_CASE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFromProjectCase 无权使用 ProjectCase 应抛异常")
    void shouldRejectCreateWhenNoAccessToProjectCase() {
        ProjectCaseEntity projectCase = baseProjectCase();
        projectCase.setAuthorId(2L);
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);
        when(solutionMapper.selectBySourceCaseId(77L)).thenReturn(null);
        when(projectCaseMapper.selectActiveById(77L)).thenReturn(projectCase);
        when(rbacService.isMember(1L, 100L)).thenReturn(false);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.createFromProjectCase(1L, SolutionCreateFromProjectCaseRequest.builder()
                        .projectCaseId(77L)
                        .title("Test")
                        .build()));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFromProjectCase visibilityScope 为 ENTERPRISE 应抛异常")
    void shouldRejectEnterpriseVisibilityScope() {
        ProjectCaseEntity projectCase = baseProjectCase();
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);
        when(solutionMapper.selectBySourceCaseId(77L)).thenReturn(null);
        when(projectCaseMapper.selectActiveById(77L)).thenReturn(projectCase);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.createFromProjectCase(1L, SolutionCreateFromProjectCaseRequest.builder()
                        .projectCaseId(77L)
                        .title("Test")
                        .visibilityScope("enterprise")
                        .build()));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFromProjectCase 已存在本人 Solution 应复用")
    void shouldReuseExistingSolutionFromSameAuthor() throws Exception {
        SolutionEntity existingSolution = baseSolution();
        existingSolution.setAuthorId(1L); // 当前 userId 必须是 author 才能复用
        when(rbacService.hasScope(1L, "solution:write")).thenReturn(true);
        when(solutionMapper.selectBySourceCaseId(77L)).thenReturn(existingSolution);
        when(solutionVersionMapper.selectBySolutionId(10L)).thenReturn(List.of());
        when(feedbackMapper.selectFeedbackStatsBySolutionId(10L)).thenReturn(List.of());

        var result = solutionService.createFromProjectCase(1L, SolutionCreateFromProjectCaseRequest.builder()
                .projectCaseId(77L)
                .title("Duplicate")
                .build());

        assertNotNull(result);
        assertEquals("SOL-010", result.getSolutionCode());
    }

    @Test
    @DisplayName("listPublicSolutions limit 为 null 应使用默认值")
    void shouldUseDefaultLimitWhenNull() {
        when(solutionMapper.searchVisibleSolutions(anyString(), any(), anyList(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(baseSolution()));

        var result = solutionService.listPublicSolutions("spring", null, null, null, null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("listPublicSolutions minVerificationLevel 超出范围应抛异常")
    void shouldRejectInvalidMinVerificationLevel() {
        BizException ex = assertThrows(BizException.class,
                () -> solutionService.listPublicSolutions("spring", null, null, 10, null));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("listPublicSolutions minVerificationLevel 超出上限 5 应抛异常")
    void shouldCapMinVerificationLevelAtL5() {
        // 当前实现：minVerificationLevel > L5(5) 直接抛 PARAM_INVALID，
        // 不做静默修正。这是有意为之：让调用方明确感知参数错误。
        // 因此这里不能 mock searchVisibleSolutions，否则会触发 UnnecessaryStubbing。

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.listPublicSolutions("spring", null, null, 6, null));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("minVerificationLevel"));
    }

    @Test
    @DisplayName("getDetail null solutionId 应抛异常")
    void shouldRejectNullSolutionIdForGetDetail() {
        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getDetail(1L, null));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getPublicDetail null solutionId 应抛异常")
    void shouldRejectNullSolutionIdForGetPublicDetail() {
        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getPublicDetail(null));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getPublicDetail solution 不存在应抛异常")
    void shouldThrowWhenPublicDetailSolutionNotFound() {
        when(solutionMapper.selectActiveById(10L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getPublicDetail(10L));

        assertEquals(ErrorCode.SOLUTION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getPublicDetail 低验证等级 solution 应隐藏")
    void shouldHideLowVerificationFromPublicDetail() {
        SolutionEntity solution = baseSolution();
        solution.setVerificationLevel(VerificationLevel.L0);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getPublicDetail(10L));

        assertEquals(ErrorCode.SOLUTION_NOT_FOUND.getCode(), ex.getCode());
    }

    private SolutionEntity baseSolution() {
        SolutionEntity solution = new SolutionEntity();
        solution.setId(10L);
        solution.setSolutionCode("SOL-010");
        solution.setTitle("Solution Detail");
        solution.setWorkspaceId(100L);
        solution.setVerificationLevel(VerificationLevel.L2);
        solution.setRiskLevel(RiskLevel.R1.getLevel());
        solution.setStatus(SolutionStatus.VERIFIED);
        solution.setVisibilityScope(VisibilityScope.PUBLIC);
        return solution;
    }

    private ProjectCaseEntity baseProjectCase() {
        ProjectCaseEntity projectCase = new ProjectCaseEntity();
        projectCase.setId(77L);
        projectCase.setTraceId(66L);
        projectCase.setWorkspaceId(100L);
        projectCase.setProjectId(200L);
        projectCase.setAuthorId(1L);
        projectCase.setVisibilityScope("private");
        projectCase.setLicenseScope("open_source");
        projectCase.setRedactionStatus("complete");
        projectCase.setStatus("private");
        return projectCase;
    }
}
