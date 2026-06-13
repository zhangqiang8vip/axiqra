package com.axiqra.api.controller;

import com.axiqra.api.annotation.RequireScope;
import com.axiqra.common.domain.vo.ContributionSummaryVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.ContributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contribution Controller
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Tag(name = "Contribution", description = "贡献接口")
@RestController
@RequestMapping("/api/v1/contributions")
@RequiredArgsConstructor
@Validated
public class ContributionController {

    private final ContributionService contributionService;

    @Operation(summary = "获取贡献汇总")
    @GetMapping("/users/{userId}/summary")
    @RequireScope("contribution:read")
    public ResponseEntity<ApiResponse<ContributionSummaryVO>> getContributionSummary(@PathVariable @Positive Long userId) {
        ContributionSummaryVO result = contributionService.getContributionSummary(userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "获取贡献记录")
    @GetMapping("/users/{userId}/records")
    @RequireScope("contribution:read")
    public ResponseEntity<ApiResponse<List<ContributionSummaryVO.ContributionRecord>>> getContributionRecords(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(1000) int limit) {
        List<ContributionSummaryVO.ContributionRecord> result = contributionService.getContributionRecords(userId, limit);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
