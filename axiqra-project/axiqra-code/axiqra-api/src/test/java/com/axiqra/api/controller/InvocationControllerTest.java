package com.axiqra.api.controller;

import com.axiqra.common.domain.dto.InvocationReportRequest;
import com.axiqra.common.domain.vo.InvocationDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.core.service.InvocationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvocationController 接口测试")
class InvocationControllerTest {

    @Mock
    private InvocationService invocationService;

    @InjectMocks
    private InvocationController controller;

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
    @DisplayName("POST /api/v1/invocations 应调用 service.reportInvocation")
    void reportInvocationShouldDelegate() {
        InvocationReportRequest request = new InvocationReportRequest();
        request.setRequestId("req-1");
        request.setResultType("worked");
        request.setToolType("cursor");
        request.setTargetType("solution");
        request.setTargetId(77L);
        request.setWorkspaceId(100L);

        InvocationDetailVO expected = new InvocationDetailVO();
        expected.setId(10L);
        expected.setRequestId("req-1");
        expected.setUserId(1L);
        expected.setResultType("worked");

        when(invocationService.reportInvocation(1L, request)).thenReturn(expected);

        var result = controller.reportInvocation(request);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(10L, result.getBody().getData().getId());
        verify(invocationService).reportInvocation(1L, request);
    }

    @Test
    @DisplayName("GET /api/v1/invocations/{id} 已授权时应返回详情")
    void getDetailShouldReturnWhenAuthorized() {
        InvocationDetailVO expected = new InvocationDetailVO();
        expected.setId(10L);
        expected.setRequestId("req-1");

        when(invocationService.isUserAuthorized(1L, 10L)).thenReturn(true);
        when(invocationService.getInvocationDetail(1L, 10L)).thenReturn(expected);

        var result = controller.getInvocationDetail(10L);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(10L, result.getBody().getData().getId());
        verify(invocationService).isUserAuthorized(1L, 10L);
        verify(invocationService).getInvocationDetail(1L, 10L);
    }

    @Test
    @DisplayName("GET /api/v1/invocations/{id} 未授权时应抛出 BizException")
    void getDetailShouldThrowBizExceptionWhenUnauthorized() {
        when(invocationService.isUserAuthorized(1L, 10L)).thenReturn(false);

        BizException ex = assertThrows(BizException.class,
                () -> controller.getInvocationDetail(10L));

        assertEquals("无权限查看该调用记录", ex.getMessage());
    }

    @Test
    @DisplayName("GET /api/v1/invocations/solutions/{id}/feedback-stats 应返回统计数据")
    void getFeedbackStatsShouldReturnStats() {
        SolutionFeedbackStatsVO expected = new SolutionFeedbackStatsVO();
        expected.setWorkedCount(5L);
        expected.setFailedCount(2L);
        expected.setTotalCount(7L);

        when(invocationService.getSolutionFeedbackStats(77L)).thenReturn(expected);

        var result = controller.getSolutionFeedbackStats(77L);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(5L, result.getBody().getData().getWorkedCount());
        assertEquals(7L, result.getBody().getData().getTotalCount());
        verify(invocationService).getSolutionFeedbackStats(77L);
    }
}
