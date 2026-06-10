package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.vo.SolutionDetailVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.SolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solution 控制器
 */
@Slf4j
@RestController
@RequestMapping("/solutions")
@RequiredArgsConstructor
@Tag(name = "Solution", description = "Solution 详情、版本与反馈统计")
public class SolutionController {

    private final SolutionService solutionService;

    @GetMapping("/{solutionId}")
    @Operation(summary = "获取 Solution 详情")
    public ApiResponse<SolutionDetailVO> getDetail(@PathVariable Long solutionId) {
        long userId = StpUtil.getLoginIdAsLong();
        SolutionDetailVO result = solutionService.getDetail(userId, solutionId);
        log.info("读取 Solution 详情: solutionId={}, userId={}", solutionId, userId);
        return ApiResponse.ok(result);
    }
}
