package com.axiqra.core.mapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solution 反馈统计聚合行
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackStatRow {

    private String feedbackType;

    private Long count;
}
