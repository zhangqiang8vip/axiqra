package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.dto.SolutionCreateFromProjectCaseRequest;
import com.axiqra.common.domain.vo.PageResponse;
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
 * 工程方案 Controller
 */
@Slf4j
@RestController
@RequestMapping("/solutions")
@RequiredArgsConstructor
@Tag(name = "工程方案", description = "Solution 详情、版本与反馈统计")
public class SolutionController {

    private final SolutionService solutionService;

    @PostMapping("/from-project-case")
    @Operation(summary = "从项目案例生成方案", description = "基于 Project Case 自动生成 Solution 工程方案")
    public ApiResponse<SolutionDetailVO> createFromProjectCase(@Valid @RequestBody SolutionCreateFromProjectCaseRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        SolutionDetailVO result = solutionService.createFromProjectCase(userId, request);
        log.info("从 Project Case 生成 Solution: solutionId={}, projectCaseId={}, userId={}",
                result.getId(), request.getProjectCaseId(), userId);
        return ApiResponse.ok(result);
    }

    @GetMapping("/{solutionId}")
    @Operation(summary = "获取方案详情", description = "查询指定 Solution 的完整详情，包括版本和反馈统计")
    public ApiResponse<SolutionDetailVO> getDetail(@PathVariable Long solutionId) {
        long userId = StpUtil.getLoginIdAsLong();
        SolutionDetailVO result = solutionService.getDetail(userId, solutionId);
        log.info("读取 Solution 详情: solutionId={}, userId={}", solutionId, userId);
        return ApiResponse.ok(result);
    }

    @GetMapping("/public")
    @Operation(summary = "公开方案列表", description = "列出所有公开可展示的 Solution，支持按领域、技术栈筛选")
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
    @Operation(summary = "获取公开方案详情", description = "无需登录，获取公开 Solution 的详情信息")
    public ApiResponse<SolutionDetailVO> getPublicDetail(@PathVariable Long solutionId) {
        SolutionDetailVO result = solutionService.getPublicDetail(solutionId);
        log.info("匿名读取 Public Solution 详情: solutionId={}", solutionId);
        return ApiResponse.ok(result);
    }

    @GetMapping
    @Operation(summary = "我的方案列表", description = "分页查询当前用户创建的 Solution")
    public ApiResponse<PageResponse<SearchResultItemVO>> listMySolutions(
            @RequestParam(required = false) Long workspaceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        long userId = StpUtil.getLoginIdAsLong();
        PageResponse<SearchResultItemVO> result = solutionService.listMySolutions(userId, workspaceId, page, pageSize);
        log.info("分页查询 Solutions: userId={}, workspaceId={}, page={}, pageSize={}", userId, workspaceId, page, pageSize);
        return ApiResponse.ok(result);
    }

    @PostMapping("/{solutionId}/submit-for-review")
    @Operation(summary = "提交审核", description = "将 Solution 从草稿/候选状态提交进入人工审核流程")
    public ApiResponse<Void> submitForReview(@PathVariable Long solutionId) {
        long userId = StpUtil.getLoginIdAsLong();
        solutionService.transitionToNeedsReview(userId, solutionId);
        log.info("提交 Solution 审核: solutionId={}, userId={}", solutionId, userId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{solutionId}/archive")
    @Operation(summary = "归档方案", description = "将 Solution 标记为废弃状态")
    public ApiResponse<Void> archive(@PathVariable Long solutionId) {
        long userId = StpUtil.getLoginIdAsLong();
        solutionService.transitionToArchived(solutionId, userId);
        log.info("归档 Solution: solutionId={}, userId={}", solutionId, userId);
        return ApiResponse.ok(null);
    }
}
