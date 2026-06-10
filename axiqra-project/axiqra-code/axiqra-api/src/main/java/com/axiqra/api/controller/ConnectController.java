package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.dto.ConnectSessionCreateRequest;
import com.axiqra.common.domain.vo.ConnectDoctorVO;
import com.axiqra.common.domain.vo.ConnectSessionVO;
import com.axiqra.common.domain.vo.QuotaStatusVO;
import com.axiqra.common.domain.vo.RateLimitStatusVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.ConnectService;
import com.axiqra.core.service.QuotaService;
import com.axiqra.core.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/connect")
@RequiredArgsConstructor
@Tag(name = "Connect", description = "Quota + RateLimit + Connect 会话")
public class ConnectController {

    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final ConnectService connectService;
    private final QuotaService quotaService;
    private final RateLimitService rateLimitService;

    @GetMapping("/quota")
    @Operation(summary = "查询当前配额状态")
    public ApiResponse<QuotaStatusVO> quota() {
        long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(quotaService.getStatus(userId));
    }

    @GetMapping("/rate-limit")
    @Operation(summary = "检查当前限流窗口")
    public ApiResponse<RateLimitStatusVO> rateLimit(HttpServletResponse response) {
        long userId = StpUtil.getLoginIdAsLong();
        try {
            RateLimitStatusVO result = rateLimitService.checkOrThrow(userId, "connect:probe");
            response.setHeader(RETRY_AFTER_HEADER, String.valueOf(result.getRetryAfterSeconds()));
            return ApiResponse.ok(result);
        } catch (BizException ex) {
            if (ex.getCode() == ErrorCode.RATE_LIMITED.getCode()
                    && ex.getRetryAfterSeconds() != null
                    && ex.getRetryAfterSeconds() > 0) {
                response.setHeader(RETRY_AFTER_HEADER, String.valueOf(ex.getRetryAfterSeconds()));
            }
            throw ex;
        }
    }

    @GetMapping("/doctor")
    @Operation(summary = "执行 Connect doctor 八项检查")
    public ApiResponse<ConnectDoctorVO> doctor(@RequestParam String channel,
                                               @RequestParam String toolType,
                                               @RequestParam(required = false) Long workspaceId) {
        long userId = StpUtil.getLoginIdAsLong();
        if (!ALLOWED_CHANNELS.contains(channel)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "channel 值非法: " + channel);
        }
        if (!ALLOWED_TOOL_TYPES.contains(toolType)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "toolType 值非法: " + toolType);
        }
        return ApiResponse.ok(connectService.runDoctor(userId, channel, toolType, workspaceId));
    }

    private static final java.util.Set<String> ALLOWED_CHANNELS =
            java.util.Set.of("mcp", "cli", "api", "webhook", "plugin");

    private static final java.util.Set<String> ALLOWED_TOOL_TYPES =
            java.util.Set.of("mcp", "database", "search", "storage", "compute", "integration", "messaging", "monitoring", "ai");

    @GetMapping("/sessions")
    @Operation(summary = "查询我的 Connect 会话列表")
    public ApiResponse<List<ConnectSessionVO>> listSessions() {
        long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(connectService.listSessions(userId));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "查询 Connect 会话详情")
    public ApiResponse<ConnectSessionVO> getSession(@PathVariable String sessionId) {
        long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(connectService.getSession(userId, sessionId));
    }

    @PostMapping("/sessions")
    @Operation(summary = "创建 Connect 会话")
    public ResponseEntity<ApiResponse<ConnectSessionVO>> create(@Valid @RequestBody ConnectSessionCreateRequest request,
                                                HttpServletResponse response) {
        long userId = StpUtil.getLoginIdAsLong();
        RateLimitStatusVO rateLimitStatus = rateLimitService.checkOrThrow(userId, "connect:create");
        ConnectSessionVO session = connectService.createSession(userId, request);
        if (rateLimitStatus != null) {
            response.setHeader(RETRY_AFTER_HEADER, String.valueOf(rateLimitStatus.getRetryAfterSeconds()));
        }
        log.info("创建 Connect 会话: sessionId={}, userId={}", session.getSessionId(), userId);
        URI location = URI.create("/connect/sessions/" + session.getSessionId());
        return ResponseEntity.created(location).body(ApiResponse.ok(session));
    }
}
