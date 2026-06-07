package com.axiqra.common.domain.entity;

import com.axiqra.common.domain.enums.IndexStatus;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.TraceStatus;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Engineering Trace Package 表 axiqra_engineering_trace
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_engineering_trace")
public class EngineeringTraceEntity extends BaseEntity {

    private Long workspaceId;
    @Nullable
    private Long projectId;
    private Long authorId;
    @Nullable
    private String toolType;
    private String taskGoal;
    @Nullable
    private String contextSnapshot;
    @Nullable
    private String forwardSteps;
    @Nullable
    private String reversePath;
    @Nullable
    private String decisions;
    @Nullable
    private String rollbackPath;
    private String outcome;
    private RiskLevel riskLevel;
    private TraceStatus status;
    private String userConfirmation;
    @Nullable
    private String idempotencyKey;
    private VisibilityScope visibilityScope;
    private IndexStatus indexStatus;
    @Nullable
    private Long reviewId;
    @Nullable
    private Long solutionId;
    @Nullable
    private String evolutionSuggestion;
    private Integer isDeleted;
}
