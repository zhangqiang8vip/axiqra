package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.annotation.RequireScope;
import com.axiqra.common.domain.dto.FeedbackSubmitRequest;
import com.axiqra.common.domain.vo.FeedbackDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feedback Controller
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Tag(name = "Feedback", description = "反馈接口")
@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "提交反馈")
    @PostMapping
    @RequireScope("feedback:write")
    public ResponseEntity<ApiResponse<FeedbackDetailVO>> submitFeedback(
            @Valid @RequestBody FeedbackSubmitRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        FeedbackDetailVO result = feedbackService.submitFeedback(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "获取反馈列表")
    @GetMapping
    @RequireScope("feedback:read")
    public ResponseEntity<ApiResponse<List<FeedbackDetailVO>>> listFeedbacks(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        long userId = StpUtil.getLoginIdAsLong();
        List<FeedbackDetailVO> result = feedbackService.listFeedbacks(userId, targetType, targetId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "获取 Solution 反馈统计")
    @GetMapping("/solutions/{solutionId}/stats")
    @RequireScope("feedback:read")
    public ResponseEntity<ApiResponse<SolutionFeedbackStatsVO>> getSolutionFeedbackStats(@PathVariable Long solutionId) {
        SolutionFeedbackStatsVO result = feedbackService.getSolutionFeedbackStats(solutionId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
