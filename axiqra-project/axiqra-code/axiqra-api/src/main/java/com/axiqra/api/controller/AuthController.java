package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.domain.dto.LoginRequest;
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
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UserEntity user = userService.getByUsername(request.getUsername());
        String targetHash = user != null ? user.getPasswordHash() : DUMMY_BCRYPT_HASH;
        boolean passwordMatches = userService.checkPassword(request.getPassword(), targetHash);
        if (user == null || !passwordMatches) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        log.info("用户登录成功: userId={}", user.getId());
        return ApiResponse.ok(LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .token(token)
                .build());
    }

    @PostMapping("/register")
    @Operation(summary = "注册", description = "注册新用户")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserEntity user = userService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getNickname()
            );
            StpUtil.login(user.getId());
            String token = StpUtil.getTokenValue();
            return ApiResponse.ok(LoginResponse.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .token(token)
                    .build());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户注册失败: username={}", request.getUsername(), e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "注册失败，请稍后重试");
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "注销当前会话")
    public ApiResponse<Void> logout() {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            StpUtil.logout();
            log.info("用户登出: userId={}", userId);
        } catch (Exception e) {
            log.warn("登出时发生异常", e);
        }
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    @Operation(summary = "当前用户信息", description = "获取登录用户信息")
    public ApiResponse<LoginResponse> me() {
        long userId = StpUtil.getLoginIdAsLong();
        UserEntity user = userService.getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return ApiResponse.ok(LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .token(StpUtil.getTokenValue())
                .build());
    }
}
