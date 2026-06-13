package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.annotation.RequireScope;
import com.axiqra.common.domain.vo.ReviewDetailVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Review Controller
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Tag(name = "Review", description = "审核接口")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "获取待审核队列")
    @GetMapping("/pending")
    @RequireScope("review:read")
    public ResponseEntity<ApiResponse<List<ReviewDetailVO>>> getPendingReviews(
            @RequestParam(defaultValue = "human") @Pattern(regexp = "^(HUMAN|AUTO|QUARANTINED)$", message = "queue 只能是 HUMAN|AUTO|QUARANTINED") @Size(max = 32) String queue,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        long userId = StpUtil.getLoginIdAsLong();
        List<ReviewDetailVO> result = reviewService.getPendingReviews(userId, queue, limit);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "获取审核详情")
    @GetMapping("/{reviewId}")
    @RequireScope("review:read")
    public ResponseEntity<ApiResponse<ReviewDetailVO>> getReviewDetail(
            @PathVariable @Positive Long reviewId) {
        long userId = StpUtil.getLoginIdAsLong();
        ReviewDetailVO result = reviewService.getReviewDetail(userId, reviewId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "审核通过")
    @PostMapping("/{reviewId}/approve")
    @RequireScope("review:write")
    public ResponseEntity<ApiResponse<ReviewDetailVO>> approve(
            @PathVariable @Positive Long reviewId,
            @RequestParam(required = false) @Size(max = 64, message = "reasonCode 长度不能超过 64") String reasonCode,
            @RequestParam(required = false) @Size(max = 1000, message = "notes 长度不能超过 1000") String notes) {
        long userId = StpUtil.getLoginIdAsLong();
        ReviewDetailVO result = reviewService.approve(userId, reviewId, reasonCode, notes);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "审核拒绝")
    @PostMapping("/{reviewId}/reject")
    @RequireScope("review:write")
    public ResponseEntity<ApiResponse<ReviewDetailVO>> reject(
            @PathVariable @Positive Long reviewId,
            @RequestParam(required = false) @Size(max = 64, message = "reasonCode 长度不能超过 64") String reasonCode,
            @RequestParam(required = false) @Size(max = 1000, message = "notes 长度不能超过 1000") String notes) {
        long userId = StpUtil.getLoginIdAsLong();
        ReviewDetailVO result = reviewService.reject(userId, reviewId, reasonCode, notes);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "隔离内容")
    @PostMapping("/{reviewId}/quarantine")
    @RequireScope("review:write")
    public ResponseEntity<ApiResponse<ReviewDetailVO>> quarantine(
            @PathVariable @Positive Long reviewId,
            @RequestParam(required = false) @Size(max = 64, message = "reasonCode 长度不能超过 64") String reasonCode,
            @RequestParam(required = false) @Size(max = 1000, message = "notes 长度不能超过 1000") String notes) {
        long userId = StpUtil.getLoginIdAsLong();
        ReviewDetailVO result = reviewService.quarantine(userId, reviewId, reasonCode, notes);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "申诉")
    @PostMapping("/{reviewId}/appeal")
    @RequireScope("review:write")
    public ResponseEntity<ApiResponse<ReviewDetailVO>> appeal(
            @PathVariable @Positive Long reviewId,
            @RequestParam @NotBlank(message = "申诉内容不能为空") @Size(max = 2000, message = "appealContent 长度不能超过 2000") String appealContent) {
        long userId = StpUtil.getLoginIdAsLong();
        ReviewDetailVO result = reviewService.appeal(userId, reviewId, appealContent);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
