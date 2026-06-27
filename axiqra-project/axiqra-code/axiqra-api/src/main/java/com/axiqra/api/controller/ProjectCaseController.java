package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.dto.ProjectCaseCreateRequest;
import com.axiqra.common.domain.vo.PageResponse;
import com.axiqra.common.domain.vo.ProjectCaseDetailVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.common.util.PrivacyUtils;
import com.axiqra.core.service.ProjectCaseService;
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

/**
 * Project Case 控制器
 */
@Slf4j
@RestController
@RequestMapping("/project-cases")
@RequiredArgsConstructor
@Tag(name = "Project Case", description = "私有 Case 创建、详情与发布申请")
public class ProjectCaseController {

    private final ProjectCaseService projectCaseService;

    @PostMapping
    @Operation(summary = "从 Trace 创建 Project Case")
    public ApiResponse<ProjectCaseDetailVO> create(@Valid @RequestBody ProjectCaseCreateRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        ProjectCaseDetailVO result = projectCaseService.create(userId, request);
        // 隐私合规：控制器日志不直接记录用户标识，改为稳定脱敏摘要。
        log.info("创建 Project Case: caseId={}, actorHash={}", result.getId(), PrivacyUtils.pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @GetMapping("/{caseId}")
    @Operation(summary = "获取 Project Case 详情")
    public ApiResponse<ProjectCaseDetailVO> getDetail(@PathVariable Long caseId) {
        long userId = StpUtil.getLoginIdAsLong();
        ProjectCaseDetailVO result = projectCaseService.getDetail(userId, caseId);
        log.info("读取 Project Case 详情: caseId={}, actorHash={}", caseId, PrivacyUtils.pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @GetMapping
    @Operation(summary = "分页查询 Project Cases")
    public ApiResponse<PageResponse<ProjectCaseDetailVO>> listByWorkspace(
            @RequestParam(required = false) Long workspaceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        long userId = StpUtil.getLoginIdAsLong();
        PageResponse<ProjectCaseDetailVO> result = projectCaseService.listByWorkspace(userId, workspaceId, page, pageSize);
        log.info("分页查询 Project Cases: userId={}, workspaceId={}, page={}, pageSize={}", userId, workspaceId, page, pageSize);
        return ApiResponse.ok(result);
    }

    @PostMapping("/{caseId}/publish-request")
    @Operation(summary = "发起 Project Case 发布申请")
    public ApiResponse<ProjectCaseDetailVO> requestPublish(@PathVariable Long caseId,
                                                           @RequestParam Long authorizationId) {
        long userId = StpUtil.getLoginIdAsLong();
        ProjectCaseDetailVO result = projectCaseService.requestPublish(userId, caseId, authorizationId);
        log.info("发起 Project Case 发布申请: caseId={}, authorizationId={}, actorHash={}",
                caseId, authorizationId, PrivacyUtils.pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

}
