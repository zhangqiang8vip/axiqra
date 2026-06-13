package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.ToolModelAttributionEntity;
import com.axiqra.common.domain.entity.ToolModelLeaderboardSnapshotEntity;
import com.axiqra.common.domain.vo.ToolModelLeaderboardVO;
import com.axiqra.core.mapper.ToolModelAttributionMapper;
import com.axiqra.core.mapper.ToolModelLeaderboardMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolModelServiceImpl 单元测试")
class ToolModelServiceImplTest {

    @Mock
    private ToolModelLeaderboardMapper leaderboardMapper;

    @Mock
    private ToolModelAttributionMapper attributionMapper;

    @InjectMocks
    private ToolModelServiceImpl toolModelService;

    @Test
    @DisplayName("getLeaderboard 无 toolName 应返回排行榜")
    void shouldReturnLeaderboard() {
        ToolModelLeaderboardSnapshotEntity entity = leaderboardEntity("cursor", "gpt-4o",
                new BigDecimal("95.5"), new BigDecimal("95.0"), 1000, 1);
        when(leaderboardMapper.selectLeaderboard(any(LocalDate.class), eq("public"), eq(null), eq(50)))
                .thenReturn(List.of(entity));

        List<ToolModelLeaderboardVO> result = toolModelService.getLeaderboard("public", null, null, 50);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("cursor", result.get(0).getToolName());
        assertEquals("gpt-4o", result.get(0).getReportedModelName());
        assertEquals(new BigDecimal("95.5"), result.get(0).getSuccessRate7d());
    }

    @Test
    @DisplayName("getLeaderboard 带 toolName 应传入 limit 参数")
    void shouldPassLimitWhenToolNameProvided() {
        ToolModelLeaderboardSnapshotEntity entity = leaderboardEntity("cursor", "gpt-4o",
                new BigDecimal("95.5"), new BigDecimal("95.0"), 1000, 1);
        when(leaderboardMapper.selectByToolName(any(LocalDate.class), eq("public"), eq(null), eq("cursor"), eq(50)))
                .thenReturn(List.of(entity));

        List<ToolModelLeaderboardVO> result =
                toolModelService.getLeaderboard("public", null, "cursor", 50);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("cursor", result.get(0).getToolName());
    }

    @Test
    @DisplayName("backfillAttribution 并发重复时应吞掉 DataIntegrityViolationException")
    void shouldIgnoreDuplicateKeyViolation() {
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(attributionMapper).insert(any(ToolModelAttributionEntity.class));

        // 不应抛异常
        toolModelService.backfillAttribution(77L, "req-1", "cursor", "openai", "gpt-4o", "v1", "0.9");

        verify(attributionMapper).insert(any(ToolModelAttributionEntity.class));
    }

    @Test
    @DisplayName("backfillAttribution 正常时应创建记录")
    void shouldCreateAttributionOnBackfill() {
        ArgumentCaptor<ToolModelAttributionEntity> captor =
                ArgumentCaptor.forClass(ToolModelAttributionEntity.class);

        toolModelService.backfillAttribution(77L, "req-new", "cursor", "openai", "gpt-4o", "v1", "0.9");

        verify(attributionMapper).insert(captor.capture());
        assertEquals(77L, captor.getValue().getSolutionId());
        assertEquals("req-new", captor.getValue().getRequestId());
        assertEquals("cursor", captor.getValue().getToolName());
        assertEquals("openai", captor.getValue().getReportedModelProvider());
        assertEquals("gpt-4o", captor.getValue().getReportedModelName());
        assertEquals("v1", captor.getValue().getReportedModelVersion());
        assertEquals("0.9", captor.getValue().getReportedModelConfidence());
        assertEquals("backfill", captor.getValue().getAttributionSource());
    }

    @Test
    @DisplayName("getLeaderboard 空结果应返回空列表")
    void shouldReturnEmptyLeaderboard() {
        when(leaderboardMapper.selectLeaderboard(any(LocalDate.class), eq("public"), eq(null), eq(50)))
                .thenReturn(List.of());

        List<ToolModelLeaderboardVO> result =
                toolModelService.getLeaderboard("public", null, null, 50);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getLeaderboard null 输入应返回空列表而非抛异常")
    void shouldReturnEmptyListForNullInput() {
        List<ToolModelLeaderboardVO> result = toolModelService.getLeaderboard("public", null, null, 50);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private ToolModelLeaderboardSnapshotEntity leaderboardEntity(
            String toolName, String modelName, BigDecimal successRate,
            BigDecimal rankScore, Integer sampleSize, Integer rank) {
        ToolModelLeaderboardSnapshotEntity entity = new ToolModelLeaderboardSnapshotEntity();
        entity.setToolName(toolName);
        entity.setReportedModelName(modelName);
        entity.setSuccessRate7d(successRate);
        entity.setSampleSize(sampleSize);
        entity.setRank(rank);
        entity.setRankScore(rankScore);
        entity.setDomain("coding");
        entity.setTechStack("java");
        entity.setWindowStart(LocalDate.now().minusDays(7));
        entity.setWindowEnd(LocalDate.now());
        return entity;
    }
}
