package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.ContributionLedgerEntity;
import com.axiqra.common.domain.vo.ContributionSummaryVO;
import com.axiqra.core.mapper.ContributionLedgerMapper;
import com.axiqra.core.service.ContributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Contribution Service 实现
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContributionServiceImpl implements ContributionService {

    private final ContributionLedgerMapper contributionLedgerMapper;

    @Override
    public ContributionSummaryVO getContributionSummary(Long actorId) {
        if (actorId == null || actorId <= 0) {
            throw new IllegalArgumentException("actorId must be positive");
        }
        Integer totalPoints = contributionLedgerMapper.sumPointsByActorId(actorId);

        // 按事件类型精确计数，不依赖记录条数上限
        int traceCount = contributionLedgerMapper.countByActorIdAndEventType(actorId, "trace_created");
        int solutionCount = contributionLedgerMapper.countByActorIdAndEventType(actorId, "solution_published");
        int caseCount = contributionLedgerMapper.countByActorIdAndEventType(actorId, "case_published");

        // recentRecords 仅用于展示最新动态，上限固定为 10
        List<ContributionLedgerEntity> records = contributionLedgerMapper.selectByActorId(actorId, 10);

        return ContributionSummaryVO.builder()
                .actorId(actorId)
                .totalPoints(totalPoints != null ? totalPoints : 0)
                .traceCount(traceCount)
                .solutionCount(solutionCount)
                .caseCount(caseCount)
                .recentRecords(toRecordList(records))
                .build();
    }

    @Override
    public List<ContributionSummaryVO.ContributionRecord> getContributionRecords(Long actorId, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        List<ContributionLedgerEntity> records = contributionLedgerMapper.selectByActorId(actorId, limit);
        return toRecordList(records);
    }

    @Override
    @Transactional
    public void recordContribution(Long actorId, String eventType, String objectType, Long objectId, int points, String evidenceRefs) {
        ContributionLedgerEntity entity = new ContributionLedgerEntity();
        entity.setActorId(actorId);
        entity.setEventType(eventType);
        entity.setObjectType(objectType);
        entity.setObjectId(objectId);
        entity.setPoints(points);
        entity.setEvidenceRefs(evidenceRefs);
        entity.setStatus("recorded");

        contributionLedgerMapper.insert(entity);
        log.info("贡献记录: actorId={}, eventType={}, points={}", actorId, eventType, points);
    }

    private List<ContributionSummaryVO.ContributionRecord> toRecordList(List<ContributionLedgerEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        List<ContributionSummaryVO.ContributionRecord> list = new ArrayList<>();
        for (ContributionLedgerEntity entity : entities) {
            list.add(ContributionSummaryVO.ContributionRecord.builder()
                    .id(entity.getId())
                    .eventType(entity.getEventType())
                    .objectType(entity.getObjectType())
                    .objectId(entity.getObjectId())
                    .points(entity.getPoints())
                    .status(entity.getStatus())
                    .evidenceRefs(entity.getEvidenceRefs())
                    .gmtCreate(entity.getGmtCreate())
                    .build());
        }
        return list;
    }
}
