package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.ContributionLedgerEntity;
import com.axiqra.common.domain.vo.ContributionSummaryVO;
import com.axiqra.core.mapper.ContributionLedgerMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContributionServiceImpl 单元测试")
class ContributionServiceImplTest {

    @Mock
    private ContributionLedgerMapper contributionLedgerMapper;

    @InjectMocks
    private ContributionServiceImpl contributionService;

    @Test
    @DisplayName("getContributionSummary 应汇总用户贡献")
    void shouldReturnContributionSummary() {
        when(contributionLedgerMapper.sumPointsByActorId(1L)).thenReturn(25);
        when(contributionLedgerMapper.countByActorIdAndEventType(1L, "trace_created")).thenReturn(1);
        when(contributionLedgerMapper.countByActorIdAndEventType(1L, "solution_published")).thenReturn(0);
        when(contributionLedgerMapper.countByActorIdAndEventType(1L, "case_published")).thenReturn(0);
        when(contributionLedgerMapper.selectByActorId(1L, 10))
                .thenReturn(List.of(ledgerEntity(1L, "trace_created", 10)));

        ContributionSummaryVO result = contributionService.getContributionSummary(1L);

        assertNotNull(result);
        assertEquals(1L, result.getActorId());
        assertEquals(25, result.getTotalPoints());
        assertEquals(1, result.getTraceCount());
        assertEquals(0, result.getSolutionCount());
        assertEquals(0, result.getCaseCount());
    }

    @Test
    @DisplayName("getContributionSummary 无记录时返回零值")
    void shouldReturnZeroWhenNoRecords() {
        when(contributionLedgerMapper.sumPointsByActorId(1L)).thenReturn(null);
        when(contributionLedgerMapper.countByActorIdAndEventType(1L, "trace_created")).thenReturn(0);
        when(contributionLedgerMapper.countByActorIdAndEventType(1L, "solution_published")).thenReturn(0);
        when(contributionLedgerMapper.countByActorIdAndEventType(1L, "case_published")).thenReturn(0);
        when(contributionLedgerMapper.selectByActorId(1L, 10)).thenReturn(List.of());

        ContributionSummaryVO result = contributionService.getContributionSummary(1L);

        assertEquals(0, result.getTotalPoints());
        assertEquals(0, result.getTraceCount());
    }

    @Test
    @DisplayName("getContributionRecords 应返回分页记录")
    void shouldReturnContributionRecords() {
        ContributionLedgerEntity entity = ledgerEntity(5L, "solution_published", 50);
        when(contributionLedgerMapper.selectByActorId(1L, 20))
                .thenReturn(List.of(entity));

        List<ContributionSummaryVO.ContributionRecord> result =
                contributionService.getContributionRecords(1L, 20);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("solution_published", result.get(0).getEventType());
        assertEquals(50, result.get(0).getPoints());
    }

    @Test
    @DisplayName("recordContribution 应插入贡献记录")
    void shouldRecordContribution() {
        ArgumentCaptor<ContributionLedgerEntity> captor =
                ArgumentCaptor.forClass(ContributionLedgerEntity.class);

        contributionService.recordContribution(1L, "trace_created", "trace", 77L, 10, "ref-1");

        verify(contributionLedgerMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getActorId());
        assertEquals("trace_created", captor.getValue().getEventType());
        assertEquals(10, captor.getValue().getPoints());
        assertEquals("ref-1", captor.getValue().getEvidenceRefs());
        assertEquals("recorded", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("贡献类型统计应通过精确计数查询")
    void shouldCountEventTypesPrecisely() {
        when(contributionLedgerMapper.sumPointsByActorId(1L)).thenReturn(130);
        when(contributionLedgerMapper.countByActorIdAndEventType(1L, "trace_created")).thenReturn(2);
        when(contributionLedgerMapper.countByActorIdAndEventType(1L, "solution_published")).thenReturn(1);
        when(contributionLedgerMapper.countByActorIdAndEventType(1L, "case_published")).thenReturn(3);
        when(contributionLedgerMapper.selectByActorId(1L, 10)).thenReturn(List.of());

        ContributionSummaryVO result = contributionService.getContributionSummary(1L);

        assertEquals(2, result.getTraceCount());
        assertEquals(1, result.getSolutionCount());
        assertEquals(3, result.getCaseCount());
        assertEquals(130, result.getTotalPoints());
    }

    @Test
    @DisplayName("getContributionSummary actorId <= 0 应抛出 IllegalArgumentException")
    void shouldThrowWhenActorIdNonPositive() {
        assertThrows(IllegalArgumentException.class,
                () -> contributionService.getContributionSummary(0L));
        assertThrows(IllegalArgumentException.class,
                () -> contributionService.getContributionSummary(-1L));
    }

    @Test
    @DisplayName("getContributionRecords limit <= 0 应抛出 IllegalArgumentException")
    void shouldThrowWhenLimitNonPositive() {
        assertThrows(IllegalArgumentException.class,
                () -> contributionService.getContributionRecords(1L, 0));
    }

    private ContributionLedgerEntity ledgerEntity(Long id, String eventType, int points) {
        ContributionLedgerEntity entity = new ContributionLedgerEntity();
        entity.setId(id);
        entity.setActorId(1L);
        entity.setEventType(eventType);
        entity.setObjectType("trace");
        entity.setObjectId(77L);
        entity.setPoints(points);
        entity.setStatus("recorded");
        return entity;
    }
}
