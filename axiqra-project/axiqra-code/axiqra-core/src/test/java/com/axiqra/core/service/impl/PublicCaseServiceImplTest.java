package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.entity.ProjectCaseEntity;
import com.axiqra.common.domain.entity.PublicCaseEntity;
import com.axiqra.common.domain.enums.PublicCaseStatus;
import com.axiqra.common.domain.vo.PublicCaseDetailVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.ProjectCaseMapper;
import com.axiqra.core.mapper.PublicCaseMapper;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicCaseServiceImpl 单元测试")
class PublicCaseServiceImplTest {

    @Mock
    private PublicCaseMapper publicCaseMapper;

    @Mock
    private ProjectCaseMapper projectCaseMapper;

    @Mock
    private RbacService rbacService;

    @Mock
    private AuditPort auditPort;

    @InjectMocks
    private PublicCaseServiceImpl publicCaseService;

    @Test
    @DisplayName("满足公开发布条件时应创建 Public Case")
    void shouldPublishPublicCase() {
        ProjectCaseEntity sourceCase = projectCaseEntity();
        when(rbacService.hasScope(1L, "case:publish")).thenReturn(true);
        when(projectCaseMapper.selectActiveById(101L)).thenReturn(sourceCase);

        ArgumentCaptor<PublicCaseEntity> captor = ArgumentCaptor.forClass(PublicCaseEntity.class);
        org.mockito.Mockito.doAnswer(invocation -> {
            PublicCaseEntity entity = invocation.getArgument(0);
            entity.setId(301L);
            return 1;
        }).when(publicCaseMapper).insertSelective(captor.capture());

        PublicCaseDetailVO result = publicCaseService.publish(1L, 101L);

        assertNotNull(result);
        assertEquals(301L, result.getId());
        assertEquals("verified", result.getStatus());
        assertEquals(PublicCaseStatus.VERIFIED, captor.getValue().getStatus());
        verify(auditPort).log(any());
    }

    @Test
    @DisplayName("不允许公开的授权范围应拒绝发布")
    void shouldRejectPublishWhenLicenseScopeDisallowsPublicRelease() {
        ProjectCaseEntity sourceCase = projectCaseEntity();
        sourceCase.setLicenseScope("proprietary");
        when(rbacService.hasScope(1L, "case:publish")).thenReturn(true);
        when(projectCaseMapper.selectActiveById(101L)).thenReturn(sourceCase);

        BizException ex = assertThrows(BizException.class, () -> publicCaseService.publish(1L, 101L));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        verify(publicCaseMapper, never()).insertSelective(any());
    }

    @Test
    @DisplayName("重复来源 Project Case 应拒绝发布 — DB 唯一约束冲突")
    void shouldRejectDuplicateSourceCase() {
        ProjectCaseEntity sourceCase = projectCaseEntity();
        when(rbacService.hasScope(1L, "case:publish")).thenReturn(true);
        when(projectCaseMapper.selectActiveById(101L)).thenReturn(sourceCase);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException(
                        "duplicate key",
                        new SQLIntegrityConstraintViolationException("duplicate source_case_id on idx_source_case")))
                .when(publicCaseMapper).insertSelective(any());

        BizException ex = assertThrows(BizException.class, () -> publicCaseService.publish(1L, 101L));

        assertEquals(ErrorCode.CASE_DUPLICATE_SOURCE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("数据库唯一约束冲突时应正确翻译为业务异常")
    void shouldTranslateDuplicateKeyViolation() {
        ProjectCaseEntity sourceCase = projectCaseEntity();
        when(rbacService.hasScope(1L, "case:publish")).thenReturn(true);
        when(projectCaseMapper.selectActiveById(101L)).thenReturn(sourceCase);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException(
                        "duplicate key",
                        new SQLIntegrityConstraintViolationException("duplicate source_case_id on idx_source_case")))
                .when(publicCaseMapper).insertSelective(any());

        BizException ex = assertThrows(BizException.class, () -> publicCaseService.publish(1L, 101L));

        assertEquals(ErrorCode.CASE_DUPLICATE_SOURCE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("具备 public:read 时应可查看公开详情")
    void shouldGetPublicCaseDetail() {
        when(publicCaseMapper.selectActiveById(301L)).thenReturn(publicCaseEntity());
        when(rbacService.hasScope(2L, "public:read")).thenReturn(true);

        PublicCaseDetailVO result = publicCaseService.getDetail(2L, 301L);

        assertEquals(301L, result.getId());
        assertEquals("verified", result.getStatus());
    }

    @Test
    @DisplayName("缺少 public:read 时应拒绝查看公开详情")
    void shouldRejectDetailWithoutPublicRead() {
        when(publicCaseMapper.selectActiveById(301L)).thenReturn(publicCaseEntity());
        when(rbacService.hasScope(2L, "public:read")).thenReturn(false);

        BizException ex = assertThrows(BizException.class, () -> publicCaseService.getDetail(2L, 301L));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("公开列表应只返回公开可搜索状态")
    void shouldListPublicCases() {
        when(rbacService.hasScope(2L, "public:read")).thenReturn(true);
        when(publicCaseMapper.selectPubliclySearchable(5)).thenReturn(List.of(publicCaseEntity()));

        List<PublicCaseDetailVO> result = publicCaseService.listPublicCases(2L, 5);

        assertEquals(1, result.size());
        assertFalse(result.isEmpty());
        assertEquals("verified", result.get(0).getStatus());
    }

    private ProjectCaseEntity projectCaseEntity() {
        ProjectCaseEntity entity = new ProjectCaseEntity();
        entity.setId(101L);
        entity.setWorkspaceId(100L);
        entity.setAuthorId(1L);
        entity.setAuthorizationId(201L);
        entity.setLicenseScope("open_source");
        entity.setRedactionStatus("complete");
        entity.setStatus("pending_review");
        return entity;
    }

    private PublicCaseEntity publicCaseEntity() {
        PublicCaseEntity entity = new PublicCaseEntity();
        entity.setId(301L);
        entity.setSourceCaseId(101L);
        entity.setWorkspaceId(100L);
        entity.setAuthorId(1L);
        entity.setRedactionStatus("complete");
        entity.setStatus(PublicCaseStatus.VERIFIED);
        return entity;
    }
}
