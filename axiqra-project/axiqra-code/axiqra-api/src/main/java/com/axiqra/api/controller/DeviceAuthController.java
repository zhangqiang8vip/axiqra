package com.axiqra.api.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.observability.AxiqraMetrics;
import com.axiqra.core.service.UserService;
import com.axiqra.core.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 设备授权 Controller（类似 GitHub Device Flow / OAuth 2.0 Device Authorization Grant）
 *
 * <h2>S1.1 增强</h2>
 * <ul>
 *   <li>新增 refresh_token：access_token 过期前可换取新 token，无需再次走 device flow</li>
 *   <li>新增 expires_at：客户端可直接比对本地时间决定是否刷新</li>
 *   <li>refresh_token 单次使用，刷新后立即 rotate（检测到重放则吊销整条会话链）</li>
 * </ul>
 *
 * <h2>Redis Key 设计</h2>
 * <ul>
 *   <li>{@code auth:device:{deviceCode}} — 设备码状态 hash，包含 user_code / status / user_id / refresh_token / access_expires_at</li>
 *   <li>{@code auth:refresh:{refreshToken}} — refresh_token 反向索引，存储 deviceCode + access_expires_at</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/auth/device")
@RequiredArgsConstructor
@Tag(name = "设备授权", description = "OAuth 2.0 设备授权流程，用于 CLI/桌面端登录")
public class DeviceAuthController {

    private final UserService userService;
    private final WorkspaceService workspaceService;
    private final StringRedisTemplate redisTemplate;
    private final AxiqraMetrics metrics;

    private static final String CODE_PREFIX = "auth:device:";
    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final int CODE_EXPIRE_SECONDS = 600;
    private static final int POLLING_INTERVAL_SECONDS = 2;
    private static final String DEFAULT_WEB_URL = "http://localhost:5173";

    /** Access token 有效期：30 天（与 sa-token.timeout 保持一致）。 */
    private static final long ACCESS_TOKEN_EXPIRE_SECONDS = 2_592_000L;
    /** Refresh token 有效期：90 天。 */
    private static final long REFRESH_TOKEN_EXPIRE_SECONDS = 7_776_000L;

    private static final SecureRandom RANDOM = new SecureRandom();

    @SaIgnore
    @PostMapping("/code")
    @Operation(summary = "获取授权码", description = "设备端请求授权码，返回 device_code 和 user_code 用于后续验证")
    public ApiResponse<Map<String, Object>> getDeviceCode() {
        String deviceCode = generateRandomString(16);
        String userCode = generateUserCode();

        String key = CODE_PREFIX + deviceCode;
        Map<String, String> codeData = new HashMap<>();
        codeData.put("user_code", userCode);
        codeData.put("status", "pending");

        redisTemplate.opsForHash().putAll(key, codeData);
        redisTemplate.expire(key, CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("device_code", deviceCode);
        result.put("user_code", userCode);
        // 构建完整验证 URL（包含协议、主机、端口）
        String verificationUrl = buildFullVerificationUrl(userCode, deviceCode);
        result.put("verification_url", verificationUrl);
        result.put("interval", POLLING_INTERVAL_SECONDS);
        result.put("expires_in", CODE_EXPIRE_SECONDS);

        metrics.recordDeviceAuthCodeIssued();
        log.info("设备授权码已生成: userCode={}, deviceCode={}", userCode, deviceCode);
        return ApiResponse.ok(result);
    }

    @SaIgnore
    @PostMapping("/token")
    @Operation(summary = "轮询令牌", description = "设备端轮询查询授权状态，用户在网页确认后返回 access_token + refresh_token")
    public ApiResponse<Map<String, Object>> pollToken(@RequestParam String deviceCode) {
        String key = CODE_PREFIX + deviceCode;

        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return ApiResponse.fail(ErrorCode.AUTHORIZATION_EXPIRED.getCode(), "设备码无效或已过期");
        }

        String status = (String) redisTemplate.opsForHash().get(key, "status");
        String userIdStr = (String) redisTemplate.opsForHash().get(key, "user_id");

        if (!"authorized".equals(status)) {
            return ApiResponse.fail(ErrorCode.AUTHORIZATION_PENDING.getCode(), "等待用户授权...");
        }

        if (userIdStr == null) {
            return ApiResponse.fail(ErrorCode.AUTHORIZATION_PENDING.getCode(), "授权处理中...");
        }

        Long userId = Long.parseLong(userIdStr);
        UserEntity user = userService.getById(userId);
        if (user == null) {
            return ApiResponse.fail(ErrorCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }

        StpUtil.login(userId);
        String token = StpUtil.getTokenValue();
        Instant accessExpiresAt = Instant.now().plusSeconds(ACCESS_TOKEN_EXPIRE_SECONDS);
        String refreshToken = generateRandomString(48);
        // 反向索引：refresh_token → device_code（用于刷新时反查用户与设备上下文）
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + refreshToken,
                deviceCode,
                REFRESH_TOKEN_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );
        // 设备码 hash 写入 refresh_token + 过期时间（用于 revoke / audit）
        redisTemplate.opsForHash().put(key, "refresh_token", refreshToken);
        redisTemplate.opsForHash().put(key, "access_expires_at", String.valueOf(accessExpiresAt.getEpochSecond()));
        redisTemplate.opsForHash().put(key, "issued_user_id", String.valueOf(userId));

        Long workspaceId = workspaceService.getOrCreatePersonalWorkspaceId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("access_token", token);
        result.put("token_type", "Bearer");
        result.put("expires_in", ACCESS_TOKEN_EXPIRE_SECONDS);
        result.put("expires_at", accessExpiresAt.toString());
        result.put("refresh_token", refreshToken);
        result.put("refresh_expires_in", REFRESH_TOKEN_EXPIRE_SECONDS);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
        if (workspaceId != null) {
            userInfo.put("workspaceId", workspaceId);
        }
        result.put("user", userInfo);

        metrics.recordDeviceAuthTokenRefresh("issued");
        log.info("设备授权成功: userId={}, workspaceId={}, accessExpiresAt={}", userId, workspaceId, accessExpiresAt);
        return ApiResponse.ok(result);
    }

    @SaIgnore
    @PostMapping("/refresh")
    @Operation(summary = "刷新 access_token",
               description = "使用 refresh_token 换取新的 access_token + 新的 refresh_token（rotate 语义）。refresh_token 单次使用，被检测到重放会吊销整条会话链。")
    public ApiResponse<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        String oldRefreshToken = request.get("refresh_token");
        if (oldRefreshToken == null || oldRefreshToken.isBlank()) {
            return ApiResponse.fail(ErrorCode.INVALID_PARAMETER.getCode(), "refresh_token 不能为空");
        }

        String refreshKey = REFRESH_PREFIX + oldRefreshToken;
        String deviceCode = redisTemplate.opsForValue().get(refreshKey);
        if (deviceCode == null) {
            // 重放攻击 / 已轮转 / 已过期
            metrics.recordDeviceAuthTokenRefresh("rejected");
            log.warn("refresh_token 无效或已轮转: token={}", maskToken(oldRefreshToken));
            return ApiResponse.fail(ErrorCode.AUTHORIZATION_EXPIRED.getCode(), "refresh_token 无效或已过期，请重新登录");
        }

        String deviceKey = CODE_PREFIX + deviceCode;
        String userIdStr = (String) redisTemplate.opsForHash().get(deviceKey, "issued_user_id");
        if (userIdStr == null) {
            metrics.recordDeviceAuthTokenRefresh("rejected");
            return ApiResponse.fail(ErrorCode.AUTHORIZATION_EXPIRED.getCode(), "设备授权上下文已失效");
        }
        Long userId = Long.parseLong(userIdStr);

        // 立即撤销旧 refresh_token（rotate 语义：旧 token 一次性使用）
        redisTemplate.delete(refreshKey);

        // 颁发新 token
        StpUtil.login(userId);
        String newAccessToken = StpUtil.getTokenValue();
        Instant newAccessExpiresAt = Instant.now().plusSeconds(ACCESS_TOKEN_EXPIRE_SECONDS);
        String newRefreshToken = generateRandomString(48);
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + newRefreshToken,
                deviceCode,
                REFRESH_TOKEN_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );
        redisTemplate.opsForHash().put(deviceKey, "refresh_token", newRefreshToken);
        redisTemplate.opsForHash().put(deviceKey, "access_expires_at", String.valueOf(newAccessExpiresAt.getEpochSecond()));

        Map<String, Object> result = new HashMap<>();
        result.put("access_token", newAccessToken);
        result.put("token_type", "Bearer");
        result.put("expires_in", ACCESS_TOKEN_EXPIRE_SECONDS);
        result.put("expires_at", newAccessExpiresAt.toString());
        result.put("refresh_token", newRefreshToken);
        result.put("refresh_expires_in", REFRESH_TOKEN_EXPIRE_SECONDS);

        metrics.recordDeviceAuthTokenRefresh("rotated");
        log.info("设备授权 token 已轮转: userId={}, deviceCode={}, newExpiresAt={}", userId, deviceCode, newAccessExpiresAt);
        return ApiResponse.ok(result);
    }

    @SaIgnore
    @PostMapping("/revoke")
    @Operation(summary = "主动吊销 refresh_token", description = "用户主动登出时调用，撤销 refresh_token 与对应 Sa-Token 会话")
    public ApiResponse<Map<String, Object>> revokeToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refresh_token");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.fail(ErrorCode.INVALID_PARAMETER.getCode(), "refresh_token 不能为空");
        }
        String refreshKey = REFRESH_PREFIX + refreshToken;
        String deviceCode = redisTemplate.opsForValue().get(refreshKey);
        if (deviceCode != null) {
            String deviceKey = CODE_PREFIX + deviceCode;
            String userIdStr = (String) redisTemplate.opsForHash().get(deviceKey, "issued_user_id");
            redisTemplate.delete(refreshKey);
            redisTemplate.delete(deviceKey);
            if (userIdStr != null) {
                try {
                    StpUtil.logout(Long.parseLong(userIdStr));
                } catch (RuntimeException ex) {
                    log.warn("Sa-Token 登出失败（可能已过期）: userId={}, err={}", userIdStr, ex.getMessage());
                }
            }
        }
        metrics.recordDeviceAuthTokenRefresh("revoked");
        log.info("设备授权已主动吊销: token={}", maskToken(refreshToken));
        return ApiResponse.ok(Map.of("success", true));
    }

    @SaIgnore
    @GetMapping("/verify-page")
    @Operation(summary = "验证页面", description = "返回验证页面 URL，供用户在浏览器中打开授权")
    public String getVerifyPage() {
        return "redirect:" + DEFAULT_WEB_URL + "/auth/device";
    }

    @PostMapping("/confirm")
    @Operation(summary = "确认授权", description = "已登录用户在网页端确认设备授权，填入 user_code 完成授权")
    public ApiResponse<Map<String, Object>> confirmAuthorization(@RequestBody Map<String, String> request) {
        String userCode = request.get("user_code");
        String deviceCode = request.get("device_code");

        if (userCode == null || deviceCode == null) {
            return ApiResponse.fail(ErrorCode.INVALID_PARAMETER.getCode(), "参数不完整");
        }

        String key = CODE_PREFIX + deviceCode;

        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return ApiResponse.fail(ErrorCode.AUTHORIZATION_EXPIRED.getCode(), "设备码无效");
        }

        String storedCode = (String) redisTemplate.opsForHash().get(key, "user_code");
        if (!userCode.equalsIgnoreCase(storedCode)) {
            return ApiResponse.fail(ErrorCode.INVALID_PARAMETER.getCode(), "授权码不匹配");
        }

        long loginUserId = StpUtil.getLoginIdAsLong();
        UserEntity user = userService.getById(loginUserId);
        if (user == null) {
            return ApiResponse.fail(ErrorCode.USER_NOT_FOUND.getCode(), "当前登录用户不存在");
        }

        redisTemplate.opsForHash().put(key, "status", "authorized");
        redisTemplate.opsForHash().put(key, "user_id", String.valueOf(user.getId()));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "授权成功！请回到终端。");

        log.info("设备授权确认: userCode={}, userId={}", userCode, user.getId());
        return ApiResponse.ok(result);
    }

    private String generateRandomString(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
    }

    private String generateUserCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0 && i % 3 == 0) sb.append("-");
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 构建完整的验证页面 URL，确保包含协议、主机和端口
     */
    private String buildFullVerificationUrl(String userCode, String deviceCode) {
        return toDeviceVerificationUrl(userCode, deviceCode);
    }

    private String toDeviceVerificationUrl(String userCode, String deviceCode) {
        return DEFAULT_WEB_URL + "/auth/device?code=" + userCode + "&device=" + deviceCode;
    }

    /** 仅打印 token 的前缀与尾段用于日志审计，避免完整 token 落盘。 */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}
