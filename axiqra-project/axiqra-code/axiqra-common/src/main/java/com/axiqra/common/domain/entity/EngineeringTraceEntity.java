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
    @Nullable
    private String evidencePath;
    @Column("is_deleted")
    private boolean isDeleted = false;

    // ========== V10 新增字段：D06 §3 Engineering Trace Package 完整字段 ==========

    /** 正向路径 (JSON): 成功执行的主要步骤序列，对应 D06 §3 forward_path */
    @Nullable
    @Column("forward_path")
    private String forwardPathJson;

    /** 反向路径 (JSON): 从结果反向推导的过程，对应 D06 §3 reverse_path */
    @Nullable
    @Column("reverse_path")
    private String reversePathJson;

    /** 回滚路径 (JSON): 失败时如何恢复，对应 D06 §3 rollback_path */
    @Nullable
    @Column("rollback_path")
    private String rollbackPathJson;

    /** 决策路径 (JSON): 关键决策点和选择理由，对应 D06 §3 decision_path */
    @Nullable
    @Column("decision_path")
    private String decisionPathJson;

    /** 本次案例中尝试过但不成立的方向（单数），对应 D07 §10 */
    @Nullable
    @Column("attempted_failure_path")
    private String attemptedFailurePath;

    /** 演进提示 (TEXT): 方案的演进方向和优化建议，对应 D06 §3 evolution_hint */
    @Nullable
    @Column("evolution_hint")
    private String evolutionHint;

    /** 授权边界 FK → axiqra_authorization.id，用于可见范围决策（D13 §3） */
    @Nullable
    @Column("authorization_id")
    private Long authorizationId;

    /** AI 工具类型：cursor / claude_code / codex / human */
    @Nullable
    @Column("source_tool")
    private String sourceTool;

    /** 结构版本，用于向前兼容性（如 "1.0"） */
    @Nullable
    @Column("schema_version")
    private String schemaVersion = "1.0";
}