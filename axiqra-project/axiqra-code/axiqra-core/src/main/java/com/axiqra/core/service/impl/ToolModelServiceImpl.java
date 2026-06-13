package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.ToolModelAttributionEntity;
import com.axiqra.common.domain.entity.ToolModelLeaderboardSnapshotEntity;
import com.axiqra.common.domain.vo.ToolModelLeaderboardVO;
import com.axiqra.core.mapper.ToolModelAttributionMapper;
import com.axiqra.core.mapper.ToolModelLeaderboardMapper;
import com.axiqra.core.service.ToolModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ToolModel Service 实现
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolModelServiceImpl implements ToolModelService {

    private final ToolModelLeaderboardMapper leaderboardMapper;
    private final ToolModelAttributionMapper attributionMapper;

    @Override
    public List<ToolModelLeaderboardVO> getLeaderboard(String scopeType, Long scopeId, String toolName, int limit) {
        LocalDate today = LocalDate.now();
        List<ToolModelLeaderboardSnapshotEntity> entities;

        if (toolName != null && !toolName.isEmpty()) {
            entities = leaderboardMapper.selectByToolName(today, scopeType, scopeId, toolName, limit);
        } else {
            entities = leaderboardMapper.selectLeaderboard(today, scopeType, scopeId, limit);
        }

        return toVOList(entities);
    }

    @Override
    @Transactional
    public void backfillAttribution(Long solutionId, String requestId, String toolName, String modelProvider,
                                    String modelName, String modelVersion, String confidence) {
        if (solutionId == null || requestId == null || requestId.isBlank()
                || toolName == null || toolName.isBlank()
                || modelProvider == null || modelProvider.isBlank()
                || modelName == null || modelName.isBlank()) {
            log.warn("归因回填参数不完整，跳过: solutionId={}, requestId={}, toolName={}, modelProvider={}, modelName={}",
                    solutionId, requestId, toolName, modelProvider, modelName);
            return;
        }

        try {
            ToolModelAttributionEntity entity = new ToolModelAttributionEntity();
            entity.setSolutionId(solutionId);
            entity.setRequestId(requestId);
            entity.setToolName(toolName);
            entity.setReportedModelProvider(modelProvider);
            entity.setReportedModelName(modelName);
            entity.setReportedModelVersion(modelVersion);
            entity.setReportedModelConfidence(confidence);
            entity.setAttributionSource("backfill");
            entity.setToolType("unknown");
            entity.setToolVersion("unknown");
            entity.setClientChannel("unknown");
            entity.setReportedModelSource("unknown");

            attributionMapper.insert(entity);
            log.info("归因回填成功: solutionId={}, toolName={}, modelName={}", solutionId, toolName, modelName);
        } catch (DataIntegrityViolationException ex) {
            log.info("Attribution 已存在（并发），跳过: requestId={}", requestId);
        }
    }

    private List<ToolModelLeaderboardVO> toVOList(List<ToolModelLeaderboardSnapshotEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        List<ToolModelLeaderboardVO> list = new ArrayList<>();
        for (ToolModelLeaderboardSnapshotEntity entity : entities) {
            list.add(ToolModelLeaderboardVO.builder()
                    .toolName(entity.getToolName())
                    .reportedModelName(entity.getReportedModelName())
                    .domain(entity.getDomain())
                    .techStack(entity.getTechStack())
                    .rank(entity.getRank())
                    .successRate7d(entity.getSuccessRate7d())
                    .sampleSize(entity.getSampleSize())
                    .rankScore(entity.getRankScore())
                    .windowStart(entity.getWindowStart())
                    .windowEnd(entity.getWindowEnd())
                    .build());
        }
        return list;
    }
}
