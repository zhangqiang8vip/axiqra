package com.axiqra.core.service.impl;

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
import com.axiqra.core.mapper.SolutionMapper;
import com.axiqra.core.mapper.SolutionVersionMapper;
import com.axiqra.core.service.RbacService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolutionServiceImpl 单元测试")
class SolutionServiceImplTest {

    @Mock
    private SolutionMapper solutionMapper;

    @Mock
    private SolutionVersionMapper solutionVersionMapper;

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private RbacService rbacService;

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
        solution.setRiskLevel(RiskLevel.R4);
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
        solution.setRiskLevel(RiskLevel.R4);
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

    private SolutionEntity baseSolution() {
        SolutionEntity solution = new SolutionEntity();
        solution.setId(10L);
        solution.setSolutionCode("SOL-010");
        solution.setTitle("Solution Detail");
        solution.setWorkspaceId(100L);
        solution.setVerificationLevel(VerificationLevel.L2);
        solution.setRiskLevel(RiskLevel.R1);
        solution.setStatus(SolutionStatus.VERIFIED);
        solution.setVisibilityScope(VisibilityScope.PUBLIC);
        return solution;
    }
}
