package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.vo.PublicCaseDetailVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.PublicCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Public Case 控制器
 */
@Slf4j
@RestController
@RequestMapping("/public-cases")
@RequiredArgsConstructor
@Tag(name = "Public Case", description = "公开 Case 发布与查询")
public class PublicCaseController {

    private final PublicCaseService publicCaseService;

    @PostMapping("/publish/{projectCaseId}")
    @Operation(summary = "将 Project Case 发布为 Public Case")
    public ApiResponse<PublicCaseDetailVO> publish(@PathVariable Long projectCaseId) {
        long userId = StpUtil.getLoginIdAsLong();
        PublicCaseDetailVO result = publicCaseService.publish(userId, projectCaseId);
        // 隐私合规：控制器日志不直接记录用户标识，改为稳定脱敏摘要。
        log.info("发布 Public Case: sourceCaseId={}, publicCaseId={}, actorHash={}",
                projectCaseId, result.getId(), pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @GetMapping("/{publicCaseId}")
    @Operation(summary = "获取 Public Case 详情")
    public ApiResponse<PublicCaseDetailVO> getDetail(@PathVariable Long publicCaseId) {
        long userId = StpUtil.getLoginIdAsLong();
        PublicCaseDetailVO result = publicCaseService.getDetail(userId, publicCaseId);
        log.info("读取 Public Case 详情: publicCaseId={}, actorHash={}", publicCaseId, pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @GetMapping
    @Operation(summary = "列出公开可读的 Public Case")
    public ApiResponse<List<PublicCaseDetailVO>> list(@RequestParam(required = false) Integer limit) {
        long userId = StpUtil.getLoginIdAsLong();
        List<PublicCaseDetailVO> result = publicCaseService.listPublicCases(userId, limit);
        log.info("列出 Public Case: limit={}, actorHash={}, count={}", limit, pseudonymizeUserId(userId), result.size());
        return ApiResponse.ok(result);
    }

    private String pseudonymizeUserId(Long userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.valueOf(userId).getBytes(StandardCharsets.UTF_8));
            return "%02x%02x%02x%02x".formatted(hash[0], hash[1], hash[2], hash[3]);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
