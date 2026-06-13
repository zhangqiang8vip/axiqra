package com.axiqra.core.service.impl;

import com.axiqra.common.domain.vo.ReviewDetailVO;
import com.axiqra.common.domain.enums.ReviewQueue;
import com.axiqra.common.domain.enums.ReviewResult;
import com.axiqra.common.domain.entity.ReviewEntity;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.ReviewMapper;
import com.axiqra.core.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Review Service 实现
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;

    @Override
    public List<ReviewDetailVO> getPendingReviews(Long userId, String queue, int limit) {
        List<ReviewEntity> entities = reviewMapper.selectPendingByQueue(queue, ReviewResult.PENDING.getCode(), limit);
        return toDetailVOList(entities);
    }

    @Override
    @Transactional
    public ReviewDetailVO approve(Long reviewerId, Long reviewId, String reasonCode, String notes) {
        ReviewEntity entity = loadAndValidateAndTransition(reviewerId, reviewId, ReviewResult.APPROVED, reasonCode, notes);
        log.info("审核通过: reviewId={}, reviewerId={}", reviewId, reviewerId);
        return toDetailVO(entity);
    }

    @Override
    @Transactional
    public ReviewDetailVO reject(Long reviewerId, Long reviewId, String reasonCode, String notes) {
        ReviewEntity entity = loadAndValidateAndTransition(reviewerId, reviewId, ReviewResult.REJECTED, reasonCode, notes);
        log.info("审核拒绝: reviewId={}, reviewerId={}, reasonCode={}", reviewId, reviewerId, reasonCode);
        return toDetailVO(entity);
    }

    @Override
    @Transactional
    public ReviewDetailVO quarantine(Long reviewerId, Long reviewId, String reasonCode, String notes) {
        ReviewEntity entity = loadAndValidateAndTransition(reviewerId, reviewId, ReviewResult.QUARANTINED, reasonCode, notes);
        log.info("内容隔离: reviewId={}, reviewerId={}", reviewId, reviewerId);
        return toDetailVO(entity);
    }

    @Override
    @Transactional
    public ReviewDetailVO appeal(Long userId, Long reviewId, String appealContent) {
        if (appealContent == null || appealContent.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "申诉内容不能为空");
        }
        ReviewEntity entity = reviewMapper.selectById(reviewId);
        if (entity == null || entity.isDeleted()) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "审核任务不存在");
        }
        if (entity.getStatus() != ReviewResult.REJECTED && entity.getStatus() != ReviewResult.QUARANTINED) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "只有被拒绝或隔离的内容可以申诉");
        }

        entity.setStatus(ReviewResult.APPEAL_IN_PROGRESS);
        entity.setAppealContent(appealContent);
        int rows = reviewMapper.update(entity);
        if (rows == 0) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "乐观锁冲突，申诉状态已变更");
        }

        log.info("提交申诉: reviewId={}, userId={}", reviewId, userId);
        return toDetailVO(entity);
    }

    @Override
    public ReviewDetailVO getReviewDetail(Long userId, Long reviewId) {
        ReviewEntity entity = reviewMapper.selectById(reviewId);
        if (entity == null || entity.isDeleted()) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "审核任务不存在");
        }
        return toDetailVO(entity);
    }

    private ReviewEntity loadAndValidateAndTransition(Long reviewerId, Long reviewId, ReviewResult targetStatus,
                                                      String reasonCode, String notes) {
        ReviewEntity entity = reviewMapper.selectById(reviewId);
        if (entity == null || entity.isDeleted()) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "审核任务不存在");
        }
        if (entity.getStatus().isFinal()) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "审核已完成，无法重复审核");
        }

        entity.setReviewerId(reviewerId);
        entity.setStatus(targetStatus);
        entity.setReasonCode(reasonCode);
        entity.setNotes(notes);
        int rows = reviewMapper.update(entity);
        if (rows == 0) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "乐观锁冲突，审核状态已变更");
        }
        return entity;
    }

    private ReviewDetailVO toDetailVO(ReviewEntity entity) {
        return ReviewDetailVO.builder()
                .id(entity.getId())
                .objectType(entity.getObjectType())
                .objectId(entity.getObjectId())
                .queue(entity.getQueue() != null ? entity.getQueue().getCode() : null)
                .queueDesc(entity.getQueue() != null ? entity.getQueue().getDesc() : null)
                .reviewerId(entity.getReviewerId())
                .riskLevel(entity.getRiskLevel() != null ? entity.getRiskLevel().getCode() : null)
                .riskLevelDesc(entity.getRiskLevel() != null ? entity.getRiskLevel().getDesc() : null)
                .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
                .statusDesc(entity.getStatus() != null ? entity.getStatus().getDesc() : null)
                .reasonCode(entity.getReasonCode())
                .notes(entity.getNotes())
                .appealContent(entity.getAppealContent())
                .version(entity.getVersion())
                .gmtCreate(entity.getGmtCreate())
                .gmtModified(entity.getGmtModified())
                .build();
    }

    private List<ReviewDetailVO> toDetailVOList(List<ReviewEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<ReviewDetailVO> list = new ArrayList<>();
        for (ReviewEntity entity : entities) {
            list.add(toDetailVO(entity));
        }
        return list;
    }
}
