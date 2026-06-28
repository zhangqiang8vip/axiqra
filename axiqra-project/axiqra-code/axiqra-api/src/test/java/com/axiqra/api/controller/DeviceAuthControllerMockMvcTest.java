package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.advice.TraceIdResponseAdvice;
import com.axiqra.api.filter.TraceIdFilter;
import com.axiqra.api.handler.GlobalExceptionHandler;
import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.core.observability.AxiqraMetrics;
import com.axiqra.core.service.UserService;
import com.axiqra.core.service.WorkspaceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceAuthController MockMvc 闭环测试")
class DeviceAuthControllerMockMvcTest {

    @Mock
    private UserService userService;
    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private AxiqraMetrics metrics;

    @SuppressWarnings("unchecked")
    private final HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;

    private static final Long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        DeviceAuthController controller = new DeviceAuthController(
                userService, workspaceService, redisTemplate, metrics);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new TraceIdFilter())
                .setControllerAdvice(new GlobalExceptionHandler(), new TraceIdResponseAdvice())
                .build();

        // lenient() 避免 strict 模式报 UnnecessaryStubbing
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.hasKey(anyString())).thenReturn(false);
        lenient().when(workspaceService.getOrCreatePersonalWorkspaceId(anyLong())).thenReturn(1L);

        stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class);
        stpUtilMock.when(() -> StpUtil.login(anyLong())).thenAnswer(inv -> null);
        stpUtilMock.when(StpUtil::getTokenValue).thenReturn("sa-token-mock");
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    private UserEntity mockUser() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setUsername("alice");
        user.setNickname("Alice");
        return user;
    }

    @Test
    @DisplayName("/auth/device/code 应返回 device_code / user_code / expires_in")
    void shouldIssueDeviceCode() throws Exception {
        mockMvc.perform(post("/auth/device/code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.device_code").exists())
                .andExpect(jsonPath("$.data.user_code").exists())
                .andExpect(jsonPath("$.data.expires_in").value(600))
                .andExpect(jsonPath("$.data.interval").value(2));

        verify(metrics).recordDeviceAuthCodeIssued();
    }

    @Test
    @DisplayName("轮询 /auth/device/token 应返回 access_token + refresh_token + expires_at")
    void shouldIssueTokensWithRefreshToken() throws Exception {
        // 设备码 hash 里 status=authorized, user_id=42
        when(redisTemplate.hasKey("auth:device:device-abc")).thenReturn(true);
        when(hashOps.get("auth:device:device-abc", "status")).thenReturn("authorized");
        when(hashOps.get("auth:device:device-abc", "user_id")).thenReturn(String.valueOf(USER_ID));
        when(userService.getById(USER_ID)).thenReturn(mockUser());

        mockMvc.perform(post("/auth/device/token").param("deviceCode", "device-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.access_token").value("sa-token-mock"))
                .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                .andExpect(jsonPath("$.data.refresh_token").exists())
                .andExpect(jsonPath("$.data.expires_in").value(2_592_000L))
                .andExpect(jsonPath("$.data.refresh_expires_in").value(7_776_000L))
                .andExpect(jsonPath("$.data.expires_at").exists());

        // refresh_token 应当被写入反向索引 auth:refresh:{token}
        verify(valueOps).set(anyString(), eq("device-abc"), eq(7_776_000L), eq(TimeUnit.SECONDS));
        // 设备码 hash 上应当记录 issued_user_id / access_expires_at / refresh_token
        verify(hashOps).put(eq("auth:device:device-abc"), eq("refresh_token"), anyString());
        verify(hashOps).put(eq("auth:device:device-abc"), eq("issued_user_id"), eq(String.valueOf(USER_ID)));
        verify(metrics).recordDeviceAuthTokenRefresh("issued");
    }

    @Test
    @DisplayName("刷新 token：使用有效 refresh_token 应返回新 token 并 rotate")
    void shouldRotateRefreshToken() throws Exception {
        when(valueOps.get("auth:refresh:rt-1")).thenReturn("device-abc");
        when(hashOps.get("auth:device:device-abc", "issued_user_id")).thenReturn(String.valueOf(USER_ID));

        Map<String, String> body = new HashMap<>();
        body.put("refresh_token", "rt-1");

        mockMvc.perform(post("/auth/device/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"rt-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.access_token").value("sa-token-mock"))
                .andExpect(jsonPath("$.data.refresh_token").exists())
                // 旧 refresh_token 应当被立即吊销
                .andExpect(jsonPath("$.data.refresh_token").value(org.hamcrest.Matchers.not("rt-1")));

        verify(redisTemplate).delete("auth:refresh:rt-1");
        verify(metrics).recordDeviceAuthTokenRefresh("rotated");
    }

    @Test
    @DisplayName("刷新 token：使用已吊销/重放的 refresh_token 应被拒绝")
    void shouldRejectReplayedRefreshToken() throws Exception {
        when(valueOps.get("auth:refresh:rt-bad")).thenReturn(null);

        mockMvc.perform(post("/auth/device/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"rt-bad\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20010)); // AUTHORIZATION_EXPIRED

        verify(redisTemplate, never()).delete(anyString());
        verify(metrics).recordDeviceAuthTokenRefresh("rejected");
    }

    @Test
    @DisplayName("刷新 token：缺少 refresh_token 字段应返回 INVALID_PARAMETER")
    void shouldRejectMissingRefreshToken() throws Exception {
        mockMvc.perform(post("/auth/device/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10005)); // INVALID_PARAMETER
    }

    @Test
    @DisplayName("/auth/device/revoke：主动吊销应清理 redis + 登出 sa-token")
    void shouldRevokeTokens() throws Exception {
        when(valueOps.get("auth:refresh:rt-rev")).thenReturn("device-xyz");
        when(hashOps.get("auth:device:device-xyz", "issued_user_id")).thenReturn(String.valueOf(USER_ID));

        mockMvc.perform(post("/auth/device/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"rt-rev\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));

        verify(redisTemplate).delete("auth:refresh:rt-rev");
        verify(redisTemplate).delete("auth:device:device-xyz");
        stpUtilMock.verify(() -> StpUtil.logout(USER_ID));
        verify(metrics).recordDeviceAuthTokenRefresh("revoked");
    }
}
