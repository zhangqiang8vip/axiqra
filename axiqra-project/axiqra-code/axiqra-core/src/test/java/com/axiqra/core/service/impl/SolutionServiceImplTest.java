package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.FeedbackEntity;
import com.axiqra.common.domain.entity.SolutionEntity;
import com.axiqra.common.domain.entity.SolutionVersionEntity;
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
    @DisplayName("草稿态 solution 应抛 SOLUTION_NOT_VERIFIED")
    void shouldRejectDraftSolution() {
        SolutionEntity solution = baseSolution();
        solution.setStatus(SolutionStatus.DRAFT);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getDetail(1L, 10L));

        assertEquals(ErrorCode.SOLUTION_NOT_VERIFIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("高风险 solution 应抛 SOLUTION_QUARANTINED")
    void shouldRejectHighRiskSolution() {
        SolutionEntity solution = baseSolution();
        solution.setRiskLevel(RiskLevel.R4);
        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);

        BizException ex = assertThrows(BizException.class,
                () -> solutionService.getDetail(1L, 10L));

        assertEquals(ErrorCode.SOLUTION_QUARANTINED.getCode(), ex.getCode());
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
        version.setIsActive(1);

        FeedbackEntity worked = new FeedbackEntity().setFeedbackType("worked");
        FeedbackEntity failed = new FeedbackEntity().setFeedbackType("failed");

        when(solutionMapper.selectActiveById(10L)).thenReturn(solution);
        when(rbacService.isMember(1L, 100L)).thenReturn(true);
        when(solutionVersionMapper.selectBySolutionId(10L)).thenReturn(List.of(version));
        when(feedbackMapper.selectBySolutionId(10L)).thenReturn(List.of(worked, failed));

        var result = solutionService.getDetail(1L, 10L);

        assertNotNull(result);
        assertEquals("SOL-010", result.getSolutionCode());
        assertEquals(1, result.getFeedbackStats().getWorkedCount());
        assertEquals(1, result.getFeedbackStats().getFailedCount());
        assertEquals(101L, result.getActiveVersion().getId());
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
