package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.advice.TraceIdResponseAdvice;
import com.axiqra.api.filter.TraceIdFilter;
import com.axiqra.api.handler.GlobalExceptionHandler;
import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController MockMvc 闭环测试")
class UserControllerMockMvcTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new TraceIdFilter())
                .setControllerAdvice(new GlobalExceptionHandler(), new TraceIdResponseAdvice())
                .build();

        stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    @DisplayName("GET /users/me 应返回统一成功响应并注入 requestId")
    void shouldReturnCurrentUserWithUnifiedResponseAndRequestId() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setUsername("alice");
        user.setNickname("Alice");
        user.setEmail("alice@example.com");
        user.setAvatar("https://cdn.example.com/a.png");
        when(userService.getById(USER_ID)).thenReturn(user);

        mockMvc.perform(get("/users/me")
                        .header("X-Trace-Id", "trace-user-me-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.requestId").value("trace-user-me-001"))
                .andExpect(jsonPath("$.traceId").value("trace-user-me-001"))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("GET /users/{userId} 用户不存在时应走统一异常响应")
    void shouldReturnUnifiedNotFoundResponseWhenUserMissing() throws Exception {
        when(userService.getById(99L)).thenReturn(null);

        mockMvc.perform(get("/users/{userId}", 99L)
                        .header("X-Trace-Id", "trace-user-404-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.requestId").value("trace-user-404-001"));
    }
}
