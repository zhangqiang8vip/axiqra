package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 审核队列项 VO
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewQueueItemVO {

    /** 队列项 ID */
    private String queueItemId;

    /** 内容类型 */
    private String contentType;

    /** 内容 ID */
    private Long contentId;

    /** 审核类型 */
    private String reviewType;

    /** 状态: pending, in_review, approved, rejected, cancelled */
    private String status;

    /** 优先级 */
    private String priority;

    /** 提交人 ID */
    private Long submitterId;

    /** 审核人 ID */
    private Long reviewerId;

    /** 审核结果 */
    private String reviewResult;

    /** 原因码 */
    private String reasonCode;

    /** 审核备注 */
    private String reviewNotes;

    /** AI 审核结果 JSON */
    private String aiReviewResult;

    /** 提交时间 */
    private Instant submittedAt;

    /** 开始审核时间 */
    private Instant startedAt;

    /** 完成审核时间 */
    private Instant completedAt;

    /** 创建时间 */
    private Instant createdAt;
}
