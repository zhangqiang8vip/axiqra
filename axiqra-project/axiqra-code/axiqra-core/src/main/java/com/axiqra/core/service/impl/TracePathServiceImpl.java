package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.TracePathDTO;
import com.axiqra.common.domain.entity.InvocationEntity;
import com.axiqra.core.mapper.InvocationMapper;
import com.axiqra.core.service.TracePathService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Trace 路径服务实现
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TracePathServiceImpl implements TracePathService {

    private final InvocationMapper invocationMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void recordTracePath(Long invocationId, TracePathDTO tracePath) {
        if (invocationId == null || tracePath == null) {
            log.warn("Invalid parameters: invocationId={}, tracePath={}", invocationId, tracePath);
            return;
        }

        InvocationEntity entity = invocationMapper.selectActiveById(invocationId);
        if (entity == null) {
            log.warn("Invocation not found: {}", invocationId);
            return;
        }

        try {
            // 序列化整个路径对象
            String pathJson = objectMapper.writeValueAsString(tracePath);

            if (tracePath.getForwardPath() != null) {
                entity.setForwardPath(objectMapper.writeValueAsString(tracePath.getForwardPath()));
            }
            if (tracePath.getDecisionPath() != null) {
                entity.setDecisionPath(objectMapper.writeValueAsString(tracePath.getDecisionPath()));
            }
            if (tracePath.getRollbackPath() != null) {
                entity.setRollbackPath(objectMapper.writeValueAsString(tracePath.getRollbackPath()));
            }
            if (tracePath.getEvolutionHint() != null) {
                entity.setEvolutionHint(tracePath.getEvolutionHint());
            }
            if (tracePath.getReversePath() != null) {
                entity.setReversePath(objectMapper.writeValueAsString(tracePath.getReversePath()));
            }

            invocationMapper.update(entity);
            log.info("Recorded trace path for invocation: {}", invocationId);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize trace path: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void updateForwardPath(Long invocationId, TracePathDTO.ForwardPath forwardPath) {
        if (invocationId == null) {
            return;
        }

        InvocationEntity entity = invocationMapper.selectActiveById(invocationId);
        if (entity == null) {
            log.warn("Invocation not found: {}", invocationId);
            return;
        }

        try {
            entity.setForwardPath(objectMapper.writeValueAsString(forwardPath));
            invocationMapper.update(entity);
            log.debug("Updated forward path for invocation: {}", invocationId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize forward path: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void updateDecisionPath(Long invocationId, TracePathDTO.DecisionStep decisionStep) {
        if (invocationId == null) {
            return;
        }

        InvocationEntity entity = invocationMapper.selectActiveById(invocationId);
        if (entity == null) {
            log.warn("Invocation not found: {}", invocationId);
            return;
        }

        try {
            // 追加到现有决策路径
            TracePathDTO.DecisionStep[] existingSteps = {};
            if (entity.getDecisionPath() != null) {
                existingSteps = objectMapper.readValue(
                        entity.getDecisionPath(),
                        TracePathDTO.DecisionStep[].class
                );
            }

            // 创建新的数组并添加新步骤
            TracePathDTO.DecisionStep[] newSteps = new TracePathDTO.DecisionStep[existingSteps.length + 1];
            System.arraycopy(existingSteps, 0, newSteps, 0, existingSteps.length);
            newSteps[existingSteps.length] = decisionStep;

            entity.setDecisionPath(objectMapper.writeValueAsString(newSteps));
            invocationMapper.update(entity);
            log.debug("Added decision step for invocation: {}", invocationId);
        } catch (JsonProcessingException e) {
            log.error("Failed to update decision path: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void updateRollbackPath(Long invocationId, TracePathDTO.RollbackPath rollbackPath) {
        if (invocationId == null) {
            return;
        }

        InvocationEntity entity = invocationMapper.selectActiveById(invocationId);
        if (entity == null) {
            log.warn("Invocation not found: {}", invocationId);
            return;
        }

        try {
            entity.setRollbackPath(objectMapper.writeValueAsString(rollbackPath));
            invocationMapper.update(entity);
            log.debug("Updated rollback path for invocation: {}", invocationId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize rollback path: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void updateEvolutionHint(Long invocationId, String evolutionHint) {
        if (invocationId == null) {
            return;
        }

        InvocationEntity entity = invocationMapper.selectActiveById(invocationId);
        if (entity == null) {
            log.warn("Invocation not found: {}", invocationId);
            return;
        }

        entity.setEvolutionHint(evolutionHint);
        invocationMapper.update(entity);
        log.debug("Updated evolution hint for invocation: {}", invocationId);
    }

    @Override
    @Transactional
    public void updateReversePath(Long invocationId, TracePathDTO.ReversePath reversePath) {
        if (invocationId == null) {
            return;
        }

        InvocationEntity entity = invocationMapper.selectActiveById(invocationId);
        if (entity == null) {
            log.warn("Invocation not found: {}", invocationId);
            return;
        }

        try {
            entity.setReversePath(objectMapper.writeValueAsString(reversePath));
            invocationMapper.update(entity);
            log.debug("Updated reverse path for invocation: {}", invocationId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize reverse path: {}", e.getMessage(), e);
        }
    }

    @Override
    public TracePathDTO getTracePath(Long invocationId) {
        if (invocationId == null) {
            return null;
        }

        InvocationEntity entity = invocationMapper.selectActiveById(invocationId);
        if (entity == null) {
            return null;
        }

        return TracePathDTO.builder()
                .decisionPath(arrayToList(deserializeDecisionPath(entity.getDecisionPath())))
                .rollbackPath(deserializeRollbackPath(entity.getRollbackPath()))
                .evolutionHint(entity.getEvolutionHint())
                .reversePath(deserializeReversePath(entity.getReversePath()))
                .build();
    }

    private TracePathDTO.ForwardPath[] deserializeForwardPath(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, TracePathDTO.ForwardPath[].class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize forward path: {}", e.getMessage());
            return null;
        }
    }

    private TracePathDTO.DecisionStep[] deserializeDecisionPath(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, TracePathDTO.DecisionStep[].class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize decision path: {}", e.getMessage());
            return null;
        }
    }

    private TracePathDTO.RollbackPath deserializeRollbackPath(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, TracePathDTO.RollbackPath.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize rollback path: {}", e.getMessage());
            return null;
        }
    }

    private TracePathDTO.ReversePath deserializeReversePath(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, TracePathDTO.ReversePath.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize reverse path: {}", e.getMessage());
            return null;
        }
    }

    private <T> List<T> arrayToList(T[] array) {
        if (array == null) {
            return null;
        }
        return List.of(array);
    }
}
