package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Invocation 调用记录表 axiqra_invocation
 * 
 * 对应 D06 文档 §15 规定的调用记录字段
 * 
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_invocation")
public class InvocationEntity extends BaseEntity {

    private String requestId;
    private Long userId;
    private String targetType;
    private Long targetId;
    private Long workspaceId;
    private String invocationCode;
    private String toolType;
    @Nullable
    private String queryHash;
    private Integer riskLevel;
    private Integer requiredConfirmation;
    private Integer confirmationObtained;
    @Nullable
    private String resultType;
    @Nullable
    @Column("tenant_id")
    private Long tenantId;
    @Column("is_deleted")
    private boolean isDeleted = false;
    
    // ========== D06 §15 规定的补充字段 ==========
    
    /** 调用者类型: ai_tool / human / system */
    @Nullable
    @Column("caller_type")
    private String callerType;
    
    /** 调用状态: invoked / completed / failed / cancelled */
    @Nullable
    @Column("invocation_status")
    private String invocationStatus;
    
    /** 任务目标 */
    @Nullable
    @Column("task_goal")
    private String taskGoal;
    
    /** 错误签名或报错信息 */
    @Nullable
    @Column("error_signature")
    private String errorSignature;
    
    /** 技术栈 */
    @Nullable
    @Column("tech_stack")
    private String techStack;
    
    /** 运行环境 */
    @Nullable
    @Column("environment")
    private String environment;
    
    /** 上下文哈希 (用于去重和关联) */
    @Nullable
    @Column("context_hash")
    private String contextHash;
    
    /** 适配分 (搜索时计算) */
    @Nullable
    @Column("fit_score")
    private Double fitScore;
    
    /** 返回结果数量 */
    @Nullable
    @Column("returned_results_count")
    private Integer returnedResultsCount;
    
    /** 已尝试过的路径 (JSON 数组) */
    @Nullable
    @Column("prior_attempts")
    private String priorAttempts;
    
    /** 问题类型: build / deploy / performance / security / permission 等 */
    @Nullable
    @Column("problem_type")
    private String problemType;
    
    /** 用户意图: fix / learn / compare / validate / rollback */
    @Nullable
    @Column("user_intent")
    private String userIntent;

    // ========== 工具模型归因字段 (D06 §15 / D12) ==========

    /** AI Agent 名称: cursor / claude-code / codex / codex-cli / windsurf / copilot / mimo / opencode 等 */
    @Nullable
    @Column("tool_name")
    private String toolName;

    /** AI Agent 提供商: Cursor / Anthropic / Microsoft / Windsurf 等 */
    @Nullable
    @Column("tool_vendor")
    private String toolVendor;

    /** AI Agent 版本 */
    @Nullable
    @Column("tool_version")
    private String toolVersion;

    /** 接入渠道: mcp / cli / api / sdk */
    @Nullable
    @Column("client_channel")
    private String clientChannel;

    /** 模型提供商: openai / anthropic / google / ollama / cohere / azure */
    @Nullable
    @Column("model_provider")
    private String modelProvider;

    /** 模型名称 */
    @Nullable
    @Column("model_name")
    private String modelName;

    /** 模型版本 */
    @Nullable
    @Column("model_version")
    private String modelVersion;

    /** 模型来源: auto_detect / user_reported / fallback */
    @Nullable
    @Column("model_source")
    private String modelSource;

    /** 模型置信度 */
    @Nullable
    @Column("model_confidence")
    private String modelConfidence;

    // ========== Engineering Trace 扩展字段 ==========

    /** 正向路径 (JSON): 成功解决问题的步骤路径 */
    @Nullable
    @Column("forward_path")
    private String forwardPath;

    /** 决策路径 (JSON): 关键决策点和选择理由 */
    @Nullable
    @Column("decision_path")
    private String decisionPath;

    /** 回滚路径 (JSON): 失败时的回滚步骤 */
    @Nullable
    @Column("rollback_path")
    private String rollbackPath;

    /** 演化提示 (TEXT): 方案的演进方向和优化建议 */
    @Nullable
    @Column("evolution_hint")
    private String evolutionHint;

    /** 反向路径 (JSON): 从结果反向推导的过程 */
    @Nullable
    @Column("reverse_path")
    private String reversePath;
}
