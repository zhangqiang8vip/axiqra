package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Invocation 调用结果上报请求
 * 
 * 对应 D06 文档 §15 规定的调用记录字段
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Data
@NoArgsConstructor
public class InvocationReportRequest {

    @NotBlank(message = "请求 ID 不能为空")
    @Size(max = 96, message = "请求 ID 长度不能超过 96")
    private String requestId;

    @NotBlank(message = "调用结果不能为空")
    private String resultType;

    @NotBlank(message = "工具类型不能为空")
    private String toolType;

    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    @NotNull(message = "目标 ID 不能为空")
    private Long targetId;

    @NotNull(message = "Workspace ID 不能为空")
    private Long workspaceId;

    private String invocationCode;

    private Integer riskLevel;

    private Integer requiredConfirmation;

    private Integer confirmationObtained;

    private String feedbackContent;

    private List<String> evidenceRefs;

    private String contextDelta;

    private String boundaryNotes;
    
    // ========== D06 §15 规定的补充字段 ==========
    
    /** 调用者类型: ai_tool / human / system */
    private String callerType;
    
    /** 任务目标 */
    private String taskGoal;
    
    /** 错误签名或报错信息 */
    private String errorSignature;
    
    /** 技术栈 */
    private String techStack;
    
    /** 运行环境 */
    private String environment;
    
    /** 上下文哈希 (用于去重和关联) */
    private String contextHash;
    
    /** 适配分 (搜索时计算) */
    private Double fitScore;
    
    /** 返回结果数量 */
    private Integer returnedResultsCount;
    
    /** 已尝试过的路径 (JSON 数组) */
    private String priorAttempts;
    
    /** 问题类型: build / deploy / performance / security / permission 等 */
    private String problemType;
    
    /** 用户意图: fix / learn / compare / validate / rollback */
    private String userIntent;
}
