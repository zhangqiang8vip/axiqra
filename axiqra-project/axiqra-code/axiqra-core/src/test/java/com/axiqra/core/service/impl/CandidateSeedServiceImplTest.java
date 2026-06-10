package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.SearchRequest;
import com.axiqra.common.domain.entity.CandidateSeedEntity;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.CandidateSeedMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateSeedServiceImpl 单元测试")
class CandidateSeedServiceImplTest {

    @Mock
    private CandidateSeedMapper candidateSeedMapper;

    @InjectMocks
    private CandidateSeedServiceImpl candidateSeedService;

    @Test
    @DisplayName("不存在时应创建新 candidate seed")
    void shouldCreateCandidateSeedWhenMissing() {
        SearchRequest request = SearchRequest.builder()
                .query("missing runbook")
                .workspaceId(100L)
                .domain("backend")
                .techStack("spring")
                .minVerificationLevel(2)
                .build();
        when(candidateSeedMapper.selectByQueryHashAndWorkspaceId(anyString(), any())).thenReturn(null);

        var result = candidateSeedService.createOrReuseCandidateSeed(1L, request);

        assertTrue(result.created());
        assertNotNull(result.seed());
        assertEquals("missing runbook", result.seed().getTaskGoal());
        assertEquals("spring", result.seed().getTechStack());
        assertTrue(result.seed().getCoverageGap().contains("domain=backend"));
        verify(candidateSeedMapper).insertSelective(any(CandidateSeedEntity.class));
    }

    @Test
    @DisplayName("已存在时应直接复用")
    void shouldReuseExistingCandidateSeed() {
        SearchRequest request = SearchRequest.builder()
                .query("missing runbook")
                .workspaceId(100L)
                .build();
        CandidateSeedEntity existing = new CandidateSeedEntity();
        existing.setId(99L);
        existing.setWorkspaceId(100L);
        existing.setTaskGoal("missing runbook");
        when(candidateSeedMapper.selectByQueryHashAndWorkspaceId(anyString(), any())).thenReturn(existing);

        var result = candidateSeedService.createOrReuseCandidateSeed(1L, request);

        assertFalse(result.created());
        assertEquals(99L, result.seed().getId());
    }

    @Test
    @DisplayName("重复插入异常时应回查并复用")
    void shouldReuseWhenDuplicateInsertOccurs() {
        SearchRequest request = SearchRequest.builder()
                .query("missing runbook")
                .workspaceId(100L)
                .build();
        CandidateSeedEntity existing = new CandidateSeedEntity();
        existing.setId(88L);
        existing.setWorkspaceId(100L);
        existing.setTaskGoal("missing runbook");
        when(candidateSeedMapper.selectByQueryHashAndWorkspaceId(anyString(), any()))
                .thenReturn(null)
                .thenReturn(existing);
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(candidateSeedMapper).insertSelective(any(CandidateSeedEntity.class));

        var result = candidateSeedService.createOrReuseCandidateSeed(1L, request);

        assertFalse(result.created());
        assertEquals(88L, result.seed().getId());
    }

    @Test
    @DisplayName("重复插入后仍查不到时应抛 DUPLICATE_ENTRY")
    void shouldThrowDuplicateEntryWhenReuseLookupStillMissing() {
        SearchRequest request = SearchRequest.builder()
                .query("missing runbook")
                .workspaceId(100L)
                .build();
        when(candidateSeedMapper.selectByQueryHashAndWorkspaceId(anyString(), any()))
                .thenReturn(null)
                .thenReturn(null);
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(candidateSeedMapper).insertSelective(any(CandidateSeedEntity.class));

        BizException ex = assertThrows(BizException.class,
                () -> candidateSeedService.createOrReuseCandidateSeed(1L, request));

        assertEquals(ErrorCode.DUPLICATE_ENTRY.getCode(), ex.getCode());
        assertEquals("候选种子在重试时已被删除或处于不一致状态", ex.getMessage());
    }
}
