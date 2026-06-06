package com.axiqra.common.domain.entity;

import com.axiqra.common.domain.enums.RiskLevel;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工具模型日粒度表现统计表 axiqra_tool_model_performance_daily
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_tool_model_performance_daily")
public class ToolModelPerformanceDailyEntity extends BaseEntity {

    private LocalDate statDate;
    private Long solutionId;
    private String toolName;
    private String reportedModelName;
    @Nullable
    private String domain;
    @Nullable
    private String techStack;
    @Nullable
    private RiskLevel riskLevel;
    private Integer eligibleCount7d;
    private Integer workedCount7d;
    private Integer partialCount7d;
    private Integer failedCount7d;
    private Integer notApplicableCount7d;
    @Nullable
    private BigDecimal successRate7d;
}
