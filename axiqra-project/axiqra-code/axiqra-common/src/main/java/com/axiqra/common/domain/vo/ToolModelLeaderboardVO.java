package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工具模型排行榜 VO
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolModelLeaderboardVO {

    private String toolName;

    private String reportedModelName;

    private String domain;

    private String techStack;

    private Integer rank;

    private BigDecimal successRate7d;

    private Integer sampleSize;

    private BigDecimal rankScore;

    private LocalDate windowStart;

    private LocalDate windowEnd;
}
