package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.dto.TraceConfirmRequest;
import com.axiqra.common.domain.dto.TraceCreateRequest;
import com.axiqra.common.domain.vo.TraceDetailVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trace 控制器
 */
@Slf4j
@RestController
@RequestMapping("/traces")
@RequiredArgsConstructor
@Tag(name = "Trace", description = "工程轨迹提交、确认与查询")
public class TraceController {

    private final TraceService traceService;

    @PostMapping
    @Operation(summary = "创建 Trace 草稿")
    public ApiResponse<TraceDetailVO> createDraft(@Valid @RequestBody TraceCreateRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        TraceDetailVO result = traceService.createDraft(userId, request);
        // 隐私合规：控制器日志不直接记录用户标识，改为稳定脱敏摘要。
        log.info("创建 Trace 草稿: traceId={}, actorHash={}", result.getId(), pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @PostMapping("/{traceId}/confirm")
    @Operation(summary = "确认 Trace 执行结果")
    public ApiResponse<TraceDetailVO> confirm(@PathVariable Long traceId,
                                              @Valid @RequestBody TraceConfirmRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        TraceDetailVO result = traceService.confirm(userId, traceId, request);
        log.info("确认 Trace: traceId={}, actorHash={}", traceId, pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @PostMapping("/{traceId}/submit")
    @Operation(summary = "提交 Trace 进入审核")
    public ApiResponse<TraceDetailVO> submit(@PathVariable Long traceId) {
        long userId = StpUtil.getLoginIdAsLong();
        TraceDetailVO result = traceService.submit(userId, traceId);
        log.info("提交 Trace: traceId={}, actorHash={}", traceId, pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @GetMapping("/{traceId}")
    @Operation(summary = "获取 Trace 详情")
    public ApiResponse<TraceDetailVO> getDetail(@PathVariable Long traceId) {
        long userId = StpUtil.getLoginIdAsLong();
        TraceDetailVO result = traceService.getDetail(userId, traceId);
        log.info("读取 Trace 详情: traceId={}, actorHash={}", traceId, pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    private String pseudonymizeUserId(Long userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.valueOf(userId).getBytes(StandardCharsets.UTF_8));
            return "%02x%02x%02x%02x".formatted(hash[0], hash[1], hash[2], hash[3]);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
