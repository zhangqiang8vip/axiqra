package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.TraceConfirmRequest;
import com.axiqra.common.domain.dto.TraceCreateRequest;
import com.axiqra.common.domain.entity.EngineeringTraceEntity;
import com.axiqra.common.domain.entity.TraceEvidenceRefEntity;
import com.axiqra.common.domain.enums.IndexStatus;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.TraceStatus;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.axiqra.common.domain.vo.TraceDetailVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.EngineeringTraceMapper;
import com.axiqra.core.mapper.TraceEvidenceRefMapper;
import com.axiqra.core.service.RbacService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TraceServiceImpl 单元测试")
class TraceServiceImplTest {

    @Mock
    private EngineeringTraceMapper engineeringTraceMapper;

    @Mock
    private TraceEvidenceRefMapper traceEvidenceRefMapper;

    @Mock
    private RbacService rbacService;

    @InjectMocks
    private TraceServiceImpl traceService;

    @Test
    @DisplayName("有 trace:write scope 且是空间成员时应创建 Trace 草稿")
    void shouldCreateDraftWhenAuthorized() {
        TraceCreateRequest request = baseCreateRequest();
        when(rbacService.hasScope(1L, "trace:write")).thenReturn(true);
        when(rbacService.isMember(1L, 100L)).thenReturn(true);
        when(engineeringTraceMapper.selectByIdempotencyKey("idem-1")).thenReturn(null);
        when(traceEvidenceRefMapper.selectByTraceId(101L)).thenReturn(List.of(evidenceEntity(1001L, 101L)));

        ArgumentCaptor<EngineeringTraceEntity> traceCaptor = ArgumentCaptor.forClass(EngineeringTraceEntity.class);
        ArgumentCaptor<TraceEvidenceRefEntity> evidenceCaptor = ArgumentCaptor.forClass(TraceEvidenceRefEntity.class);
        org.mockito.Mockito.doAnswer(invocation -> {
            EngineeringTraceEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            return 1;
        }).when(engineeringTraceMapper).insertSelective(traceCaptor.capture());

        org.mockito.Mockito.doAnswer(invocation -> 1).when(traceEvidenceRefMapper).insertSelective(evidenceCaptor.capture());

        TraceDetailVO result = traceService.createDraft(1L, request);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("draft", result.getStatus());
        assertEquals("R2", result.getRiskLevel());
        assertEquals(1, result.getEvidences().size());
        assertEquals(IndexStatus.NOT_INDEXED.getCode(), result.getIndexStatus());
        assertEquals(TraceStatus.DRAFT, traceCaptor.getValue().getStatus());
        assertEquals(VisibilityScope.WORKSPACE, traceCaptor.getValue().getVisibilityScope());
        assertEquals("artifact", evidenceCaptor.getValue().getType());
    }

    @Test
    @DisplayName("重复幂等键命中本人 Trace 时应直接复用")
    void shouldReuseExistingTraceWhenIdempotencyKeyMatchesAuthor() {
        TraceCreateRequest request = baseCreateRequest();
        EngineeringTraceEntity existing = traceEntity(88L, 1L, TraceStatus.DRAFT, RiskLevel.R1);
        existing.setIdempotencyKey("idem-1");
        when(rbacService.hasScope(1L, "trace:write")).thenReturn(true);
        when(rbacService.isMember(1L, 100L)).thenReturn(true);
        when(engineeringTraceMapper.selectByIdempotencyKey("idem-1")).thenReturn(existing);
        when(traceEvidenceRefMapper.selectByTraceId(88L)).thenReturn(List.of(evidenceEntity(2001L, 88L)));

        TraceDetailVO result = traceService.createDraft(1L, request);

        assertEquals(88L, result.getId());
        verify(engineeringTraceMapper, never()).insertSelective(any());
    }

    @Test
    @DisplayName("确认后应进入 user_confirmed 状态")
    void shouldConfirmTrace() {
        EngineeringTraceEntity trace = traceEntity(88L, 1L, TraceStatus.DRAFT, RiskLevel.R1);
        when(rbacService.hasScope(1L, "trace:confirm")).thenReturn(true);
        when(engineeringTraceMapper.selectActiveById(88L)).thenReturn(trace);
        when(traceEvidenceRefMapper.selectByTraceId(88L)).thenReturn(List.of(evidenceEntity(2001L, 88L)));

        TraceDetailVO result = traceService.confirm(1L, 88L, TraceConfirmRequest.builder()
                .userConfirmation("已由用户确认")
                .build());

        assertEquals("user_confirmed", result.getStatus());
        assertEquals("已由用户确认", result.getUserConfirmation());
        verify(engineeringTraceMapper).update(trace);
    }

    @Test
    @DisplayName("未确认直接提交时应返回等待用户确认")
    void shouldRejectSubmitWhenConfirmationMissing() {
        EngineeringTraceEntity trace = traceEntity(88L, 1L, TraceStatus.DRAFT, RiskLevel.R1);
        when(rbacService.hasScope(1L, "trace:write")).thenReturn(true);
        when(engineeringTraceMapper.selectActiveById(88L)).thenReturn(trace);

        BizException ex = assertThrows(BizException.class, () -> traceService.submit(1L, 88L));

        assertEquals(ErrorCode.TRACE_USER_CONFIRMATION_PENDING.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("中高风险 Trace 提交后应进入 needs_review")
    void shouldSubmitToNeedsReviewForHumanReviewRisk() {
        EngineeringTraceEntity trace = traceEntity(88L, 1L, TraceStatus.USER_CONFIRMED, RiskLevel.R3);
        when(rbacService.hasScope(1L, "trace:write")).thenReturn(true);
        when(engineeringTraceMapper.selectActiveById(88L)).thenReturn(trace);
        when(traceEvidenceRefMapper.selectByTraceId(88L)).thenReturn(List.of(evidenceEntity(2001L, 88L)));

        TraceDetailVO result = traceService.submit(1L, 88L);

        assertEquals("needs_review", result.getStatus());
        verify(engineeringTraceMapper).update(trace);
    }

    @Test
    @DisplayName("低风险 Trace 提交后应进入 submitted")
    void shouldSubmitDirectlyForLowRiskTrace() {
        EngineeringTraceEntity trace = traceEntity(89L, 1L, TraceStatus.USER_CONFIRMED, RiskLevel.R1);
        when(rbacService.hasScope(1L, "trace:write")).thenReturn(true);
        when(engineeringTraceMapper.selectActiveById(89L)).thenReturn(trace);
        when(traceEvidenceRefMapper.selectByTraceId(89L)).thenReturn(List.of(evidenceEntity(2002L, 89L)));

        TraceDetailVO result = traceService.submit(1L, 89L);

        assertEquals("submitted", result.getStatus());
    }

    @Test
    @DisplayName("详情读取时作者总是可见")
    void shouldAllowAuthorToViewTraceDetail() {
        EngineeringTraceEntity trace = traceEntity(90L, 1L, TraceStatus.SUBMITTED, RiskLevel.R1);
        trace.setVisibilityScope(VisibilityScope.PRIVATE);
        when(engineeringTraceMapper.selectActiveById(90L)).thenReturn(trace);
        when(traceEvidenceRefMapper.selectByTraceId(90L)).thenReturn(List.of(evidenceEntity(2003L, 90L)));

        TraceDetailVO result = traceService.getDetail(1L, 90L);

        assertEquals(90L, result.getId());
        assertFalse(result.getEvidences().isEmpty());
    }

    @Test
    @DisplayName("非成员读取 workspace Trace 时应拒绝")
    void shouldRejectViewWhenNoAccess() {
        EngineeringTraceEntity trace = traceEntity(91L, 2L, TraceStatus.SUBMITTED, RiskLevel.R1);
        trace.setVisibilityScope(VisibilityScope.WORKSPACE);
        when(engineeringTraceMapper.selectActiveById(91L)).thenReturn(trace);
        when(rbacService.hasScope(1L, "trace:admin")).thenReturn(false);
        when(rbacService.hasScope(1L, "trace:read")).thenReturn(true);
        when(rbacService.isMember(1L, 100L)).thenReturn(false);

        BizException ex = assertThrows(BizException.class, () -> traceService.getDetail(1L, 91L));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("证据缺失时应拒绝创建")
    void shouldRejectCreateWhenEvidenceMissing() {
        TraceCreateRequest request = baseCreateRequest();
        request.setEvidences(List.of());
        when(rbacService.hasScope(1L, "trace:write")).thenReturn(true);

        BizException ex = assertThrows(BizException.class, () -> traceService.createDraft(1L, request));

        assertEquals(ErrorCode.TRACE_EVIDENCE_MISSING.getCode(), ex.getCode());
    }

    private TraceCreateRequest baseCreateRequest() {
        return TraceCreateRequest.builder()
                .workspaceId(100L)
                .projectId(10L)
                .taskGoal("补齐 Trace 闭环")
                .toolType("cursor")
                .contextSnapshot("ctx")
                .forwardSteps("step1 -> step2")
                .reversePath("git revert")
                .decisions("采用最小实现")
                .rollbackPath("restore previous state")
                .outcome("实现成功")
                .riskLevel("R2")
                .visibilityScope("workspace")
                .solutionId(77L)
                .evolutionSuggestion("后续补发布链路")
                .idempotencyKey("idem-1")
                .evidences(List.of(TraceCreateRequest.TraceEvidenceItem.builder()
                        .uri("minio://bucket/evidence-1")
                        .hash("sha256:abc")
                        .type("artifact")
                        .sizeBytes(128L)
                        .build()))
                .build();
    }

    private EngineeringTraceEntity traceEntity(Long id, Long authorId, TraceStatus status, RiskLevel riskLevel) {
        EngineeringTraceEntity entity = new EngineeringTraceEntity();
        entity.setId(id);
        entity.setWorkspaceId(100L);
        entity.setProjectId(10L);
        entity.setAuthorId(authorId);
        entity.setToolType("cursor");
        entity.setTaskGoal("补齐 Trace 闭环");
        entity.setOutcome("实现成功");
        entity.setRiskLevel(riskLevel);
        entity.setStatus(status);
        entity.setVisibilityScope(VisibilityScope.WORKSPACE);
        entity.setIndexStatus(IndexStatus.NOT_INDEXED);
        entity.setIdempotencyKey("idem-1");
        return entity;
    }

    private TraceEvidenceRefEntity evidenceEntity(Long id, Long traceId) {
        TraceEvidenceRefEntity entity = new TraceEvidenceRefEntity();
        entity.setId(id);
        entity.setTraceId(traceId);
        entity.setUri("minio://bucket/evidence-1");
        entity.setHash("sha256:abc");
        entity.setType("artifact");
        entity.setSizeBytes(128L);
        return entity;
    }
}
