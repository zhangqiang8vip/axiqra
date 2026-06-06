package com.axiqra.common.domain.entity;

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
    private String riskLevel;
    private String status;
    private String userConfirmation;
    @Nullable
    private String idempotencyKey;
    private String visibilityScope;
    private String indexStatus;
    @Nullable
    private Long reviewId;
    @Nullable
    private Long solutionId;
    @Nullable
    private String evolutionSuggestion;
    private Integer isDeleted;
}
