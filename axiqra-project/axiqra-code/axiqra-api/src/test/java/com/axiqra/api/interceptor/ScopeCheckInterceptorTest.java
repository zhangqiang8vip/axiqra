package com.axiqra.api.interceptor;

import com.axiqra.api.annotation.RequireScope;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.PolicyEngineService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScopeCheckInterceptor 单元测试")
class ScopeCheckInterceptorTest {

    @Mock
    private PolicyEngineService policyEngineService;

    @Mock
    private jakarta.servlet.http.HttpServletRequest request;

    @Mock
    private jakarta.servlet.http.HttpServletResponse response;

    private ScopeCheckInterceptor interceptor;
    private MockedStatic<cn.dev33.satoken.stp.StpUtil> stpUtilMock;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        interceptor = new ScopeCheckInterceptor(policyEngineService);
        stpUtilMock = mockStatic(cn.dev33.satoken.stp.StpUtil.class);
        stpUtilMock.when(cn.dev33.satoken.stp.StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Nested
    @DisplayName("preHandle")
    class PreHandleTests {

        @Test
        @DisplayName("非 HandlerMethod 应直接放行")
        void shouldAllowNonHandlerMethod() {
            boolean allowed = interceptor.preHandle(request, response, new Object());

            assertTrue(allowed);
            verifyNoInteractions(policyEngineService);
        }

        @Test
        @DisplayName("无 RequireScope 注解应直接放行")
        void shouldAllowWhenAnnotationMissing() throws Exception {
            HandlerMethod method = new HandlerMethod(new ScopeAnnotatedController(),
                    ScopeAnnotatedController.class.getMethod("openEndpoint"));

            boolean allowed = interceptor.preHandle(request, response, method);

            assertTrue(allowed);
            verifyNoInteractions(policyEngineService);
        }

        @Test
        @DisplayName("ANY 模式命中任一 scope 时应放行")
        void shouldAllowWhenAnyScopeSatisfied() throws Exception {
            HandlerMethod method = new HandlerMethod(new ScopeAnnotatedController(),
                    ScopeAnnotatedController.class.getMethod("anyScopeEndpoint"));
            when(policyEngineService.hasScope(USER_ID, "search:read")).thenReturn(false);
            when(policyEngineService.hasScope(USER_ID, "solution:read")).thenReturn(true);

            boolean allowed = interceptor.preHandle(request, response, method);

            assertTrue(allowed);
            verify(policyEngineService).hasScope(USER_ID, "search:read");
            verify(policyEngineService).hasScope(USER_ID, "solution:read");
        }

        @Test
        @DisplayName("ANY 模式全部不满足时应抛 FORBIDDEN")
        void shouldThrowWhenAnyScopeUnsatisfied() throws Exception {
            HandlerMethod method = new HandlerMethod(new ScopeAnnotatedController(),
                    ScopeAnnotatedController.class.getMethod("anyScopeEndpoint"));
            when(policyEngineService.hasScope(USER_ID, "search:read")).thenReturn(false);
            when(policyEngineService.hasScope(USER_ID, "solution:read")).thenReturn(false);

            BizException exception = assertThrows(BizException.class,
                    () -> interceptor.preHandle(request, response, method));

            assertEquals(ErrorCode.FORBIDDEN.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("search:read,solution:read"));
        }

        @Test
        @DisplayName("ALL 模式全部满足时应放行")
        void shouldAllowWhenAllScopesSatisfied() throws Exception {
            HandlerMethod method = new HandlerMethod(new ScopeAnnotatedController(),
                    ScopeAnnotatedController.class.getMethod("allScopesEndpoint"));
            when(policyEngineService.hasScope(USER_ID, "search:read")).thenReturn(true);
            when(policyEngineService.hasScope(USER_ID, "trace:write")).thenReturn(true);

            boolean allowed = interceptor.preHandle(request, response, method);

            assertTrue(allowed);
            verify(policyEngineService).hasScope(USER_ID, "search:read");
            verify(policyEngineService).hasScope(USER_ID, "trace:write");
        }

        @Test
        @DisplayName("ALL 模式任一 scope 不满足时应抛 FORBIDDEN")
        void shouldThrowWhenAllScopesNotSatisfied() throws Exception {
            HandlerMethod method = new HandlerMethod(new ScopeAnnotatedController(),
                    ScopeAnnotatedController.class.getMethod("allScopesEndpoint"));
            when(policyEngineService.hasScope(USER_ID, "search:read")).thenReturn(true);
            when(policyEngineService.hasScope(USER_ID, "trace:write")).thenReturn(false);

            BizException exception = assertThrows(BizException.class,
                    () -> interceptor.preHandle(request, response, method));

            assertEquals(ErrorCode.FORBIDDEN.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("search:read,trace:write"));
        }
    }

    @SuppressWarnings("unused")
    static class ScopeAnnotatedController {

        public void openEndpoint() {
        }

        @RequireScope({"search:read", "solution:read"})
        public void anyScopeEndpoint() {
        }

        @RequireScope(value = {"search:read", "trace:write"}, mode = RequireScope.RequireMode.ALL)
        public void allScopesEndpoint() {
        }
    }
}
