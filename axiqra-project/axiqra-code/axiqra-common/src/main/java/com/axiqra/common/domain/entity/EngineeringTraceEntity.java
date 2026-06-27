package com.axiqra.common.domain.entity;

import com.axiqra.common.domain.enums.IndexStatus;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.TraceStatus;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.mybatisflex.annotation.Column;
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
    /**
     * 决策路径 (JSON): 关键决策点和选择理由
     * 对应 D06 §3 decision_path
     */
    @Nullable
    @Column("decision_path")
    private String decisionPath;
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
    /**
     * 演进提示 (TEXT): 方案的演进方向和优化建议
     * 对应 D06 §3 evolution_hint
     */
    @Nullable
    @Column("evolution_hint")
    private String evolutionHint;
    /**
     * 证据路径 - 证据文件引用列表 (JSON array)
     * 包含日志、diff、测试结果、截图等证据文件引用
     */
    @Nullable
    private String evidencePath;
    @Column("is_deleted")
    private boolean isDeleted = false;
}
