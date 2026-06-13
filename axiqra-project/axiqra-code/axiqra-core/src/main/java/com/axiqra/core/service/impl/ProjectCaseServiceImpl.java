package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.dto.ProjectCaseCreateRequest;
import com.axiqra.common.domain.entity.AuthorizationEntity;
import com.axiqra.common.domain.entity.EngineeringTraceEntity;
import com.axiqra.common.domain.entity.ProjectCaseEntity;
import com.axiqra.common.domain.enums.LicenseScope;
import com.axiqra.common.domain.enums.ScopeEnum;
import com.axiqra.common.domain.enums.TraceStatus;
import com.axiqra.common.domain.vo.ProjectCaseDetailVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.AuthorizationMapper;
import com.axiqra.core.mapper.EngineeringTraceMapper;
import com.axiqra.core.mapper.ProjectCaseMapper;
import com.axiqra.core.service.ProjectCaseService;
import com.axiqra.core.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project Case 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCaseServiceImpl implements ProjectCaseService {

    private static final String STATUS_PRIVATE = "private";
    private static final String STATUS_PENDING_REVIEW = "pending_review";
    private static final String REDACTION_COMPLETE = "complete";
    private static final String AUTH_STATUS_ACTIVE = "active";

    private final ProjectCaseMapper projectCaseMapper;
    private final EngineeringTraceMapper engineeringTraceMapper;
    private final AuthorizationMapper authorizationMapper;
    private final RbacService rbacService;
    private final AuditPort auditPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectCaseDetailVO create(Long userId, ProjectCaseCreateRequest request) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!rbacService.hasScope(userId, ScopeEnum.CASE_WRITE.getCode())) {
            throw new BizException(ErrorCode.FORBIDDEN, "缺少 case:write 权限");
        }
        validateCreateRequest(request);

        EngineeringTraceEntity trace = engineeringTraceMapper.selectActiveById(request.getTraceId());
        if (trace == null) {
            throw new BizException(ErrorCode.TRACE_NOT_FOUND);
        }
        if (!userId.equals(trace.getAuthorId()) && !rbacService.isAdmin(userId, trace.getWorkspaceId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权基于该 Trace 创建 Case");
        }
        if (trace.getStatus() == null || !trace.getStatus().isSubmitted()) {
            throw new BizException(ErrorCode.CASE_SOURCE_MISSING, "Trace 尚未提交，不能创建 Case");
        }

        LicenseScope licenseScope = LicenseScope.of(request.getLicenseScope().trim());
        if (licenseScope == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不支持的 licenseScope: " + request.getLicenseScope());
        }

        ProjectCaseEntity entity = new ProjectCaseEntity()
                .setTraceId(trace.getId())
                .setWorkspaceId(trace.getWorkspaceId())
                .setProjectId(trace.getProjectId())
                .setAuthorId(trace.getAuthorId())
                .setAuthorizationId(null)
                .setVisibilityScope(trace.getVisibilityScope() != null ? trace.getVisibilityScope().getCode() : "workspace")
                .setLicenseScope(licenseScope.getCode())
                .setRedactionStatus(request.getRedactionStatus().trim())
                .setStatus(STATUS_PRIVATE)
                .setReviewId(null)
                .setDeleted(false);
        try {
            projectCaseMapper.insertSelective(entity);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateTraceViolation(ex)) {
                throw new BizException(ErrorCode.CASE_DUPLICATE_SOURCE);
            }
            throw ex;
        }

        auditPort.log(AuditPort.AuditEvent.builder()
                .requestId(traceId())
                .actorId(userId)
                .actorType("user")
                .action("project_case.create")
                .objectType("project_case")
                .objectId(entity.getId())
                .result("success")
                .payload(java.util.Map.of(
                        "traceId", trace.getId(),
                        "licenseScope", entity.getLicenseScope(),
                        "redactionStatus", entity.getRedactionStatus()
                ))
                .tenantId(null)
                .build());

        return toDetailVO(entity);
    }

    @Override
    public ProjectCaseDetailVO getDetail(Long userId, Long caseId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (caseId == null || caseId <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "caseId 不能为空且必须大于 0");
        }
        ProjectCaseEntity entity = projectCaseMapper.selectActiveById(caseId);
        if (entity == null) {
            throw new BizException(ErrorCode.PROJECT_CASE_NOT_FOUND);
        }
        if (!canAccess(userId, entity)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该 Project Case");
        }
        return toDetailVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectCaseDetailVO requestPublish(Long userId, Long caseId, Long authorizationId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!rbacService.hasScope(userId, ScopeEnum.CASE_PUBLISH.getCode())) {
            throw new BizException(ErrorCode.FORBIDDEN, "缺少 case:publish 权限");
        }
        if (authorizationId == null || authorizationId <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "authorizationId 不能为空且必须大于 0");
        }
        ProjectCaseEntity entity = projectCaseMapper.selectActiveById(caseId);
        if (entity == null) {
            throw new BizException(ErrorCode.PROJECT_CASE_NOT_FOUND);
        }
        if (!userId.equals(entity.getAuthorId()) && !rbacService.isAdmin(userId, entity.getWorkspaceId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权发布该 Project Case");
        }
        if (!REDACTION_COMPLETE.equalsIgnoreCase(entity.getRedactionStatus())) {
            throw new BizException(ErrorCode.CASE_REDACTION_INCOMPLETE);
        }
        if (entity.getLicenseScope() == null || entity.getLicenseScope().isBlank()) {
            throw new BizException(ErrorCode.LICENSE_SCOPE_MISSING);
        }

        AuthorizationEntity authorization = authorizationMapper.selectActiveById(authorizationId);
        if (authorization == null) {
            throw new BizException(ErrorCode.CASE_SOURCE_MISSING, "授权记录不存在");
        }
        if (!userId.equals(authorization.getOwnerId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "授权记录不属于当前用户");
        }
        if (!AUTH_STATUS_ACTIVE.equalsIgnoreCase(authorization.getStatus())) {
            throw new BizException(ErrorCode.AUTHORIZATION_REVOKED);
        }
        String authorizationLicenseScope = authorization.getLicenseScope();
        if (authorizationLicenseScope == null
                || !entity.getLicenseScope().equalsIgnoreCase(authorizationLicenseScope)) {
            throw new BizException(ErrorCode.LICENSE_SCOPE_MISSING, "授权范围与 Case 的 licenseScope 不匹配");
        }

        entity.setAuthorizationId(authorizationId);
        entity.setStatus(STATUS_PENDING_REVIEW);
        int rows = projectCaseMapper.update(entity);
        if (rows == 0) {
            throw new BizException(ErrorCode.CONCURRENT_MODIFICATION, "Project Case 已被其他人修改，请重试");
        }

        auditPort.logAuthorizationChange(new AuditPort.AuthorizationEvent(
                traceId(),
                authorizationId,
                authorization.getStatus(),
                STATUS_PENDING_REVIEW,
                userId,
                authorization.getLicenseScope()
        ));

        return toDetailVO(entity);
    }

    private void validateCreateRequest(ProjectCaseCreateRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "request 不能为空");
        }
        if (request.getTraceId() == null || request.getTraceId() <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "traceId 不能为空且必须大于 0");
        }
        if (request.getLicenseScope() == null || request.getLicenseScope().isBlank()) {
            throw new BizException(ErrorCode.LICENSE_SCOPE_MISSING);
        }
        if (request.getRedactionStatus() == null || request.getRedactionStatus().isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "redactionStatus 不能为空");
        }
    }

    private boolean canAccess(Long userId, ProjectCaseEntity entity) {
        if (userId.equals(entity.getAuthorId())) {
            return true;
        }
        if (!rbacService.hasScope(userId, ScopeEnum.CASE_READ.getCode())) {
            return false;
        }
        return entity.getWorkspaceId() != null && rbacService.isMember(userId, entity.getWorkspaceId());
    }

    private ProjectCaseDetailVO toDetailVO(ProjectCaseEntity entity) {
        return ProjectCaseDetailVO.builder()
                .id(entity.getId())
                .traceId(entity.getTraceId())
                .workspaceId(entity.getWorkspaceId())
                .projectId(entity.getProjectId())
                .authorId(entity.getAuthorId())
                .authorizationId(entity.getAuthorizationId())
                .visibilityScope(entity.getVisibilityScope())
                .licenseScope(entity.getLicenseScope())
                .redactionStatus(entity.getRedactionStatus())
                .status(entity.getStatus())
                .reviewId(entity.getReviewId())
                .gmtCreate(entity.getGmtCreate())
                .gmtModified(entity.getGmtModified())
                .build();
    }

    private String traceId() {
        String traceId = MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? "missing-trace-id" : traceId;
    }

    private boolean isDuplicateTraceViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getRootCause();
        if (cause instanceof java.sql.SQLIntegrityConstraintViolationException sqlEx) {
            String message = sqlEx.getMessage() != null ? sqlEx.getMessage().toLowerCase() : "unknown";
            return message.contains("trace_id");
        }
        return false;
    }
}
