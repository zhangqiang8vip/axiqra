package com.axiqra.api.controller;

import com.axiqra.api.annotation.RequireScope;
import com.axiqra.common.domain.vo.ToolModelLeaderboardVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.ToolModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ToolModel Controller
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Tag(name = "ToolModel", description = "工具模型接口")
@RestController
@RequestMapping("/api/v1/tool-models")
@RequiredArgsConstructor
@Validated
public class ToolModelController {

    private final ToolModelService toolModelService;

    @Operation(summary = "获取工具模型排行榜")
    @GetMapping("/leaderboard")
    @RequireScope("public:read")
    public ResponseEntity<ApiResponse<List<ToolModelLeaderboardVO>>> getLeaderboard(
            @RequestParam(defaultValue = "global") String scopeType,
            @RequestParam(required = false) Long scopeId,
            @RequestParam(required = false) String toolName,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit) {
        List<ToolModelLeaderboardVO> result = toolModelService.getLeaderboard(scopeType, scopeId, toolName, limit);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
