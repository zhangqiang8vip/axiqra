package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审核队列统计 VO
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewQueueStatsVO {

    /** 待审核数量 */
    private long pendingCount;

    /** 审核中数量 */
    private long inReviewCount;

    /** 已完成数量（今日） */
    private long completedTodayCount;

    /** 已通过数量（今日） */
    private long approvedTodayCount;

    /** 已拒绝数量（今日） */
    private long rejectedTodayCount;

    /** 平均审核时间（小时） */
    private double avgReviewTimeHours;

    /** 队列总数 */
    private long totalQueueSize;

    /** 统计生成时间 */
    private Long generatedAt;
}
