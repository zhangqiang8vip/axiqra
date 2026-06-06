package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工具模型排行榜快照表 axiqra_tool_model_leaderboard_snapshot
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_tool_model_leaderboard_snapshot")
public class ToolModelLeaderboardSnapshotEntity extends BaseEntity {

    private LocalDate windowStart;
    private LocalDate windowEnd;
    private String scopeType;
    @Nullable
    private Long scopeId;
    private String toolName;
    private String reportedModelName;
    @Nullable
    private String domain;
    @Nullable
    private String techStack;
    private Integer rank;
    private BigDecimal successRate7d;
    private Integer sampleSize;
    @Nullable
    private BigDecimal rankScore;
}
