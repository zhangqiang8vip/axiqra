package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.FeedbackSubmitRequest;
import com.axiqra.common.domain.entity.FeedbackEntity;
import com.axiqra.common.domain.entity.InvocationEntity;
import com.axiqra.common.domain.vo.FeedbackDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.FeedbackMapper;
import com.axiqra.core.mapper.InvocationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackServiceImpl 单元测试")
class FeedbackServiceImplTest {

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private InvocationMapper invocationMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    @Test
    @DisplayName("Invocation 不存在时应抛异常")
    void shouldThrowWhenInvocationNotFound() {
        when(invocationMapper.selectById(77L)).thenReturn(null);

        FeedbackSubmitRequest request = new FeedbackSubmitRequest();
        request.setInvocationId(77L);
        request.setFeedbackType("worked");

        BizException ex = assertThrows(BizException.class,
                () -> feedbackService.submitFeedback(1L, request));

        assertEquals(ErrorCode.INVOCATION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("Invocation 已删除时应抛异常")
    void shouldThrowWhenInvocationDeleted() {
        InvocationEntity invocation = new InvocationEntity();
        invocation.setId(77L);
        invocation.setDeleted(true);
        when(invocationMapper.selectById(77L)).thenReturn(invocation);

        FeedbackSubmitRequest request = new FeedbackSubmitRequest();
        request.setInvocationId(77L);
        request.setFeedbackType("worked");

        BizException ex = assertThrows(BizException.class,
                () -> feedbackService.submitFeedback(1L, request));

        assertEquals(ErrorCode.INVOCATION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("有效请求应创建 Feedback 并返回详情")
    void shouldCreateFeedback() {
        InvocationEntity invocation = new InvocationEntity();
        invocation.setId(77L);
        invocation.setDeleted(false);
        when(invocationMapper.selectById(77L)).thenReturn(invocation);

        ArgumentCaptor<FeedbackEntity> captor = ArgumentCaptor.forClass(FeedbackEntity.class);

        FeedbackSubmitRequest request = new FeedbackSubmitRequest();
        request.setInvocationId(77L);
        request.setFeedbackType("worked");
        request.setFeedbackContent("完美解决");
        request.setEvidenceRefs(List.of("ref-1", "ref-2"));
        request.setContextDelta("上下文");
        request.setBoundaryNotes("边界");

        FeedbackDetailVO result = feedbackService.submitFeedback(1L, request);

        assertNotNull(result);
        verify(feedbackMapper).insert(captor.capture());
        assertEquals(77L, captor.getValue().getInvocationId());
        assertEquals("worked", captor.getValue().getFeedbackType());
        assertEquals("accepted", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("evidenceRefs 应正确反序列化为 List 返回给前端")
    void shouldDeserializeEvidenceRefsToList() {
        InvocationEntity invocation = new InvocationEntity();
        invocation.setId(77L);
        invocation.setDeleted(false);
        when(invocationMapper.selectById(77L)).thenReturn(invocation);

        FeedbackSubmitRequest request = new FeedbackSubmitRequest();
        request.setInvocationId(77L);
        request.setFeedbackType("worked");
        request.setEvidenceRefs(List.of("ref-a", "ref-b"));

        FeedbackDetailVO result = feedbackService.submitFeedback(1L, request);

        assertNotNull(result.getEvidenceRefs());
        assertEquals(2, result.getEvidenceRefs().size());
        assertTrue(result.getEvidenceRefs().contains("ref-a"));
        assertTrue(result.getEvidenceRefs().contains("ref-b"));
    }

    @Test
    @DisplayName("listFeedbacks 对 solution 应查询并返回列表")
    void shouldListFeedbacksForSolution() {
        FeedbackEntity entity = feedbackEntity(1L, 77L);
        when(feedbackMapper.selectBySolutionId(77L)).thenReturn(List.of(entity));

        List<FeedbackDetailVO> result = feedbackService.listFeedbacks(1L, "solution", 77L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("worked", result.get(0).getFeedbackType());
        assertEquals("方案有效", result.get(0).getFeedbackTypeDesc());
    }

    @Test
    @DisplayName("listFeedbacks 对不支持类型应返回空列表")
    void shouldReturnEmptyForUnsupportedType() {
        List<FeedbackDetailVO> result = feedbackService.listFeedbacks(1L, "unknown", 77L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getSolutionFeedbackStats 应聚合各类型统计")
    void shouldAggregateFeedbackStats() {
        FeedbackMapper.FeedbackStatRow workedRow = mock(FeedbackMapper.FeedbackStatRow.class);
        when(workedRow.getFeedbackType()).thenReturn("worked");
        when(workedRow.getCount()).thenReturn(3L);

        FeedbackMapper.FeedbackStatRow failedRow = mock(FeedbackMapper.FeedbackStatRow.class);
        when(failedRow.getFeedbackType()).thenReturn("failed");
        when(failedRow.getCount()).thenReturn(2L);

        when(feedbackMapper.selectFeedbackStatsBySolutionId(77L))
                .thenReturn(List.of(workedRow, failedRow));

        SolutionFeedbackStatsVO result = feedbackService.getSolutionFeedbackStats(77L);

        assertEquals(3L, result.getWorkedCount());
        assertEquals(2L, result.getFailedCount());
        assertEquals(5L, result.getTotalCount());
        assertEquals(0L, result.getPartialCount());
    }

    private FeedbackMapper.FeedbackStatRow mockStatRow(String type, Long count) {
        FeedbackMapper.FeedbackStatRow row = mock(FeedbackMapper.FeedbackStatRow.class);
        when(row.getFeedbackType()).thenReturn(type);
        when(row.getCount()).thenReturn(count);
        return row;
    }

    private FeedbackEntity feedbackEntity(Long id, Long invocationId) {
        FeedbackEntity entity = new FeedbackEntity();
        entity.setId(id);
        entity.setInvocationId(invocationId);
        entity.setUserId(1L);
        entity.setFeedbackType("worked");
        entity.setFeedbackContent("test");
        entity.setStatus("accepted");
        return entity;
    }
}
