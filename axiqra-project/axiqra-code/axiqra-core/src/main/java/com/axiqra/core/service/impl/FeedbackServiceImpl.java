package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.FeedbackSubmitRequest;
import com.axiqra.common.domain.entity.FeedbackEntity;
import com.axiqra.common.domain.entity.InvocationEntity;
import com.axiqra.common.domain.vo.FeedbackDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.FeedbackMapper;
import com.axiqra.core.mapper.InvocationMapper;
import com.axiqra.core.service.FeedbackService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Feedback Service 实现
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackMapper feedbackMapper;
    private final InvocationMapper invocationMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public FeedbackDetailVO submitFeedback(Long userId, FeedbackSubmitRequest request) {
        // 验证 Invocation 存在
        InvocationEntity invocation = invocationMapper.selectById(request.getInvocationId());
        if (invocation == null || invocation.isDeleted()) {
            throw new BizException(ErrorCode.INVOCATION_NOT_FOUND);
        }

        // 创建 Feedback
        FeedbackEntity entity = new FeedbackEntity();
        entity.setInvocationId(request.getInvocationId());
        entity.setUserId(userId);
        entity.setFeedbackType(request.getFeedbackType());
        entity.setFeedbackContent(request.getFeedbackContent());
        entity.setEvidenceRefs(serializeEvidenceRefs(request.getEvidenceRefs()));
        entity.setContextDelta(request.getContextDelta());
        entity.setBoundaryNotes(request.getBoundaryNotes());
        entity.setStatus("accepted");

        feedbackMapper.insert(entity);
        log.info("Feedback 提交成功: id={}, invocationId={}, type={}", entity.getId(), request.getInvocationId(), request.getFeedbackType());

        return toDetailVO(entity);
    }

    @Override
    public List<FeedbackDetailVO> listFeedbacks(Long userId, String targetType, Long targetId) {
        // 根据 targetType 查询
        if ("solution".equals(targetType)) {
            List<FeedbackEntity> entities = feedbackMapper.selectBySolutionId(targetId);
            return toDetailVOList(entities);
        }
        return List.of();
    }

    @Override
    public SolutionFeedbackStatsVO getSolutionFeedbackStats(Long solutionId) {
        var stats = feedbackMapper.selectFeedbackStatsBySolutionId(solutionId);

        SolutionFeedbackStatsVO.SolutionFeedbackStatsVOBuilder builder = SolutionFeedbackStatsVO.builder();
        long total = 0;

        for (var row : stats) {
            String type = row.getFeedbackType();
            long count = row.getCount() != null ? row.getCount() : 0;
            total += count;

            if ("worked".equals(type)) {
                builder.workedCount(count);
            } else if ("partial".equals(type)) {
                builder.partialCount(count);
            } else if ("failed".equals(type)) {
                builder.failedCount(count);
            } else if ("not_applicable".equals(type)) {
                builder.notApplicableCount(count);
            }
        }

        return builder.totalCount(total).build();
    }

    private List<String> deserializeEvidenceRefs(String json) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.warn("证据引用 JSON 解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    private String serializeEvidenceRefs(List<String> refs) {
        if (refs == null || refs.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(refs);
        } catch (JsonProcessingException e) {
            log.warn("证据引用 JSON 序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private FeedbackDetailVO toDetailVO(FeedbackEntity entity) {
        return FeedbackDetailVO.builder()
                .id(entity.getId())
                .invocationId(entity.getInvocationId())
                .userId(entity.getUserId())
                .feedbackType(entity.getFeedbackType())
                .feedbackTypeDesc(getFeedbackTypeDesc(entity.getFeedbackType()))
                .feedbackContent(entity.getFeedbackContent())
                .evidenceRefs(deserializeEvidenceRefs(entity.getEvidenceRefs()))
                .contextDelta(entity.getContextDelta())
                .boundaryNotes(entity.getBoundaryNotes())
                .status(entity.getStatus())
                .gmtCreate(entity.getGmtCreate())
                .gmtModified(entity.getGmtModified())
                .build();
    }

    private List<FeedbackDetailVO> toDetailVOList(List<FeedbackEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        List<FeedbackDetailVO> list = new ArrayList<>();
        for (FeedbackEntity entity : entities) {
            list.add(toDetailVO(entity));
        }
        return list;
    }

    private String getFeedbackTypeDesc(String type) {
        if (type == null) return "";
        return switch (type) {
            case "worked" -> "方案有效";
            case "partial" -> "部分有效";
            case "failed" -> "方案无效";
            case "not_applicable" -> "不适用";
            default -> type;
        };
    }
}
