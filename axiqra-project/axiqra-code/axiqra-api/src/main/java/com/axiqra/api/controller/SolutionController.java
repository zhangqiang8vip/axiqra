package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.dto.SolutionCreateFromProjectCaseRequest;
import com.axiqra.common.domain.vo.SearchResultItemVO;
import com.axiqra.common.domain.vo.SolutionDetailVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.SolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PostMapping("/from-project-case")
    @Operation(summary = "从 Project Case 生成工程方案")
    public ApiResponse<SolutionDetailVO> createFromProjectCase(@Valid @RequestBody SolutionCreateFromProjectCaseRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        SolutionDetailVO result = solutionService.createFromProjectCase(userId, request);
        log.info("从 Project Case 生成 Solution: solutionId={}, projectCaseId={}, userId={}",
                result.getId(), request.getProjectCaseId(), userId);
        return ApiResponse.ok(result);
    }

    @GetMapping("/{solutionId}")
    @Operation(summary = "获取 Solution 详情")
    public ApiResponse<SolutionDetailVO> getDetail(@PathVariable Long solutionId) {
        long userId = StpUtil.getLoginIdAsLong();
        SolutionDetailVO result = solutionService.getDetail(userId, solutionId);
        log.info("读取 Solution 详情: solutionId={}, userId={}", solutionId, userId);
        return ApiResponse.ok(result);
    }

    @GetMapping("/public")
    @Operation(summary = "列出公开可展示的 Solution")
    public ApiResponse<List<SearchResultItemVO>> listPublicSolutions(@RequestParam(required = false) String query,
                                                                     @RequestParam(required = false) String domain,
                                                                     @RequestParam(required = false) String techStack,
                                                                     @RequestParam(required = false) Integer minVerificationLevel,
                                                                     @RequestParam(required = false) Integer limit) {
        List<SearchResultItemVO> result = solutionService.listPublicSolutions(query, domain, techStack, minVerificationLevel, limit);
        log.info("匿名列出 Public Solution: query={}, domain={}, techStack={}, count={}",
                query, domain, techStack, result.size());
        return ApiResponse.ok(result);
    }

    @GetMapping("/public/{solutionId}")
    @Operation(summary = "获取公开 Solution 详情")
    public ApiResponse<SolutionDetailVO> getPublicDetail(@PathVariable Long solutionId) {
        SolutionDetailVO result = solutionService.getPublicDetail(solutionId);
        log.info("匿名读取 Public Solution 详情: solutionId={}", solutionId);
        return ApiResponse.ok(result);
    }
}
