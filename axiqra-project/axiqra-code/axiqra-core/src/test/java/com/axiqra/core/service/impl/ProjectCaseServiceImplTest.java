package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.dto.ProjectCaseCreateRequest;
import com.axiqra.common.domain.entity.AuthorizationEntity;
import com.axiqra.common.domain.entity.EngineeringTraceEntity;
import com.axiqra.common.domain.entity.ProjectCaseEntity;
import com.axiqra.common.domain.enums.LicenseScope;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.TraceStatus;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.axiqra.common.domain.vo.ProjectCaseDetailVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.AuthorizationMapper;
import com.axiqra.core.mapper.EngineeringTraceMapper;
import com.axiqra.core.mapper.ProjectCaseMapper;
import com.axiqra.core.service.RbacService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectCaseServiceImpl 单元测试")
class ProjectCaseServiceImplTest {

    @Mock
    private ProjectCaseMapper projectCaseMapper;

    @Mock
    private EngineeringTraceMapper engineeringTraceMapper;

    @Mock
    private AuthorizationMapper authorizationMapper;

    @Mock
    private RbacService rbacService;

    @Mock
    private AuditPort auditPort;

    @InjectMocks
    private ProjectCaseServiceImpl projectCaseService;

    @Test
    @DisplayName("已提交 Trace 可创建私有 Project Case")
    void shouldCreateProjectCaseFromSubmittedTrace() {
        ProjectCaseCreateRequest request = ProjectCaseCreateRequest.builder()
                .traceId(88L)
                .licenseScope("open_source")
                .redactionStatus("complete")
                .build();
        EngineeringTraceEntity trace = traceEntity();
        when(rbacService.hasScope(1L, "case:write")).thenReturn(true);
        when(engineeringTraceMapper.selectActiveById(88L)).thenReturn(trace);

        ArgumentCaptor<ProjectCaseEntity> captor = ArgumentCaptor.forClass(ProjectCaseEntity.class);
        org.mockito.Mockito.doAnswer(invocation -> {
            ProjectCaseEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            return 1;
        }).when(projectCaseMapper).insertSelective(captor.capture());

        ProjectCaseDetailVO result = projectCaseService.create(1L, request);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("private", result.getStatus());
        assertEquals("open_source", captor.getValue().getLicenseScope());
        verify(auditPort).log(any());
    }

    @Test
    @DisplayName("同一 Trace 不能重复创建 Project Case — DB 唯一约束冲突")
    void shouldRejectDuplicateViaDbConstraint() {
        ProjectCaseCreateRequest request = ProjectCaseCreateRequest.builder()
                .traceId(88L)
                .licenseScope("open_source")
                .redactionStatus("complete")
                .build();
        EngineeringTraceEntity trace = traceEntity();
        when(rbacService.hasScope(1L, "case:write")).thenReturn(true);
        when(engineeringTraceMapper.selectActiveById(88L)).thenReturn(trace);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException(
                        "duplicate key",
                        new SQLIntegrityConstraintViolationException("duplicate key: trace_id")))
                .when(projectCaseMapper).insertSelective(any());

        BizException ex = assertThrows(BizException.class, () -> projectCaseService.create(1L, request));

        assertEquals(ErrorCode.CASE_DUPLICATE_SOURCE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("Trace 未提交时不能创建 Case")
    void shouldRejectUnsubmittedTrace() {
        ProjectCaseCreateRequest request = ProjectCaseCreateRequest.builder()
                .traceId(88L)
                .licenseScope("open_source")
                .redactionStatus("complete")
                .build();
        EngineeringTraceEntity trace = traceEntity();
        trace.setStatus(TraceStatus.DRAFT);
        when(rbacService.hasScope(1L, "case:write")).thenReturn(true);
        when(engineeringTraceMapper.selectActiveById(88L)).thenReturn(trace);

        BizException ex = assertThrows(BizException.class, () -> projectCaseService.create(1L, request));

        assertEquals(ErrorCode.CASE_SOURCE_MISSING.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作者可读取 Project Case 详情")
    void shouldAllowAuthorToReadDetail() {
        when(projectCaseMapper.selectActiveById(101L)).thenReturn(projectCaseEntity());

        ProjectCaseDetailVO result = projectCaseService.getDetail(1L, 101L);

        assertEquals(101L, result.getId());
    }

    @Test
    @DisplayName("脱敏完成且授权匹配时可发起发布申请")
    void shouldRequestPublishWhenAuthorizationMatches() {
        ProjectCaseEntity entity = projectCaseEntity();
        AuthorizationEntity authorization = authorizationEntity();
        when(rbacService.hasScope(1L, "case:publish")).thenReturn(true);
        when(projectCaseMapper.selectActiveById(101L)).thenReturn(entity);
        when(authorizationMapper.selectActiveById(201L)).thenReturn(authorization);
        when(projectCaseMapper.update(any())).thenReturn(1);

        ProjectCaseDetailVO result = projectCaseService.requestPublish(1L, 101L, 201L);

        assertEquals("pending_review", result.getStatus());
        assertEquals(201L, result.getAuthorizationId());
        verify(projectCaseMapper).update(entity);
        verify(auditPort).logAuthorizationChange(any());
    }

    @Test
    @DisplayName("脱敏未完成时应拒绝发布申请")
    void shouldRejectPublishWhenRedactionIncomplete() {
        ProjectCaseEntity entity = projectCaseEntity();
        entity.setRedactionStatus("pending");
        when(rbacService.hasScope(1L, "case:publish")).thenReturn(true);
        when(projectCaseMapper.selectActiveById(101L)).thenReturn(entity);

        BizException ex = assertThrows(BizException.class, () -> projectCaseService.requestPublish(1L, 101L, 201L));

        assertEquals(ErrorCode.CASE_REDACTION_INCOMPLETE.getCode(), ex.getCode());
        verify(authorizationMapper, never()).selectActiveById(any());
    }

    @Test
    @DisplayName("授权范围不匹配时应拒绝发布申请")
    void shouldRejectPublishWhenLicenseScopeMismatch() {
        ProjectCaseEntity entity = projectCaseEntity();
        AuthorizationEntity authorization = authorizationEntity();
        authorization.setLicenseScope(LicenseScope.PROPRIETARY.getCode());
        when(rbacService.hasScope(1L, "case:publish")).thenReturn(true);
        when(projectCaseMapper.selectActiveById(101L)).thenReturn(entity);
        when(authorizationMapper.selectActiveById(201L)).thenReturn(authorization);

        BizException ex = assertThrows(BizException.class, () -> projectCaseService.requestPublish(1L, 101L, 201L));

        assertEquals(ErrorCode.LICENSE_SCOPE_MISSING.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("授权范围为空时应拒绝发布申请")
    void shouldRejectPublishWhenAuthorizationLicenseScopeMissing() {
        ProjectCaseEntity entity = projectCaseEntity();
        AuthorizationEntity authorization = authorizationEntity();
        authorization.setLicenseScope(null);
        when(rbacService.hasScope(1L, "case:publish")).thenReturn(true);
        when(projectCaseMapper.selectActiveById(101L)).thenReturn(entity);
        when(authorizationMapper.selectActiveById(201L)).thenReturn(authorization);

        BizException ex = assertThrows(BizException.class, () -> projectCaseService.requestPublish(1L, 101L, 201L));

        assertEquals(ErrorCode.LICENSE_SCOPE_MISSING.getCode(), ex.getCode());
    }

    private EngineeringTraceEntity traceEntity() {
        EngineeringTraceEntity trace = new EngineeringTraceEntity();
        trace.setId(88L);
        trace.setWorkspaceId(100L);
        trace.setProjectId(10L);
        trace.setAuthorId(1L);
        trace.setStatus(TraceStatus.SUBMITTED);
        trace.setRiskLevel(RiskLevel.R1);
        trace.setVisibilityScope(VisibilityScope.WORKSPACE);
        return trace;
    }

    private ProjectCaseEntity projectCaseEntity() {
        ProjectCaseEntity entity = new ProjectCaseEntity();
        entity.setId(101L);
        entity.setTraceId(88L);
        entity.setWorkspaceId(100L);
        entity.setProjectId(10L);
        entity.setAuthorId(1L);
        entity.setVisibilityScope("workspace");
        entity.setLicenseScope(LicenseScope.OPEN_SOURCE.getCode());
        entity.setRedactionStatus("complete");
        entity.setStatus("private");
        return entity;
    }

    private AuthorizationEntity authorizationEntity() {
        AuthorizationEntity entity = new AuthorizationEntity();
        entity.setId(201L);
        entity.setOwnerId(1L);
        entity.setLicenseScope(LicenseScope.OPEN_SOURCE.getCode());
        entity.setStatus("active");
        return entity;
    }
}
