package com.axiqra.api.interceptor;

import com.axiqra.api.annotation.RequireWorkspaceRole;
import com.axiqra.api.annotation.WorkspaceRole;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.RbacService;
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
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceRoleCheckInterceptor 单元测试")
class WorkspaceRoleCheckInterceptorTest {

    @Mock
    private RbacService rbacService;

    @Mock
    private jakarta.servlet.http.HttpServletRequest request;

    @Mock
    private jakarta.servlet.http.HttpServletResponse response;

    private WorkspaceRoleCheckInterceptor interceptor;
    private MockedStatic<cn.dev33.satoken.stp.StpUtil> stpUtilMock;

    private static final Long USER_ID = 1L;
    private static final Long WORKSPACE_ID = 100L;

    @BeforeEach
    void setUp() {
        interceptor = new WorkspaceRoleCheckInterceptor(rbacService);
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
            verifyNoInteractions(rbacService);
        }

        @Test
        @DisplayName("无 RequireWorkspaceRole 注解应直接放行")
        void shouldAllowWhenAnnotationMissing() throws Exception {
            HandlerMethod method = new HandlerMethod(new WorkspaceAnnotatedController(),
                    WorkspaceAnnotatedController.class.getMethod("openEndpoint"));

            boolean allowed = interceptor.preHandle(request, response, method);

            assertTrue(allowed);
            verifyNoInteractions(rbacService);
        }

        @Test
        @DisplayName("OWNER 权限满足时应放行")
        void shouldAllowWhenOwnerRoleSatisfied() throws Exception {
            HandlerMethod method = new HandlerMethod(new WorkspaceAnnotatedController(),
                    WorkspaceAnnotatedController.class.getMethod("ownerEndpoint"));
            when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                    .thenReturn(Map.of("workspaceId", String.valueOf(WORKSPACE_ID)));
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);

            boolean allowed = interceptor.preHandle(request, response, method);

            assertTrue(allowed);
            verify(rbacService).isOwner(USER_ID, WORKSPACE_ID);
        }

        @Test
        @DisplayName("ADMIN 权限可从 query 参数读取 workspaceId")
        void shouldAllowWhenAdminRoleSatisfiedFromRequestParam() throws Exception {
            HandlerMethod method = new HandlerMethod(new WorkspaceAnnotatedController(),
                    WorkspaceAnnotatedController.class.getMethod("adminEndpoint"));
            when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(null);
            when(request.getParameter("workspaceId")).thenReturn(String.valueOf(WORKSPACE_ID));
            when(rbacService.isAdmin(USER_ID, WORKSPACE_ID)).thenReturn(true);

            boolean allowed = interceptor.preHandle(request, response, method);

            assertTrue(allowed);
            verify(rbacService).isAdmin(USER_ID, WORKSPACE_ID);
        }

        @Test
        @DisplayName("缺少 workspaceId 时应抛 PARAM_MISSING")
        void shouldThrowWhenWorkspaceIdMissing() throws Exception {
            HandlerMethod method = new HandlerMethod(new WorkspaceAnnotatedController(),
                    WorkspaceAnnotatedController.class.getMethod("ownerEndpoint"));
            when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(Map.of());
            when(request.getParameter("workspaceId")).thenReturn(null);

            BizException exception = assertThrows(BizException.class,
                    () -> interceptor.preHandle(request, response, method));

            assertEquals(ErrorCode.PARAM_MISSING.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("workspaceId 非数字时应抛 PARAM_INVALID")
        void shouldThrowWhenWorkspaceIdNotNumeric() throws Exception {
            HandlerMethod method = new HandlerMethod(new WorkspaceAnnotatedController(),
                    WorkspaceAnnotatedController.class.getMethod("ownerEndpoint"));
            when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                    .thenReturn(Map.of("workspaceId", "abc"));

            BizException exception = assertThrows(BizException.class,
                    () -> interceptor.preHandle(request, response, method));

            assertEquals(ErrorCode.PARAM_INVALID.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("参数格式错误"));
        }

        @Test
        @DisplayName("workspaceId 小于等于零时应抛 PARAM_INVALID")
        void shouldThrowWhenWorkspaceIdNotPositive() throws Exception {
            HandlerMethod method = new HandlerMethod(new WorkspaceAnnotatedController(),
                    WorkspaceAnnotatedController.class.getMethod("ownerEndpoint"));
            when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                    .thenReturn(Map.of("workspaceId", "0"));

            BizException exception = assertThrows(BizException.class,
                    () -> interceptor.preHandle(request, response, method));

            assertEquals(ErrorCode.PARAM_INVALID.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("必须为正数"));
        }

        @Test
        @DisplayName("角色不满足时应抛 FORBIDDEN")
        void shouldThrowWhenRoleNotSatisfied() throws Exception {
            HandlerMethod method = new HandlerMethod(new WorkspaceAnnotatedController(),
                    WorkspaceAnnotatedController.class.getMethod("adminEndpoint"));
            when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                    .thenReturn(Map.of("workspaceId", String.valueOf(WORKSPACE_ID)));
            when(rbacService.isAdmin(USER_ID, WORKSPACE_ID)).thenReturn(false);

            BizException exception = assertThrows(BizException.class,
                    () -> interceptor.preHandle(request, response, method));

            assertEquals(ErrorCode.FORBIDDEN.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("管理员"));
        }
    }

    @SuppressWarnings("unused")
    static class WorkspaceAnnotatedController {

        public void openEndpoint() {
        }

        @RequireWorkspaceRole(role = WorkspaceRole.OWNER)
        public void ownerEndpoint() {
        }

        @RequireWorkspaceRole(role = WorkspaceRole.ADMIN)
        public void adminEndpoint() {
        }
    }
}
