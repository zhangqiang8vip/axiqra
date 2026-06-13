package com.axiqra.api.controller;

import com.axiqra.common.domain.dto.FeedbackSubmitRequest;
import com.axiqra.common.domain.vo.FeedbackDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.FeedbackService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackController 接口测试")
class FeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    @InjectMocks
    private FeedbackController controller;

    private MockedStatic<cn.dev33.satoken.stp.StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        stpUtilMock = mockStatic(cn.dev33.satoken.stp.StpUtil.class);
        stpUtilMock.when(cn.dev33.satoken.stp.StpUtil::getLoginIdAsLong).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    @DisplayName("POST /api/v1/feedbacks 应调用 service.submitFeedback")
    void submitFeedbackShouldDelegate() {
        FeedbackSubmitRequest request = new FeedbackSubmitRequest();
        request.setInvocationId(77L);
        request.setFeedbackType("worked");
        request.setFeedbackContent("很好用");

        FeedbackDetailVO vo = new FeedbackDetailVO();
        vo.setId(5L);
        vo.setInvocationId(77L);
        vo.setUserId(1L);
        vo.setFeedbackType("worked");
        vo.setFeedbackTypeDesc("方案有效");
        vo.setStatus("accepted");

        when(feedbackService.submitFeedback(eq(1L), any(FeedbackSubmitRequest.class))).thenReturn(vo);

        var result = controller.submitFeedback(request);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(5L, result.getBody().getData().getId());
        verify(feedbackService).submitFeedback(eq(1L), any(FeedbackSubmitRequest.class));
    }

    @Test
    @DisplayName("submitFeedback 校验失败应抛出约束异常")
    void submitFeedbackWithInvalidRequest() {
        FeedbackSubmitRequest request = new FeedbackSubmitRequest();
        request.setInvocationId(null);
        request.setFeedbackType(null);

        doThrow(new BizException(ErrorCode.PARAM_INVALID))
                .when(feedbackService).submitFeedback(eq(1L), any(FeedbackSubmitRequest.class));

        assertThrows(BizException.class, () -> {
            controller.submitFeedback(request);
        });
    }

    @Test
    @DisplayName("GET /api/v1/feedbacks 应调用 service.listFeedbacks")
    void listFeedbacksShouldDelegate() {
        FeedbackDetailVO vo = new FeedbackDetailVO();
        vo.setId(5L);
        vo.setFeedbackType("worked");
        vo.setFeedbackTypeDesc("方案有效");

        when(feedbackService.listFeedbacks(1L, "solution", 77L))
                .thenReturn(List.of(vo));

        var result = controller.listFeedbacks("solution", 77L);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(1, result.getBody().getData().size());
        verify(feedbackService).listFeedbacks(1L, "solution", 77L);
    }

    @Test
    @DisplayName("GET /api/v1/feedbacks 返回空列表")
    void listFeedbacksShouldReturnEmptyList() {
        when(feedbackService.listFeedbacks(1L, "solution", 77L))
                .thenReturn(List.of());

        var result = controller.listFeedbacks("solution", 77L);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(0, result.getBody().getData().size());
    }

    @Test
    @DisplayName("GET /api/v1/feedbacks/solutions/{id}/stats 应返回统计")
    void getStatsShouldReturnStats() {
        SolutionFeedbackStatsVO stats = new SolutionFeedbackStatsVO();
        stats.setWorkedCount(10L);
        stats.setTotalCount(15L);

        when(feedbackService.getSolutionFeedbackStats(77L)).thenReturn(stats);

        var result = controller.getSolutionFeedbackStats(77L);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(10L, result.getBody().getData().getWorkedCount());
        verify(feedbackService).getSolutionFeedbackStats(77L);
    }

    @Test
    @DisplayName("GET /api/v1/feedbacks/solutions/{id}/stats 返回全零统计")
    void getStatsShouldHandleZeroCounts() {
        SolutionFeedbackStatsVO stats = new SolutionFeedbackStatsVO();
        stats.setWorkedCount(0L);
        stats.setTotalCount(0L);

        when(feedbackService.getSolutionFeedbackStats(99L)).thenReturn(stats);

        var result = controller.getSolutionFeedbackStats(99L);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(0L, result.getBody().getData().getWorkedCount());
        assertEquals(0L, result.getBody().getData().getTotalCount());
    }
}
