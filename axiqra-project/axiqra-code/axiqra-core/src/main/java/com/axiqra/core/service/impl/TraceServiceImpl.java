package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.TraceConfirmRequest;
import com.axiqra.common.domain.dto.TraceCreateRequest;
import com.axiqra.common.domain.dto.TracePathDTO;
import com.axiqra.common.domain.entity.EngineeringTraceEntity;
import com.axiqra.common.domain.entity.TraceEvidenceRefEntity;
import com.axiqra.common.domain.enums.IndexStatus;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.ScopeEnum;
import com.axiqra.common.domain.enums.TraceStatus;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.axiqra.common.domain.vo.TraceDetailVO;
import com.axiqra.common.domain.vo.TraceEvidenceVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.EngineeringTraceMapper;
import com.axiqra.core.mapper.TraceEvidenceRefMapper;
import com.axiqra.core.service.RbacService;
import com.axiqra.core.service.TraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Trace 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceServiceImpl implements TraceService {

    private final EngineeringTraceMapper engineeringTraceMapper;
    private final TraceEvidenceRefMapper traceEvidenceRefMapper;
    private final RbacService rbacService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TraceDetailVO createDraft(Long userId, TraceCreateRequest request) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!rbacService.hasScope(userId, ScopeEnum.TRACE_WRITE.getCode())) {
            throw new BizException(ErrorCode.FORBIDDEN, "缺少 trace:write 权限");
        }
        validateCreateRequest(request);
        if (!rbacService.isMember(userId, request.getWorkspaceId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权在该工作空间提交 Trace");
        }

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            EngineeringTraceEntity existing = engineeringTraceMapper.selectByIdempotencyKey(request.getIdempotencyKey().trim());
            if (existing != null) {
                if (!userId.equals(existing.getAuthorId())) {
                    throw new BizException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
                }
                return toDetailVO(existing, traceEvidenceRefMapper.selectByTraceId(existing.getId()));
            }
        }

        RiskLevel riskLevel = RiskLevel.ofLevel(request.getRiskLevel());
        if (riskLevel == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不支持的 riskLevel: " + request.getRiskLevel());
        }
        VisibilityScope visibilityScope = VisibilityScope.of(request.getVisibilityScope());
        if (visibilityScope == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不支持的 visibilityScope: " + request.getVisibilityScope());
        }

        EngineeringTraceEntity entity = new EngineeringTraceEntity()
                .setWorkspaceId(request.getWorkspaceId())
                .setProjectId(request.getProjectId())
                .setAuthorId(userId)
                .setToolType(trimToNull(request.getToolType()))
                .setTaskGoal(request.getTaskGoal().trim())
                .setContextSnapshot(trimToNull(request.getContextSnapshot()))
                .setForwardSteps(trimToNull(request.getForwardSteps()))
                .setReversePath(trimToNull(request.getReversePath()))
                .setDecisions(trimToNull(request.getDecisions()))
                .setDecisionPath(trimToNull(request.getDecisionPath()))
                .setRollbackPath(trimToNull(request.getRollbackPath()))
                .setOutcome(request.getOutcome().trim())
                .setRiskLevel(riskLevel)
                .setStatus(TraceStatus.DRAFT)
                .setUserConfirmation(null)
                .setIdempotencyKey(trimToNull(request.getIdempotencyKey()))
                .setVisibilityScope(visibilityScope)
                .setIndexStatus(IndexStatus.NOT_INDEXED)
                .setReviewId(null)
                .setSolutionId(request.getSolutionId())
                .setEvolutionSuggestion(trimToNull(request.getEvolutionSuggestion()))
                .setEvolutionHint(trimToNull(request.getEvolutionHint()))
                .setDeleted(false);
        engineeringTraceMapper.insertSelective(entity);

        for (TraceCreateRequest.TraceEvidenceItem evidence : request.getEvidences()) {
            TraceEvidenceRefEntity evidenceEntity = new TraceEvidenceRefEntity()
                    .setTraceId(entity.getId())
                    .setUri(evidence.getUri().trim())
                    .setHash(trimToNull(evidence.getHash()))
                    .setType(evidence.getType().trim())
                    .setSizeBytes(evidence.getSizeBytes())
                    .setDeleted(false)
                    .setTenantId(null);
            traceEvidenceRefMapper.insertSelective(evidenceEntity);
        }

        List<TraceEvidenceRefEntity> evidences = traceEvidenceRefMapper.selectByTraceId(entity.getId());
        log.info("创建 Trace 草稿: traceId={}, workspaceId={}, userId={}", entity.getId(), entity.getWorkspaceId(), userId);
        return toDetailVO(entity, evidences);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TraceDetailVO confirm(Long userId, Long traceId, TraceConfirmRequest request) {
        EngineeringTraceEntity trace = requireEditableTrace(userId, traceId, ScopeEnum.TRACE_CONFIRM.getCode());
        if (request == null || request.getUserConfirmation() == null || request.getUserConfirmation().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "userConfirmation 不能为空");
        }
        if (trace.getStatus() != TraceStatus.DRAFT && trace.getStatus() != TraceStatus.USER_CONFIRMED) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "当前状态不允许确认 Trace");
        }

        trace.setUserConfirmation(request.getUserConfirmation().trim());
        trace.setStatus(TraceStatus.USER_CONFIRMED);
        engineeringTraceMapper.update(trace);
        return toDetailVO(trace, traceEvidenceRefMapper.selectByTraceId(trace.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TraceDetailVO submit(Long userId, Long traceId) {
        EngineeringTraceEntity trace = requireEditableTrace(userId, traceId, ScopeEnum.TRACE_WRITE.getCode());
        if (trace.getStatus() != TraceStatus.USER_CONFIRMED) {
            throw new BizException(ErrorCode.TRACE_USER_CONFIRMATION_PENDING);
        }
        List<TraceEvidenceRefEntity> evidences = defaultIfNull(traceEvidenceRefMapper.selectByTraceId(trace.getId()));
        if (evidences.isEmpty()) {
            throw new BizException(ErrorCode.TRACE_EVIDENCE_MISSING);
        }

        trace.setStatus(trace.getRiskLevel() != null && trace.getRiskLevel().requiresHumanReview()
                ? TraceStatus.NEEDS_REVIEW
                : TraceStatus.SUBMITTED);
        engineeringTraceMapper.update(trace);
        return toDetailVO(trace, evidences);
    }

    @Override
    public TraceDetailVO getDetail(Long userId, Long traceId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (traceId == null || traceId <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "traceId 不能为空且必须大于 0");
        }
        EngineeringTraceEntity trace = engineeringTraceMapper.selectActiveById(traceId);
        if (trace == null) {
            throw new BizException(ErrorCode.TRACE_NOT_FOUND);
        }
        if (!canViewTrace(userId, trace)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该 Trace");
        }
        return toDetailVO(trace, traceEvidenceRefMapper.selectByTraceId(trace.getId()));
    }

    @Override
    public List<TraceDetailVO> listByUser(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!rbacService.hasScope(userId, ScopeEnum.TRACE_READ.getCode())) {
            throw new BizException(ErrorCode.FORBIDDEN, "缺少 trace:read 权限");
        }
        // 查询用户创建的 Trace
        List<EngineeringTraceEntity> traces = engineeringTraceMapper.selectByAuthorId(userId);
        if (traces == null || traces.isEmpty()) {
            return Collections.emptyList();
        }
        return traces.stream()
                .map(trace -> toDetailVO(trace, traceEvidenceRefMapper.selectByTraceId(trace.getId())))
                .toList();
    }

    private EngineeringTraceEntity requireEditableTrace(Long userId, Long traceId, String requiredScope) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!rbacService.hasScope(userId, requiredScope)) {
            throw new BizException(ErrorCode.FORBIDDEN, "权限不足");
        }
        if (traceId == null || traceId <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "traceId 不能为空且必须大于 0");
        }
        EngineeringTraceEntity trace = engineeringTraceMapper.selectActiveById(traceId);
        if (trace == null) {
            throw new BizException(ErrorCode.TRACE_NOT_FOUND);
        }
        if (!userId.equals(trace.getAuthorId()) && !rbacService.isAdmin(userId, trace.getWorkspaceId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权修改该 Trace");
        }
        return trace;
    }

    private boolean canViewTrace(Long userId, EngineeringTraceEntity trace) {
        if (userId.equals(trace.getAuthorId())) {
            return true;
        }
        if (rbacService.hasScope(userId, ScopeEnum.TRACE_ADMIN.getCode())) {
            return true;
        }
        if (!rbacService.hasScope(userId, ScopeEnum.TRACE_READ.getCode())) {
            return false;
        }
        if (trace.getVisibilityScope() == null) {
            return false;
        }
        return switch (trace.getVisibilityScope()) {
            case PRIVATE -> false;
            case WORKSPACE, ENTERPRISE -> trace.getWorkspaceId() != null && rbacService.isMember(userId, trace.getWorkspaceId());
            case PUBLIC -> true;
        };
    }

    private void validateCreateRequest(TraceCreateRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "request 不能为空");
        }
        if (request.getWorkspaceId() == null || request.getWorkspaceId() <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "workspaceId 不能为空且必须大于 0");
        }
        if (request.getTaskGoal() == null || request.getTaskGoal().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "taskGoal 不能为空");
        }
        if (request.getOutcome() == null || request.getOutcome().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "outcome 不能为空");
        }
        if (request.getRiskLevel() == null || request.getRiskLevel().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "riskLevel 不能为空");
        }
        List<TraceCreateRequest.TraceEvidenceItem> evidences = request.getEvidences();
        if (evidences == null || evidences.isEmpty()) {
            throw new BizException(ErrorCode.TRACE_EVIDENCE_MISSING);
        }
        boolean invalidEvidence = evidences.stream().filter(Objects::nonNull)
                .anyMatch(evidence -> evidence.getUri() == null || evidence.getUri().isBlank()
                        || evidence.getType() == null || evidence.getType().isBlank());
        if (invalidEvidence || evidences.stream().anyMatch(Objects::isNull)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "evidence uri/type 不能为空");
        }
    }

    private TraceDetailVO toDetailVO(EngineeringTraceEntity trace, List<TraceEvidenceRefEntity> evidences) {
        List<TraceEvidenceVO> evidenceViews = defaultIfNull(evidences).stream()
                .filter(Objects::nonNull)
                .map(evidence -> TraceEvidenceVO.builder()
                        .id(evidence.getId())
                        .uri(evidence.getUri())
                        .hash(evidence.getHash())
                        .type(evidence.getType())
                        .sizeBytes(evidence.getSizeBytes())
                        .build())
                .toList();
        return TraceDetailVO.builder()
                .id(trace.getId())
                .workspaceId(trace.getWorkspaceId())
                .projectId(trace.getProjectId())
                .authorId(trace.getAuthorId())
                .toolType(trace.getToolType())
                .taskGoal(trace.getTaskGoal())
                .contextSnapshot(trace.getContextSnapshot())
                .forwardSteps(trace.getForwardSteps())
                .reversePath(trace.getReversePath())
                .decisions(trace.getDecisions())
                .decisionPath(trace.getDecisionPath())
                .rollbackPath(trace.getRollbackPath())
                .outcome(trace.getOutcome())
                .riskLevel(trace.getRiskLevel() != null ? trace.getRiskLevel().getCode() : null)
                .status(trace.getStatus() != null ? trace.getStatus().getCode() : null)
                .userConfirmation(trace.getUserConfirmation())
                .idempotencyKey(trace.getIdempotencyKey())
                .visibilityScope(trace.getVisibilityScope() != null ? trace.getVisibilityScope().getCode() : null)
                .indexStatus(trace.getIndexStatus() != null ? trace.getIndexStatus().getCode() : null)
                .reviewId(trace.getReviewId())
                .solutionId(trace.getSolutionId())
                .evolutionSuggestion(trace.getEvolutionSuggestion())
                .evolutionHint(trace.getEvolutionHint())
                .evidencePath(trace.getEvidencePath())
                .gmtCreate(trace.getGmtCreate())
                .gmtModified(trace.getGmtModified())
                .evidences(evidenceViews)
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private <T> List<T> defaultIfNull(List<T> items) {
        return items != null ? items : Collections.emptyList();
    }

    @Override
    public void recordTracePath(Long invocationId, TracePathDTO tracePath) {
        log.debug("Recording trace path for invocation: {}, path: {}", invocationId, tracePath);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitEvidence(Long userId, Long traceId, List<String> evidenceRefs) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (traceId == null || traceId <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "traceId 不能为空且必须大于 0");
        }
        if (evidenceRefs == null || evidenceRefs.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "evidenceRefs 不能为空");
        }

        EngineeringTraceEntity trace = engineeringTraceMapper.selectActiveById(traceId);
        if (trace == null) {
            throw new BizException(ErrorCode.TRACE_NOT_FOUND);
        }
        if (!userId.equals(trace.getAuthorId()) && !rbacService.isAdmin(userId, trace.getWorkspaceId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权提交该 Trace 的证据");
        }

        // 将证据引用序列化为 JSON 存储到 evidencePath 字段
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String evidencePathJson = objectMapper.writeValueAsString(evidenceRefs);
            trace.setEvidencePath(evidencePathJson);
            engineeringTraceMapper.update(trace);
            log.info("提交 Trace 证据: traceId={}, evidenceCount={}", traceId, evidenceRefs.size());
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "证据序列化失败: " + e.getMessage());
        }
    }
}
