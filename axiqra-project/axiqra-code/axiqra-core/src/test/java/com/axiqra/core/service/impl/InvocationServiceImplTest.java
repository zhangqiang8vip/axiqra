package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.InvocationReportRequest;
import com.axiqra.common.domain.entity.InvocationEntity;
import com.axiqra.common.domain.vo.InvocationDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.InvocationMapper;
import com.axiqra.core.service.FeedbackService;
import com.axiqra.core.service.RateLimitService;
import com.axiqra.common.domain.vo.RateLimitStatusVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvocationServiceImpl 单元测试")
class InvocationServiceImplTest {

    @Mock
    private InvocationMapper invocationMapper;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ObjectProvider<FeedbackService> feedbackServiceProvider;

    private InvocationServiceImpl invocationService;

    @Test
    @DisplayName("重复 requestId 应幂等返回已有记录（insert 抛唯一约束异常）")
    void shouldReturnExistingWhenRequestIdDuplicate() {
        InvocationEntity existing = invocationEntity(10L, "req-1");
        when(invocationMapper.selectByRequestId("req-1")).thenReturn(existing);
        when(invocationMapper.insert(any(InvocationEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        InvocationServiceImpl service = new InvocationServiceImpl(invocationMapper, rateLimitService, feedbackServiceProvider);

        InvocationReportRequest request = new InvocationReportRequest();
        request.setRequestId("req-1");
        request.setResultType("worked");
        request.setToolType("cursor");
        request.setTargetType("solution");
        request.setTargetId(77L);
        request.setWorkspaceId(100L);

        InvocationDetailVO result = service.reportInvocation(1L, request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(invocationMapper).insert(any(InvocationEntity.class));
        verify(rateLimitService).checkOrThrow(1L, "invocation:report");
    }

    @Test
    @DisplayName("新请求应创建记录并调用限流检查")
    void shouldCreateRecordAndCheckRateLimit() {
        ArgumentCaptor<InvocationEntity> captor = ArgumentCaptor.forClass(InvocationEntity.class);

        InvocationServiceImpl service = new InvocationServiceImpl(invocationMapper, rateLimitService, feedbackServiceProvider);

        InvocationReportRequest request = new InvocationReportRequest();
        request.setRequestId("req-new");
        request.setResultType("worked");
        request.setToolType("cursor");
        request.setTargetType("solution");
        request.setTargetId(77L);
        request.setWorkspaceId(100L);
        request.setInvocationCode("cursor:agent:run");
        request.setRiskLevel(1);
        request.setRequiredConfirmation(0);
        request.setConfirmationObtained(0);

        InvocationDetailVO result = service.reportInvocation(1L, request);

        assertNotNull(result);
        verify(rateLimitService).checkOrThrow(1L, "invocation:report");
        verify(invocationMapper).insert(captor.capture());
        assertEquals("req-new", captor.getValue().getRequestId());
        assertEquals("worked", captor.getValue().getResultType());
        assertEquals("cursor", captor.getValue().getToolType());
    }

    @Test
    @DisplayName("限流超限应抛出异常，不执行 insert")
    void shouldThrowWhenRateLimited() {
        when(rateLimitService.checkOrThrow(1L, "invocation:report"))
                .thenThrow(new BizException(ErrorCode.RATE_LIMITED));

        InvocationServiceImpl service = new InvocationServiceImpl(invocationMapper, rateLimitService, feedbackServiceProvider);

        InvocationReportRequest request = new InvocationReportRequest();
        request.setRequestId("req-new");
        request.setResultType("worked");
        request.setToolType("cursor");
        request.setTargetType("solution");
        request.setTargetId(77L);
        request.setWorkspaceId(100L);

        BizException ex = assertThrows(BizException.class,
                () -> service.reportInvocation(1L, request));

        assertEquals(ErrorCode.RATE_LIMITED.getCode(), ex.getCode());
        verify(invocationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("详情读取不存在应抛异常")
    void shouldThrowWhenDetailNotFound() {
        when(invocationMapper.selectById(999L)).thenReturn(null);

        InvocationServiceImpl service = new InvocationServiceImpl(invocationMapper, rateLimitService, feedbackServiceProvider);

        BizException ex = assertThrows(BizException.class,
                () -> service.getInvocationDetail(1L, 999L));

        assertEquals(ErrorCode.INVOCATION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("详情读取软删除记录应抛异常")
    void shouldThrowWhenDetailDeleted() {
        InvocationEntity entity = invocationEntity(10L, "req-1");
        entity.setDeleted(true);
        when(invocationMapper.selectById(10L)).thenReturn(entity);

        InvocationServiceImpl service = new InvocationServiceImpl(invocationMapper, rateLimitService, feedbackServiceProvider);

        BizException ex = assertThrows(BizException.class,
                () -> service.getInvocationDetail(1L, 10L));

        assertEquals(ErrorCode.INVOCATION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("详情读取正常应返回 VO")
    void shouldReturnDetail() {
        InvocationEntity entity = invocationEntity(10L, "req-1");
        when(invocationMapper.selectById(10L)).thenReturn(entity);

        InvocationServiceImpl service = new InvocationServiceImpl(invocationMapper, rateLimitService, feedbackServiceProvider);

        InvocationDetailVO result = service.getInvocationDetail(1L, 10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("req-1", result.getRequestId());
    }

    @Test
    @DisplayName("getSolutionFeedbackStats 应委托 FeedbackService")
    void shouldDelegateFeedbackStatsToFeedbackService() {
        SolutionFeedbackStatsVO expectedStats = SolutionFeedbackStatsVO.builder()
                .workedCount(5L).totalCount(5L).build();
        FeedbackService mockFeedbackService = mock(FeedbackService.class);
        when(feedbackServiceProvider.getIfAvailable()).thenReturn(mockFeedbackService);
        when(mockFeedbackService.getSolutionFeedbackStats(77L)).thenReturn(expectedStats);

        InvocationServiceImpl service = new InvocationServiceImpl(invocationMapper, rateLimitService, feedbackServiceProvider);
        SolutionFeedbackStatsVO result = service.getSolutionFeedbackStats(77L);

        assertNotNull(result);
        assertEquals(5L, result.getWorkedCount());
        verify(mockFeedbackService).getSolutionFeedbackStats(77L);
    }

    @Test
    @DisplayName("getSolutionFeedbackStats 当 FeedbackService 不可用时应返回空统计")
    void shouldReturnEmptyStatsWhenFeedbackServiceUnavailable() {
        when(feedbackServiceProvider.getIfAvailable()).thenReturn(null);

        InvocationServiceImpl service = new InvocationServiceImpl(invocationMapper, rateLimitService, feedbackServiceProvider);
        SolutionFeedbackStatsVO result = service.getSolutionFeedbackStats(77L);

        assertNotNull(result);
        assertEquals(0L, result.getTotalCount());
    }

    private InvocationEntity invocationEntity(Long id, String requestId) {
        InvocationEntity entity = new InvocationEntity();
        entity.setId(id);
        entity.setRequestId(requestId);
        entity.setUserId(1L);
        entity.setResultType("worked");
        entity.setToolType("cursor");
        entity.setTargetType("solution");
        entity.setTargetId(77L);
        entity.setWorkspaceId(100L);
        entity.setRiskLevel(1);
        entity.setRequiredConfirmation(0);
        entity.setConfirmationObtained(0);
        return entity;
    }
}
