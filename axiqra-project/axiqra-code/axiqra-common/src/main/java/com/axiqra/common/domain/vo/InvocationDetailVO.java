package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Invocation 详情 VO
 * 
 * 对应 D06 文档 §15 规定的调用记录字段
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvocationDetailVO {

    private Long id;

    private String requestId;

    private Long userId;

    private String targetType;

    private Long targetId;

    private Long workspaceId;

    private String invocationCode;

    private String toolType;

    private String queryHash;

    private Integer riskLevel;

    private Integer requiredConfirmation;

    private Integer confirmationObtained;

    private String resultType;

    private Instant gmtCreate;

    private Instant gmtModified;
    
    // ========== D06 §15 规定的补充字段 ==========
    
    /** 调用者类型: ai_tool / human / system */
    private String callerType;
    
    /** 调用状态: invoked / completed / failed / cancelled */
    private String invocationStatus;
    
    /** 任务目标 */
    private String taskGoal;
    
    /** 错误签名或报错信息 */
    private String errorSignature;
    
    /** 技术栈 */
    private String techStack;
    
    /** 运行环境 */
    private String environment;
    
    /** 上下文哈希 */
    private String contextHash;
    
    /** 适配分 */
    private Double fitScore;
    
    /** 返回结果数量 */
    private Integer returnedResultsCount;
    
    /** 已尝试过的路径 */
    private String priorAttempts;
    
    /** 问题类型 */
    private String problemType;
    
    /** 用户意图 */
    private String userIntent;
}
