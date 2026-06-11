package com.axiqra.api.controller;

import com.axiqra.common.domain.dto.TraceConfirmRequest;
import com.axiqra.common.domain.dto.TraceCreateRequest;
import com.axiqra.common.domain.vo.TraceDetailVO;
import com.axiqra.common.domain.vo.TraceEvidenceVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.TraceService;
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
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TraceController 接口测试")
class TraceControllerTest {

    @Mock
    private TraceService traceService;

    @InjectMocks
    private TraceController controller;

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
    @DisplayName("创建草稿应委托 service")
    void createDraftShouldDelegateToService() {
        TraceCreateRequest request = TraceCreateRequest.builder()
                .workspaceId(100L)
                .taskGoal("补齐 Trace 闭环")
                .outcome("完成")
                .riskLevel("R2")
                .visibilityScope("workspace")
                .evidences(List.of(TraceCreateRequest.TraceEvidenceItem.builder()
                        .uri("minio://bucket/e1")
                        .type("artifact")
                        .build()))
                .build();
        TraceDetailVO detail = detail(101L, "draft");
        when(traceService.createDraft(1L, request)).thenReturn(detail);

        var result = controller.createDraft(request);

        assertNotNull(result.getData());
        assertEquals(101L, result.getData().getId());
        verify(traceService).createDraft(1L, request);
    }

    @Test
    @DisplayName("确认应委托 service")
    void confirmShouldDelegateToService() {
        TraceConfirmRequest request = TraceConfirmRequest.builder().userConfirmation("已确认").build();
        when(traceService.confirm(1L, 101L, request)).thenReturn(detail(101L, "user_confirmed"));

        var result = controller.confirm(101L, request);

        assertEquals("user_confirmed", result.getData().getStatus());
        verify(traceService).confirm(1L, 101L, request);
    }

    @Test
    @DisplayName("提交时 service 异常应透传")
    void submitShouldPropagateBizException() {
        BizException exception = new BizException(ErrorCode.TRACE_USER_CONFIRMATION_PENDING);
        when(traceService.submit(1L, 101L)).thenThrow(exception);

        BizException thrown = assertThrows(BizException.class, () -> controller.submit(101L));

        assertEquals(ErrorCode.TRACE_USER_CONFIRMATION_PENDING.getCode(), thrown.getCode());
        verify(traceService).submit(1L, 101L);
    }

    @Test
    @DisplayName("详情读取应委托 service")
    void getDetailShouldDelegateToService() {
        when(traceService.getDetail(1L, 101L)).thenReturn(detail(101L, "submitted"));

        var result = controller.getDetail(101L);

        assertEquals("submitted", result.getData().getStatus());
        verify(traceService).getDetail(1L, 101L);
    }

    private TraceDetailVO detail(Long traceId, String status) {
        return TraceDetailVO.builder()
                .id(traceId)
                .workspaceId(100L)
                .authorId(1L)
                .taskGoal("补齐 Trace 闭环")
                .outcome("完成")
                .riskLevel("R2")
                .status(status)
                .evidences(List.of(TraceEvidenceVO.builder()
                        .id(1001L)
                        .uri("minio://bucket/e1")
                        .type("artifact")
                        .build()))
                .build();
    }
}
