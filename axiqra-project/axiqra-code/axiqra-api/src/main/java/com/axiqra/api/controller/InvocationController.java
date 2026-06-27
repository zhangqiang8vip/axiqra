package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.annotation.RequireScope;
import com.axiqra.common.domain.dto.InvocationReportRequest;
import com.axiqra.common.domain.vo.InvocationDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.InvocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 调用记录 Controller
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Slf4j
@Tag(name = "调用记录", description = "上报 AI 工具调用结果，查询调用详情和反馈统计")
@RestController
@RequestMapping("/v1/invocations")
@RequiredArgsConstructor
@Validated
public class InvocationController {

    private final InvocationService invocationService;

    @Operation(summary = "上报调用结果", description = "由 AI 工具/插件调用，上报一次执行结果，用于统计和反馈分析")
    @PostMapping
    @RequireScope("connect:write")
    public ResponseEntity<ApiResponse<InvocationDetailVO>> reportInvocation(
            @Valid @RequestBody InvocationReportRequest request) {
        log.info("【Invocation 上报】request={}", request);
        long userId = StpUtil.getLoginIdAsLong();
        try {
            InvocationDetailVO result = invocationService.reportInvocation(userId, request);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            log.error("【Invocation 上报失败】request={}, error={}", request, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "获取调用详情", description = "查询单条调用记录的详细信息，仅可查看自己的记录")
    @GetMapping("/{invocationId}")
    @RequireScope("connect:read")
    public ResponseEntity<ApiResponse<InvocationDetailVO>> getInvocationDetail(
            @PathVariable @Positive Long invocationId) {
        long userId = StpUtil.getLoginIdAsLong();
        if (!invocationService.isUserAuthorized(userId, invocationId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权限查看该调用记录");
        }
        InvocationDetailVO result = invocationService.getInvocationDetail(userId, invocationId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "获取 Solution 反馈统计", description = "查询指定 Solution 的正负反馈数量分布")
    @GetMapping("/solutions/{solutionId}/feedback-stats")
    @RequireScope("feedback:read")
    public ResponseEntity<ApiResponse<SolutionFeedbackStatsVO>> getSolutionFeedbackStats(@PathVariable @Positive Long solutionId) {
        SolutionFeedbackStatsVO result = invocationService.getSolutionFeedbackStats(solutionId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
