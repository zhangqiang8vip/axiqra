package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.InvocationReportRequest;
import com.axiqra.common.domain.dto.FeedbackSubmitRequest;
import com.axiqra.common.domain.entity.InvocationEntity;
import com.axiqra.common.domain.vo.InvocationDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.InvocationMapper;
import com.axiqra.core.service.FeedbackService;
import com.axiqra.core.service.InvocationService;
import com.axiqra.core.service.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Invocation Service 实现
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Slf4j
@Service
public class InvocationServiceImpl implements InvocationService {

    private final InvocationMapper invocationMapper;
    private final RateLimitService rateLimitService;
    private final ObjectProvider<FeedbackService> feedbackServiceProvider;

    public InvocationServiceImpl(InvocationMapper invocationMapper,
                                  RateLimitService rateLimitService,
                                  ObjectProvider<FeedbackService> feedbackServiceProvider) {
        this.invocationMapper = invocationMapper;
        this.rateLimitService = rateLimitService;
        this.feedbackServiceProvider = feedbackServiceProvider;
    }

    @Override
    @Transactional
    public InvocationDetailVO reportInvocation(Long userId, InvocationReportRequest request) {
        rateLimitService.checkOrThrow(userId, "invocation:report");

        InvocationEntity entity = new InvocationEntity();
        entity.setRequestId(request.getRequestId());
        entity.setUserId(userId);
        entity.setTargetType(request.getTargetType());
        entity.setTargetId(request.getTargetId());
        entity.setWorkspaceId(request.getWorkspaceId());
        entity.setInvocationCode(request.getInvocationCode());
        entity.setToolType(request.getToolType());
        entity.setRiskLevel(request.getRiskLevel() != null ? request.getRiskLevel() : 0);
        entity.setRequiredConfirmation(request.getRequiredConfirmation() != null ? request.getRequiredConfirmation() : 0);
        entity.setConfirmationObtained(request.getConfirmationObtained() != null ? request.getConfirmationObtained() : 0);
        entity.setResultType(request.getResultType());

        try {
            invocationMapper.insert(entity);
            log.info("Invocation 上报成功: id={}, requestId={}", entity.getId(), request.getRequestId());
        } catch (DataIntegrityViolationException ex) {
            log.info("Invocation 已存在，幂等返回: requestId={}", request.getRequestId());
            InvocationEntity existing = invocationMapper.selectByRequestId(request.getRequestId());
            if (existing != null) {
                return toDetailVO(existing);
            }
            log.error("Invocation 插入失败但查询不到记录: requestId={}", request.getRequestId());
            throw new IllegalStateException("Invocation 幂等处理异常：无法找到已存在的记录 requestId=" + request.getRequestId());
        }

        if (request.getFeedbackContent() != null || (request.getEvidenceRefs() != null && !request.getEvidenceRefs().isEmpty())) {
            try {
                FeedbackService feedbackService = feedbackServiceProvider.getIfAvailable();
                if (feedbackService != null) {
                    FeedbackSubmitRequest feedbackRequest = new FeedbackSubmitRequest();
                    feedbackRequest.setInvocationId(entity.getId());
                    feedbackRequest.setFeedbackType(request.getResultType());
                    feedbackRequest.setFeedbackContent(request.getFeedbackContent());
                    feedbackRequest.setEvidenceRefs(request.getEvidenceRefs());
                    feedbackRequest.setContextDelta(request.getContextDelta());
                    feedbackRequest.setBoundaryNotes(request.getBoundaryNotes());
                    feedbackService.submitFeedback(userId, feedbackRequest);
                }
            } catch (Exception feedbackEx) {
                log.warn("Feedback 联动失败，不阻断 Invocation 上报: invocationId={}, error={}",
                        entity.getId(), feedbackEx.getMessage());
            }
        }

        return toDetailVO(entity);
    }

    @Override
    public boolean isUserAuthorized(Long userId, Long invocationId) {
        if (userId == null || invocationId == null) {
            return false;
        }
        InvocationEntity entity = invocationMapper.selectById(invocationId);
        if (entity == null || entity.isDeleted()) {
            return false;
        }
        if (userId.equals(entity.getUserId())) {
            return true;
        }
        return false;
    }

    @Override
    public InvocationDetailVO getInvocationDetail(Long userId, Long invocationId) {
        InvocationEntity entity = invocationMapper.selectById(invocationId);
        if (entity == null || entity.isDeleted()) {
            throw new BizException(ErrorCode.INVOCATION_NOT_FOUND);
        }
        return toDetailVO(entity);
    }

    @Override
    public SolutionFeedbackStatsVO getSolutionFeedbackStats(Long solutionId) {
        FeedbackService feedbackService = feedbackServiceProvider.getIfAvailable();
        if (feedbackService == null) {
            log.warn("FeedbackService 未就绪，返回空统计: solutionId={}", solutionId);
            return SolutionFeedbackStatsVO.builder()
                    .totalCount(0L)
                    .workedCount(0L)
                    .partialCount(0L)
                    .failedCount(0L)
                    .notApplicableCount(0L)
                    .build();
        }
        return feedbackService.getSolutionFeedbackStats(solutionId);
    }

    private InvocationDetailVO toDetailVO(InvocationEntity entity) {
        return InvocationDetailVO.builder()
                .id(entity.getId())
                .requestId(entity.getRequestId())
                .userId(entity.getUserId())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .workspaceId(entity.getWorkspaceId())
                .invocationCode(entity.getInvocationCode())
                .toolType(entity.getToolType())
                .queryHash(entity.getQueryHash())
                .riskLevel(entity.getRiskLevel())
                .requiredConfirmation(entity.getRequiredConfirmation())
                .confirmationObtained(entity.getConfirmationObtained())
                .resultType(entity.getResultType())
                .gmtCreate(entity.getGmtCreate())
                .gmtModified(entity.getGmtModified())
                .build();
    }
}
