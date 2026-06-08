package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.annotation.RequireScope;
import com.axiqra.common.domain.dto.PolicyEvaluationRequest;
import com.axiqra.common.domain.vo.PolicyEvaluationVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.PolicyEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * ABAC 策略评估控制器
 *
 * <p>提供策略评估接口，供 AI Tool 或内部服务调用。
 * 高风险操作建议使用 POST /enforce 强制评估。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@RestController
@RequestMapping("/policy")
@RequiredArgsConstructor
@Tag(name = "策略评估", description = "ABAC 策略引擎评估接口")
public class PolicyController {

    private final PolicyEngineService policyEngineService;

    @PostMapping("/evaluate")
    @Operation(summary = "策略评估", description = "评估访问控制策略，返回决策结果（不抛异常）")
    public ApiResponse<PolicyEvaluationVO> evaluate(@Valid @RequestBody PolicyEvaluationRequest request) {
        // 注入当前用户 ID（覆盖请求中的 subjectId，防止越权）
        request.setSubjectId(StpUtil.getLoginIdAsLong());
        PolicyEvaluationVO result = policyEngineService.evaluate(request);
        log.debug("策略评估: userId={}, action={}, decision={}",
                request.getSubjectId(), request.getAction(), result.getDecision());
        return ApiResponse.ok(result);
    }

    @PostMapping("/enforce")
    @Operation(summary = "强制策略评估", description = "高风险操作前的最终校验，DENY 时抛 BizException")
    @RequireScope(value = {"admin:all"}, mode = com.axiqra.api.annotation.RequireScope.RequireMode.ANY)
    public ApiResponse<Void> enforce(@Valid @RequestBody PolicyEvaluationRequest request) {
        request.setSubjectId(StpUtil.getLoginIdAsLong());
        policyEngineService.enforce(request);
        return ApiResponse.ok();
    }

    @GetMapping("/check")
    @Operation(summary = "快速权限检查", description = "检查当前用户是否持有指定 scope")
    public ApiResponse<Boolean> checkScope(@RequestParam String scope) {
        long userId = StpUtil.getLoginIdAsLong();
        boolean hasScope = policyEngineService.hasScope(userId, scope);
        return ApiResponse.ok(hasScope);
    }
}
