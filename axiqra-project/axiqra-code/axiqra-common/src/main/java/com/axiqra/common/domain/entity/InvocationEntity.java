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
}
