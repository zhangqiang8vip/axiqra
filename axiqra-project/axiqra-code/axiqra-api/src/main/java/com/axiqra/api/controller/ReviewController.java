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
 * 审核 Controller
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Tag(name = "审核", description = "Solution 和 Case 的人工审核流程管理")
@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "待审核队列", description = "获取指定队列的待审核列表，支持按类型筛选")
    @GetMapping("/pending")
    @RequireScope("review:read")
    public ResponseEntity<ApiResponse<List<ReviewDetailVO>>> getPendingReviews(
            @RequestParam(defaultValue = "human_review") @Pattern(
                regexp = "^(AUTO_PASS|AUTO_REJECT|LOW_RISK_SAMPLING|HUMAN_REVIEW|CERTIFIED_REVIEW|DOMAIN_REVIEW|APPEAL|HUMAN|AUTO|QUARANTINED)$",
                message = "queue 必须是 AUTO_PASS|AUTO_REJECT|LOW_RISK_SAMPLING|HUMAN_REVIEW|CERTIFIED_REVIEW|DOMAIN_REVIEW|APPEAL"
            ) @Size(max = 32) String queue,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        long userId = StpUtil.getLoginIdAsLong();
        List<ReviewDetailVO> result = reviewService.getPendingReviews(userId, queue, limit);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "审核详情", description = "查看单条审核记录的详细信息")
    @GetMapping("/{reviewId}")
    @RequireScope("review:read")
    public ResponseEntity<ApiResponse<ReviewDetailVO>> getReviewDetail(
            @PathVariable @Positive Long reviewId) {
        long userId = StpUtil.getLoginIdAsLong();
        ReviewDetailVO result = reviewService.getReviewDetail(userId, reviewId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "审核通过", description = "批准当前内容通过审核，可附加原因码和备注")
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

    @Operation(summary = "审核拒绝", description = "拒绝当前内容，可附加原因码和拒绝说明")
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

    @Operation(summary = "隔离内容", description = "将问题内容移入隔离区，等待进一步处理")
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

    @Operation(summary = "申诉", description = "被拒绝的内容可提交申诉，附上申诉说明")
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
