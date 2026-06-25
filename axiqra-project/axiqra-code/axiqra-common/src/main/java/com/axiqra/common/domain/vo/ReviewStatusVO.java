package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 审核状态 VO
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewStatusVO {

    /** Public Case ID */
    private Long publicCaseId;

    /** 审核 ID */
    private Long reviewId;

    /** 当前审核状态 */
    private String reviewStatus;

    /** 审核状态描述 */
    private String reviewStatusDesc;

    /** 审核类型 */
    private String reviewType;

    /** 审核结果 */
    private String reviewResult;

    /** 审核原因码 */
    private String reasonCode;

    /** 审核备注 */
    private String reviewNote;

    /** 审核人 ID */
    private Long reviewerId;

    /** 提交时间 */
    private Instant submittedAt;

    /** 审核完成时间 */
    private Instant completedAt;

    /** 预计审核时间（小时） */
    private Integer estimatedHours;
}
