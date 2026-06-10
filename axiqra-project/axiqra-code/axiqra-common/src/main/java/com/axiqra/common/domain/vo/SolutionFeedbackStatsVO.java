package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solution 反馈统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolutionFeedbackStatsVO {

    private long workedCount;
    private long partialCount;
    private long failedCount;
    private long notApplicableCount;
    private long totalCount;
}
