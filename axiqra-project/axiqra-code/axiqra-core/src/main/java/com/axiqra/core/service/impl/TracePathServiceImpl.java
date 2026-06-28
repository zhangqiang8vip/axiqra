package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.TracePathDTO;
import com.axiqra.core.service.TracePathService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trace 路径服务实现
 *
 * <p>架构说明（S1.5 演进）：
 * <ul>
 *   <li>Trace Path 字段属于 EngineeringTrace 表，不属于 Invocation 表</li>
 *   <li>V9 迁移已删除 axiqra_invocation 上的 forward_path/decision_path/rollback_path/reverse_path/evolution_hint</li>
 *   <li>本服务为兼容旧调用方保留 API，但写入语义变为：
 *     <ol>
 *       <li>关联 invocation 找到对应的 trace（未来通过 trace_id 字段关联）</li>
 *       <li>写入 axiqra_engineering_trace 表</li>
 *     </ol>
 *   </li>
 *   <li>S1.5 之前先 stub 为 no-op，避免向已删除的字段写入失败</li>
 * </ul>
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TracePathServiceImpl implements TracePathService {

    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void recordTracePath(Long invocationId, TracePathDTO tracePath) {
        if (invocationId == null || tracePath == null) {
            log.debug("recordTracePath: invalid parameters invocationId={}, tracePath={}", invocationId, tracePath);
            return;
        }
        log.warn("[Stub] recordTracePath 不再写入 invocation 表（S1.5 计划：写入 trace）。invocationId={}", invocationId);
    }

    @Override
    @Transactional
    public void updateForwardPath(Long invocationId, TracePathDTO.ForwardPath forwardPath) {
        log.warn("[Stub] updateForwardPath no-op（S1.5 计划：写入 trace）。invocationId={}", invocationId);
    }

    @Override
    @Transactional
    public void updateDecisionPath(Long invocationId, TracePathDTO.DecisionStep decisionStep) {
        log.warn("[Stub] updateDecisionPath no-op（S1.5 计划：写入 trace）。invocationId={}", invocationId);
    }

    @Override
    @Transactional
    public void updateRollbackPath(Long invocationId, TracePathDTO.RollbackPath rollbackPath) {
        log.warn("[Stub] updateRollbackPath no-op（S1.5 计划：写入 trace）。invocationId={}", invocationId);
    }

    @Override
    @Transactional
    public void updateEvolutionHint(Long invocationId, String evolutionHint) {
        log.warn("[Stub] updateEvolutionHint no-op（S1.5 计划：写入 trace）。invocationId={}", invocationId);
    }

    @Override
    @Transactional
    public void updateReversePath(Long invocationId, TracePathDTO.ReversePath reversePath) {
        log.warn("[Stub] updateReversePath no-op（S1.5 计划：写入 trace）。invocationId={}", invocationId);
    }

    @Override
    public TracePathDTO getTracePath(Long invocationId) {
        if (invocationId == null) {
            return null;
        }
        log.debug("[Stub] getTracePath: S1.5 计划通过 trace_id 关联查询 trace 表。invocationId={}", invocationId);
        return null;
    }
}
