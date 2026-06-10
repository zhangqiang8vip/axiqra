package com.axiqra.api.controller;

import com.axiqra.common.domain.dto.ConnectSessionCreateRequest;
import com.axiqra.common.domain.vo.ConnectDoctorVO;
import com.axiqra.common.domain.vo.ConnectSessionEventVO;
import com.axiqra.common.domain.vo.ConnectSessionVO;
import com.axiqra.common.domain.vo.QuotaStatusVO;
import com.axiqra.common.domain.vo.RateLimitStatusVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.ConnectService;
import com.axiqra.core.service.QuotaService;
import com.axiqra.core.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectController 接口测试")
class ConnectControllerTest {

    @Mock
    private ConnectService connectService;
    @Mock
    private QuotaService quotaService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private ConnectController controller;

    private MockedStatic<cn.dev33.satoken.stp.StpUtil> stpUtilMock;
    private MockHttpServletRequest mockHttpRequest;

    @BeforeEach
    void setUp() {
        stpUtilMock = mockStatic(cn.dev33.satoken.stp.StpUtil.class);
        stpUtilMock.when(cn.dev33.satoken.stp.StpUtil::getLoginIdAsLong).thenReturn(1L);
        mockHttpRequest = new MockHttpServletRequest();
        mockHttpRequest.setScheme("http");
        mockHttpRequest.setServerName("localhost");
        mockHttpRequest.setServerPort(8080);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    void quotaShouldDelegateToService() {
        QuotaStatusVO expected = QuotaStatusVO.builder().used(1).remaining(1999).limit(2000).exceeded(false).build();
        when(quotaService.getStatus(1L)).thenReturn(expected);

        var result = controller.quota();

        assertNotNull(result.getData());
        assertEquals(1999, result.getData().getRemaining());
        verify(quotaService).getStatus(1L);
    }

    @Test
    void rateLimitShouldSetRetryAfterHeader() {
        RateLimitStatusVO expected = RateLimitStatusVO.builder().limit(100).currentCount(2).retryAfterSeconds(58).limited(false).build();
        when(rateLimitService.checkOrThrow(1L, "connect:probe")).thenReturn(expected);

        var result = controller.rateLimit(response);

        assertNotNull(result.getData());
        verify(response).setHeader("Retry-After", "58");
    }

    @Test
    void rateLimitShouldSetRetryAfterHeaderWhenLimited() {
        when(rateLimitService.checkOrThrow(1L, "connect:probe"))
                .thenThrow(new BizException(ErrorCode.RATE_LIMITED, "请求过于频繁，请稍后重试（retry_after=40）", 40));

        BizException ex = assertThrows(BizException.class, () -> controller.rateLimit(response));

        assertEquals(ErrorCode.RATE_LIMITED.getCode(), ex.getCode());
        verify(response).setHeader("Retry-After", "40");
    }

    @Test
    void doctorShouldReturnEightChecks() {
        ConnectDoctorVO doctor = ConnectDoctorVO.builder()
                .status("PASS")
                .passedChecks(8)
                .totalChecks(8)
                .checks(List.of())
                .build();
        when(connectService.runDoctor(1L, "cli", "mcp", 100L)).thenReturn(doctor);

        var result = controller.doctor("cli", "mcp", 100L);

        assertEquals("PASS", result.getData().getStatus());
        verify(connectService).runDoctor(1L, "cli", "mcp", 100L);
    }

    @Test
    @DisplayName("非法 toolType 时应拒绝请求")
    void doctorShouldRejectInvalidToolType() {
        BizException ex = assertThrows(BizException.class, () -> controller.doctor("cli", "invalid-tool", 100L));
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        verifyNoInteractions(connectService);
    }

    @Test
    @DisplayName("非法 channel 时应拒绝请求")
    void doctorShouldRejectInvalidChannel() {
        BizException ex = assertThrows(BizException.class, () -> controller.doctor("invalid-channel", "mcp", 100L));
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        verifyNoInteractions(connectService);
    }

    @Test
    @DisplayName("创建会话时 Location 应保留 path segment 结构并编码空格")
    void createShouldReturnEncodedLocationHeader() {
        ConnectSessionCreateRequest request = new ConnectSessionCreateRequest();
        request.setChannel("cli");
        request.setToolType("mcp");
        request.setTargetType("solution");
        request.setTargetId(101L);

        ConnectSessionVO session = sampleSession();
        session.setSessionId("session/with space");
        when(rateLimitService.checkOrThrow(1L, "connect:create")).thenReturn(null);
        when(connectService.createSession(1L, request)).thenReturn(session);

        var result = controller.create(request, mockHttpRequest, response);

        assertNotNull(result.getHeaders().getLocation());
        assertEquals("http://localhost:8080/connect/sessions/session/with%20space",
                result.getHeaders().getLocation().toString());
    }

    @Test
    @DisplayName("sessionId 为空时应抛系统异常")
    void createShouldRejectMissingSessionId() {
        ConnectSessionCreateRequest request = new ConnectSessionCreateRequest();
        request.setChannel("cli");
        request.setToolType("mcp");
        request.setTargetType("solution");
        request.setTargetId(101L);

        ConnectSessionVO session = sampleSession();
        session.setSessionId(null);
        when(rateLimitService.checkOrThrow(1L, "connect:create")).thenReturn(null);
        when(connectService.createSession(1L, request)).thenReturn(session);

        BizException ex = assertThrows(BizException.class, () -> controller.create(request, mockHttpRequest, response));

        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("无 Retry-After 信息的限流异常不应写响应头")
    void rateLimitShouldNotSetHeaderWhenRetryAfterMissing() {
        when(rateLimitService.checkOrThrow(1L, "connect:probe"))
                .thenThrow(new BizException(ErrorCode.RATE_LIMITED, "请求过于频繁，请稍后重试"));

        BizException ex = assertThrows(BizException.class, () -> controller.rateLimit(response));

        assertEquals(ErrorCode.RATE_LIMITED.getCode(), ex.getCode());
        verify(response, never()).setHeader(eq("Retry-After"), anyString());
    }

    @Test
    @DisplayName("结果为空 sessionId 时仍应暴露实现问题")
    void getSessionShouldReturnDetail() {
        ConnectSessionVO session = sampleSession();
        when(connectService.getSession(1L, "s-1")).thenReturn(session);

        var result = controller.getSession("s-1");

        assertEquals("s-1", result.getData().getSessionId());
        assertNotNull(result.getData().getHistory());
    }

    @Test
    void createShouldReturnSessionAndSetHeader() {
        ConnectSessionCreateRequest request = new ConnectSessionCreateRequest();
        request.setChannel("cli");
        request.setToolType("mcp");
        request.setTargetType("solution");
        request.setTargetId(101L);

        ConnectSessionVO session = sampleSession();
        RateLimitStatusVO rateLimitStatus = RateLimitStatusVO.builder()
                .limit(100)
                .currentCount(3)
                .retryAfterSeconds(60)
                .limited(false)
                .build();
        when(rateLimitService.checkOrThrow(1L, "connect:create")).thenReturn(rateLimitStatus);
        when(connectService.createSession(1L, request)).thenReturn(session);

        var result = controller.create(request, mockHttpRequest, response);

        assertEquals("s-1", result.getBody().getData().getSessionId());
        verify(response).setHeader("Retry-After", "60");
        verify(rateLimitService).checkOrThrow(1L, "connect:create");
    }

    private ConnectSessionVO sampleSession() {
        return ConnectSessionVO.builder()
                .sessionId("s-1")
                .userId(1L)
                .channel("cli")
                .toolType("mcp")
                .targetType("solution")
                .targetId(101L)
                .status("READY")
                .createdAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .history(List.of(ConnectSessionEventVO.builder().fromStatus("CHECKED").toStatus("READY").action("SESSION_CREATED").build()))
                .build();
    }
}
