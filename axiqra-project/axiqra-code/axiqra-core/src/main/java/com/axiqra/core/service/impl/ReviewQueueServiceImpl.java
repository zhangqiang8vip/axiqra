package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.dto.ReviewQueueRequest;
import com.axiqra.common.domain.enums.ReasonCode;
import com.axiqra.common.domain.vo.ReviewQueueItemVO;
import com.axiqra.common.domain.vo.ReviewQueueStatsVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.AIReviewService;
import com.axiqra.core.service.ReviewQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 审核队列服务实现
 *
 * 注意：当前实现使用内存存储，生产环境应使用数据库持久化
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewQueueServiceImpl implements ReviewQueueService {

    private final AIReviewService aiReviewService;
    private final AuditPort auditPort;

    // 内存存储（生产环境应使用数据库）
    private final Map<String, ReviewQueueItemVO> queueItems = new ConcurrentHashMap<>();
    private long idCounter = System.currentTimeMillis();

    @Override
    public ReviewQueueItemVO enqueue(Long userId, ReviewQueueRequest request) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (request == null || request.getContentType() == null || request.getContentId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "内容类型和ID不能为空");
        }

        String queueItemId = generateQueueItemId();
        Instant now = Instant.now();

        // 执行 AI 预审
        String aiResult = null;
        try {
            var aiReport = aiReviewService.fullReview(request.getContentId(), request.getContentType());
            aiResult = "AI Review: " + aiReport.recommendation() + 
                    ", Overall Pass: " + aiReport.overallPass() +
                    ", Quality: " + aiReport.qualityScore().overallScore();
        } catch (Exception e) {
            log.warn("AI review failed: {}", e.getMessage());
        }

        ReviewQueueItemVO item = ReviewQueueItemVO.builder()
                .queueItemId(queueItemId)
                .contentType(request.getContentType())
                .contentId(request.getContentId())
                .reviewType(request.getReviewType() != null ? request.getReviewType() : "submit")
                .status("pending")
                .priority(request.getPriority() != null ? request.getPriority() : "normal")
                .submitterId(userId)
                .aiReviewResult(aiResult)
                .submittedAt(now)
                .createdAt(now)
                .build();

        queueItems.put(queueItemId, item);

        auditPort.log(AuditPort.AuditEvent.builder()
                .requestId(queueItemId)
                .actorId(userId)
                .actorType("user")
                .action("review_queue.enqueue")
                .objectType("review_queue_item")
                .result("success")
                .payload(Map.of(
                        "contentType", request.getContentType(),
                        "contentId", request.getContentId()
                ))
                .tenantId(null)
                .build());

        log.info("Review queue item created: {}", queueItemId);
        return item;
    }

    @Override
    public List<ReviewQueueItemVO> getQueue(String status, int limit) {
        return queueItems.values().stream()
                .filter(item -> status == null || status.isBlank() || status.equals(item.getStatus()))
                .sorted(Comparator.comparing(ReviewQueueItemVO::getSubmittedAt).reversed())
                .limit(limit > 0 ? limit : 50)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewQueueItemVO> getMyAssignments(Long reviewerId) {
        return queueItems.values().stream()
                .filter(item -> reviewerId.equals(item.getReviewerId()))
                .filter(item -> "in_review".equals(item.getStatus()))
                .sorted(Comparator.comparing(ReviewQueueItemVO::getStartedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public ReviewQueueItemVO claimTask(Long reviewerId, String queueItemId) {
        if (reviewerId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (queueItemId == null || queueItemId.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "queueItemId 不能为空");
        }

        ReviewQueueItemVO item = queueItems.get(queueItemId);
        if (item == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "审核队列项不存在");
        }
        if (!"pending".equals(item.getStatus())) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "只能认领待审核的项");
        }

        item.setReviewerId(reviewerId);
        item.setStatus("in_review");
        item.setStartedAt(Instant.now());

        log.info("Review task claimed: queueItemId={}, reviewerId={}", queueItemId, reviewerId);
        return item;
    }

    @Override
    public ReviewQueueItemVO review(Long reviewerId, String queueItemId, String result, String reasonCode, String notes) {
        if (reviewerId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (queueItemId == null || queueItemId.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "queueItemId 不能为空");
        }

        ReviewQueueItemVO item = queueItems.get(queueItemId);
        if (item == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "审核队列项不存在");
        }
        if (!reviewerId.equals(item.getReviewerId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能审核自己认领的任务");
        }
        if (!"in_review".equals(item.getStatus())) {
            throw new BizException(ErrorCode.STATUS_TRANSITION_INVALID, "只能在审核中状态执行审核");
        }

        // 验证结果
        if (result == null || (!"approved".equals(result) && !"rejected".equals(result))) {
            throw new BizException(ErrorCode.PARAM_INVALID, "审核结果必须是 approved 或 rejected");
        }

        item.setReviewResult(result);
        item.setReasonCode(reasonCode);
        item.setReviewNotes(notes);
        item.setStatus("approved".equals(result) ? "approved" : "rejected");
        item.setCompletedAt(Instant.now());

        auditPort.log(AuditPort.AuditEvent.builder()
                .requestId(queueItemId)
                .actorId(reviewerId)
                .actorType("user")
                .action("review_queue.review")
                .objectType("review_queue_item")
                .result(result)
                .payload(Map.of(
                        "contentType", item.getContentType(),
                        "contentId", item.getContentId(),
                        "reasonCode", reasonCode != null ? reasonCode : "",
                        "notes", notes != null ? notes : ""
                ))
                .tenantId(null)
                .build());

        log.info("Review completed: queueItemId={}, result={}", queueItemId, result);
        return item;
    }

    @Override
    public ReviewQueueStatsVO getQueueStats() {
        Instant today = Instant.now();
        long pendingCount = queueItems.values().stream().filter(item -> "pending".equals(item.getStatus())).count();
        long inReviewCount = queueItems.values().stream().filter(item -> "in_review".equals(item.getStatus())).count();
        long completedToday = queueItems.values().stream()
                .filter(item -> item.getCompletedAt() != null && item.getCompletedAt().isAfter(today))
                .count();
        long approvedToday = queueItems.values().stream()
                .filter(item -> "approved".equals(item.getStatus()))
                .filter(item -> item.getCompletedAt() != null && item.getCompletedAt().isAfter(today))
                .count();
        long rejectedToday = queueItems.values().stream()
                .filter(item -> "rejected".equals(item.getStatus()))
                .filter(item -> item.getCompletedAt() != null && item.getCompletedAt().isAfter(today))
                .count();

        // 计算平均审核时间
        double avgTime = queueItems.values().stream()
                .filter(item -> item.getCompletedAt() != null && item.getStartedAt() != null)
                .mapToLong(item -> item.getCompletedAt().toEpochMilli() - item.getStartedAt().toEpochMilli())
                .average()
                .orElse(0.0) / (1000 * 60 * 60); // 转换为小时

        return ReviewQueueStatsVO.builder()
                .pendingCount(pendingCount)
                .inReviewCount(inReviewCount)
                .completedTodayCount(completedToday)
                .approvedTodayCount(approvedToday)
                .rejectedTodayCount(rejectedToday)
                .avgReviewTimeHours(avgTime)
                .totalQueueSize(queueItems.size())
                .generatedAt(System.currentTimeMillis())
                .build();
    }

    @Override
    public ReviewQueueItemVO getQueueItem(String queueItemId) {
        if (queueItemId == null || queueItemId.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "queueItemId 不能为空");
        }
        return queueItems.get(queueItemId);
    }

    @Override
    public ReviewQueueItemVO rejectTask(Long reviewerId, String queueItemId, String reason) {
        return review(reviewerId, queueItemId, "rejected", "cancelled", reason);
    }

    private String generateQueueItemId() {
        return "RQ-" + (idCounter++);
    }
}
