package com.axiqra.api.controller;

import com.axiqra.common.domain.vo.ReviewDetailVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.ReviewService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewController 接口测试")
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController controller;

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
    @DisplayName("GET /api/v1/reviews/pending 应返回待审列表")
    void getPendingShouldReturnList() {
        ReviewDetailVO vo = new ReviewDetailVO();
        vo.setId(10L);
        vo.setStatus("pending");

        when(reviewService.getPendingReviews(1L, "human", 20)).thenReturn(List.of(vo));

        var result = controller.getPendingReviews("human", 20);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(1, result.getBody().getData().size());
        verify(reviewService).getPendingReviews(1L, "human", 20);
    }

    @Test
    @DisplayName("GET /api/v1/reviews/{id} 应返回详情")
    void getDetailShouldReturn() {
        ReviewDetailVO expected = new ReviewDetailVO();
        expected.setId(10L);
        expected.setStatus("pending");

        when(reviewService.getReviewDetail(1L, 10L)).thenReturn(expected);

        var result = controller.getReviewDetail(10L);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(10L, result.getBody().getData().getId());
        verify(reviewService).getReviewDetail(1L, 10L);
    }

    @Test
    @DisplayName("POST /api/v1/reviews/{id}/approve 应调用 approve")
    void approveShouldDelegate() {
        ReviewDetailVO expected = new ReviewDetailVO();
        expected.setId(10L);
        expected.setStatus("approved");

        when(reviewService.approve(1L, 10L, "CODE_OK", "LGTM")).thenReturn(expected);

        var result = controller.approve(10L, "CODE_OK", "LGTM");

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals("approved", result.getBody().getData().getStatus());
        verify(reviewService).approve(1L, 10L, "CODE_OK", "LGTM");
    }

    @Test
    @DisplayName("POST /api/v1/reviews/{id}/reject 应调用 reject")
    void rejectShouldDelegate() {
        ReviewDetailVO expected = new ReviewDetailVO();
        expected.setId(10L);
        expected.setStatus("rejected");

        when(reviewService.reject(1L, 10L, "RISK_HIGH", "")).thenReturn(expected);

        var result = controller.reject(10L, "RISK_HIGH", "");

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals("rejected", result.getBody().getData().getStatus());
        verify(reviewService).reject(1L, 10L, "RISK_HIGH", "");
    }

    @Test
    @DisplayName("POST /api/v1/reviews/{id}/quarantine 应调用 quarantine")
    void quarantineShouldDelegate() {
        ReviewDetailVO expected = new ReviewDetailVO();
        expected.setId(10L);
        expected.setStatus("quarantined");

        when(reviewService.quarantine(1L, 10L, "MALICIOUS", "")).thenReturn(expected);

        var result = controller.quarantine(10L, "MALICIOUS", "");

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals("quarantined", result.getBody().getData().getStatus());
        verify(reviewService).quarantine(1L, 10L, "MALICIOUS", "");
    }

    @Test
    @DisplayName("POST /api/v1/reviews/{id}/appeal 应调用 appeal")
    void appealShouldDelegate() {
        ReviewDetailVO expected = new ReviewDetailVO();
        expected.setId(10L);
        expected.setStatus("appeal_in_progress");

        when(reviewService.appeal(1L, 10L, "I disagree")).thenReturn(expected);

        var result = controller.appeal(10L, "I disagree");

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals("appeal_in_progress", result.getBody().getData().getStatus());
        verify(reviewService).appeal(1L, 10L, "I disagree");
    }
}
