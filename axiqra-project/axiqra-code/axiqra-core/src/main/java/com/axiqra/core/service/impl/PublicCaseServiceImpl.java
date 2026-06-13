package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.entity.ProjectCaseEntity;
import com.axiqra.common.domain.entity.PublicCaseEntity;
import com.axiqra.common.domain.enums.LicenseScope;
import com.axiqra.common.domain.enums.PublicCaseStatus;
import com.axiqra.common.domain.enums.ScopeEnum;
import com.axiqra.common.domain.vo.PublicCaseDetailVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.ProjectCaseMapper;
import com.axiqra.core.mapper.PublicCaseMapper;
import com.axiqra.core.service.PublicCaseService;
import com.axiqra.core.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Public Case 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicCaseServiceImpl implements PublicCaseService {

    private static final int DEFAULT_LIST_LIMIT = 10;
    private static final int MAX_LIST_LIMIT = 50;
    private static final String REDACTION_COMPLETE = "complete";
    private final PublicCaseMapper publicCaseMapper;
    private final ProjectCaseMapper projectCaseMapper;
    private final RbacService rbacService;
    private final AuditPort auditPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublicCaseDetailVO publish(Long userId, Long projectCaseId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!rbacService.hasScope(userId, ScopeEnum.CASE_PUBLISH.getCode())) {
            throw new BizException(ErrorCode.FORBIDDEN, "缺少 case:publish 权限");
        }
        if (projectCaseId == null || projectCaseId <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "projectCaseId 不能为空且必须大于 0");
        }

        ProjectCaseEntity sourceCase = projectCaseMapper.selectActiveById(projectCaseId);
        if (sourceCase == null) {
            throw new BizException(ErrorCode.PROJECT_CASE_NOT_FOUND);
        }
        if (!userId.equals(sourceCase.getAuthorId()) && !rbacService.isAdmin(userId, sourceCase.getWorkspaceId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权发布该 Project Case");
        }
        if (!REDACTION_COMPLETE.equalsIgnoreCase(sourceCase.getRedactionStatus())) {
            throw new BizException(ErrorCode.CASE_REDACTION_INCOMPLETE);
        }
        if (sourceCase.getAuthorizationId() == null || sourceCase.getAuthorizationId() <= 0) {
            throw new BizException(ErrorCode.LICENSE_SCOPE_MISSING, "缺少授权记录，不能公开发布");
        }
        if (sourceCase.getLicenseScope() == null || sourceCase.getLicenseScope().isBlank()) {
            throw new BizException(ErrorCode.LICENSE_SCOPE_MISSING);
        }
        LicenseScope licenseScope = LicenseScope.of(sourceCase.getLicenseScope());
        if (licenseScope == null || !licenseScope.allowsPublicRelease()) {
            throw new BizException(ErrorCode.FORBIDDEN, "当前 licenseScope 不允许公开发布");
        }

        PublicCaseEntity entity = new PublicCaseEntity()
                .setSourceCaseId(projectCaseId)
                .setWorkspaceId(sourceCase.getWorkspaceId())
                .setAuthorId(sourceCase.getAuthorId())
                .setRedactionStatus(sourceCase.getRedactionStatus())
                .setReviewId(sourceCase.getReviewId())
                .setStatus(PublicCaseStatus.VERIFIED)
                .setDeleted(false);
        try {
            publicCaseMapper.insertSelective(entity);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateSourceCaseViolation(ex)) {
                throw new BizException(ErrorCode.CASE_DUPLICATE_SOURCE);
            }
            throw ex;
        }

        auditPort.log(AuditPort.AuditEvent.builder()
                .requestId(traceId())
                .actorId(userId)
                .actorType("user")
                .action("public_case.publish")
                .objectType("public_case")
                .objectId(entity.getId())
                .result("success")
                .payload(java.util.Map.of(
                        "sourceCaseId", projectCaseId,
                        "licenseScope", sourceCase.getLicenseScope(),
                        "status", entity.getStatus().getCode()
                ))
                .tenantId(null)
                .build());

        return toDetailVO(entity);
    }

    @Override
    public PublicCaseDetailVO getDetail(Long userId, Long publicCaseId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (publicCaseId == null || publicCaseId <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "publicCaseId 不能为空且必须大于 0");
        }
        PublicCaseEntity entity = publicCaseMapper.selectActiveById(publicCaseId);
        if (entity == null) {
            throw new BizException(ErrorCode.PUBLIC_CASE_NOT_FOUND);
        }
        if (!canReadPublicCase(userId, entity)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该 Public Case");
        }
        return toDetailVO(entity);
    }

    @Override
    public List<PublicCaseDetailVO> listPublicCases(Long userId, Integer limit) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!rbacService.hasScope(userId, ScopeEnum.PUBLIC_READ.getCode())) {
            throw new BizException(ErrorCode.FORBIDDEN, "缺少 public:read 权限");
        }
        int normalizedLimit = normalizeLimit(limit);
        List<PublicCaseEntity> entities = publicCaseMapper.selectPubliclySearchable(normalizedLimit);
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDetailVO)
                .toList();
    }

    private boolean canReadPublicCase(Long userId, PublicCaseEntity entity) {
        if (userId.equals(entity.getAuthorId())) {
            return true;
        }
        if (!rbacService.hasScope(userId, ScopeEnum.PUBLIC_READ.getCode())) {
            return false;
        }
        return entity.getStatus() != null && entity.getStatus().isPubliclySearchable();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIST_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIST_LIMIT) {
            throw new BizException(ErrorCode.PARAM_INVALID, "limit 必须在 1 到 50 之间");
        }
        return limit;
    }

    private PublicCaseDetailVO toDetailVO(PublicCaseEntity entity) {
        return PublicCaseDetailVO.builder()
                .id(entity.getId())
                .sourceCaseId(entity.getSourceCaseId())
                .workspaceId(entity.getWorkspaceId())
                .authorId(entity.getAuthorId())
                .redactionStatus(entity.getRedactionStatus())
                .reviewId(entity.getReviewId())
                .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
                .gmtCreate(entity.getGmtCreate())
                .gmtModified(entity.getGmtModified())
                .build();
    }

    private String traceId() {
        String traceId = MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? "missing-trace-id" : traceId;
    }

    private boolean isDuplicateSourceCaseViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getRootCause();
        if (cause instanceof java.sql.SQLIntegrityConstraintViolationException sqlEx) {
            String message = sqlEx.getMessage() != null ? sqlEx.getMessage().toLowerCase() : "unknown";
            return message.contains("source_case") || message.contains("idx_source_case");
        }
        return false;
    }
}
