package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.dto.TraceConfirmRequest;
import com.axiqra.common.domain.dto.TraceCreateRequest;
import com.axiqra.common.domain.vo.TraceDetailVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.common.util.PrivacyUtils;
import com.axiqra.core.service.TraceService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工程轨迹 Controller
 */
@Slf4j
@RestController
@RequestMapping("/traces")
@RequiredArgsConstructor
@Tag(name = "工程轨迹", description = "AI 执行过程轨迹记录、确认与查询")
public class TraceController {

    private final TraceService traceService;

    @PostMapping
    @Operation(summary = "创建轨迹草稿", description = "记录 AI 执行过程的初始轨迹信息")
    public ApiResponse<TraceDetailVO> createDraft(@Valid @RequestBody TraceCreateRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        TraceDetailVO result = traceService.createDraft(userId, request);
        // 隐私合规：控制器日志不直接记录用户标识，改为稳定脱敏摘要。
        log.info("创建 Trace 草稿: traceId={}, actorHash={}", result.getId(), PrivacyUtils.pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @PostMapping("/{traceId}/confirm")
    @Operation(summary = "确认执行结果", description = "确认轨迹执行成功或失败，附带结果信息")
    public ApiResponse<TraceDetailVO> confirm(@PathVariable Long traceId,
                                              @Valid @RequestBody TraceConfirmRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        TraceDetailVO result = traceService.confirm(userId, traceId, request);
        log.info("确认 Trace: traceId={}, actorHash={}", traceId, PrivacyUtils.pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @PostMapping("/{traceId}/submit")
    @Operation(summary = "提交轨迹", description = "将确认后的轨迹提交进入审核流程")
    public ApiResponse<TraceDetailVO> submit(@PathVariable Long traceId) {
        long userId = StpUtil.getLoginIdAsLong();
        TraceDetailVO result = traceService.submit(userId, traceId);
        log.info("提交 Trace: traceId={}, actorHash={}", traceId, PrivacyUtils.pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @GetMapping
    @Operation(summary = "轨迹列表", description = "查询当前用户的所有轨迹记录")
    public ApiResponse<List<TraceDetailVO>> list() {
        long userId = StpUtil.getLoginIdAsLong();
        List<TraceDetailVO> result = traceService.listByUser(userId);
        log.info("读取 Trace 列表: count={}, actorHash={}", result.size(), PrivacyUtils.pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @GetMapping("/{traceId}")
    @Operation(summary = "轨迹详情", description = "查询单条轨迹的详细信息")
    public ApiResponse<TraceDetailVO> getDetail(@PathVariable Long traceId) {
        long userId = StpUtil.getLoginIdAsLong();
        TraceDetailVO result = traceService.getDetail(userId, traceId);
        log.info("读取 Trace 详情: traceId={}, actorHash={}", traceId, PrivacyUtils.pseudonymizeUserId(userId));
        return ApiResponse.ok(result);
    }

    @PostMapping("/{traceId}/evidence")
    @Operation(summary = "提交执行证据", description = "上传轨迹执行的证据路径列表，用于人工复核")
    public ApiResponse<Void> submitEvidence(@PathVariable Long traceId,
                                           @RequestBody EvidenceSubmitRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        traceService.submitEvidence(userId, traceId, request.evidenceRefs());
        log.info("提交 Trace 证据: traceId={}, evidenceCount={}", traceId, request.evidenceRefs().size());
        return ApiResponse.ok(null);
    }

    public record EvidenceSubmitRequest(List<String> evidenceRefs) {}
}
