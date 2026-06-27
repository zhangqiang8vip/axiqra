package com.axiqra.api.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.UserService;
import com.axiqra.core.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 设备授权 Controller（类似 GitHub Device Flow）
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

    private static final String CODE_PREFIX = "auth:device:";
    private static final int CODE_EXPIRE_SECONDS = 600;
    private static final int POLLING_INTERVAL_SECONDS = 2;
    private static final String DEFAULT_WEB_URL = "http://localhost:5173";

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

        log.info("设备授权码已生成: userCode={}, deviceCode={}", userCode, deviceCode);
        return ApiResponse.ok(result);
    }

    @SaIgnore
    @PostMapping("/token")
    @Operation(summary = "轮询令牌", description = "设备端轮询查询授权状态，用户在网页确认后返回 access_token")
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
        redisTemplate.delete(key);

        Long workspaceId = workspaceService.getOrCreatePersonalWorkspaceId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("access_token", token);
        result.put("token_type", "Bearer");
        result.put("expires_in", 2592000);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
        if (workspaceId != null) {
            userInfo.put("workspaceId", workspaceId);
        }
        result.put("user", userInfo);

        log.info("设备授权成功: userId={}, workspaceId={}", userId, workspaceId);
        return ApiResponse.ok(result);
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
}
