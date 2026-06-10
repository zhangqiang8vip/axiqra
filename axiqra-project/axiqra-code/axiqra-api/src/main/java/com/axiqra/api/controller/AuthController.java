package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.exception.NotLoginException;
import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.domain.dto.LoginRequest;
import com.axiqra.common.domain.dto.ProfileUpdateRequest;
import com.axiqra.common.domain.dto.RegisterRequest;
import com.axiqra.common.domain.vo.LoginResponse;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.common.util.PasswordHashUtil;
import com.axiqra.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 认证控制器（登录/注册/登出）
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "认证", description = "登录、注册、登出")
public class AuthController {

    /**
     * Timing-safe dummy BCrypt hash generated at class load.
     * Avoids hardcoding a placeholder string literal while preserving constant-time behavior.
     */
    private static final String DUMMY_BCRYPT_HASH = PasswordHashUtil.hash(
            "dummy-timing-balance-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt());

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "用户名密码登录，返回 Sa-Token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        UserEntity user = userService.getByUsername(request.getUsername());
        String targetHash = user != null ? user.getPasswordHash() : DUMMY_BCRYPT_HASH;
        boolean passwordMatches = userService.checkPassword(request.getPassword(), targetHash);
        if (user == null || !passwordMatches) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        log.info("用户登录成功: userId={}", user.getId());
        LoginResponse body = LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .token(token)
                .build();
        return ResponseEntity.created(URI.create("/auth/me")).body(ApiResponse.ok(body));
    }

    @PostMapping("/register")
    @Operation(summary = "注册", description = "注册新用户")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserEntity user = userService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getNickname()
            );
            StpUtil.login(user.getId());
            String token = StpUtil.getTokenValue();
            LoginResponse body = LoginResponse.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .avatar(user.getAvatar())
                    .token(token)
                    .build();
            return ResponseEntity.created(URI.create("/auth/me")).body(ApiResponse.ok(body));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户注册失败: username={}", request.getUsername(), e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "注册失败，请稍后重试");
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "注销当前会话（幂等：未登录时返回成功）")
    public ResponseEntity<Void> logout() {
        long userId = 0;
        try {
            userId = StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.debug("登出时未检测到登录会话，视为已登出");
        }
        try {
            StpUtil.logout();
        } catch (Exception e) {
            log.warn("登出操作异常（可能已超时失效），userId={}", userId, e);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "当前用户信息", description = "获取登录用户信息")
    public ApiResponse<LoginResponse> me() {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            UserEntity user = userService.getById(userId);
            if (user == null) {
                throw new BizException(ErrorCode.USER_NOT_FOUND);
            }
            return ApiResponse.ok(LoginResponse.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .email(user.getEmail())
                    .avatar(user.getAvatar())
                    .token(StpUtil.getTokenValue())
                    .build());
        } catch (BizException e) {
            throw e;
        } catch (NotLoginException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录或会话已失效");
        } catch (Exception e) {
            log.error("获取当前用户信息异常", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "获取用户信息失败，请稍后重试");
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料", description = "更新当前用户的 nickname、email 和 avatar")
    public ApiResponse<LoginResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        long userId;
        try {
            userId = StpUtil.getLoginIdAsLong();
        } catch (NotLoginException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录或会话已失效");
        }
        UserEntity user = userService.updateProfile(userId, request.getNickname(), request.getEmail(), request.getAvatar());
        log.info("更新个人资料: userId={}", userId);
        return ApiResponse.ok(LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .token(StpUtil.getTokenValue())
                .build());
    }
}
