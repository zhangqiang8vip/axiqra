package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Search Before Act 查询请求
 * 
 * 对应 D12 文档 §3/§11 规定的搜索输入字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    @NotBlank(message = "query 不能为空")
    private String query;

    @Min(value = 1, message = "workspaceId 必须大于 0")
    private Long workspaceId;

    /** 技术栈、版本信息，对应 D12 §11 problem_type 解析来源 */
    private String techStack;

    private String domain;

    @Min(value = 1, message = "limit 必须大于 0")
    @Max(value = 50, message = "limit 不能超过 50")
    @Default
    private Integer limit = 10;

    @Min(value = 0, message = "minVerificationLevel 不能小于 0")
    @Max(value = 5, message = "minVerificationLevel 不能大于 5")
    @Default
    private Integer minVerificationLevel = 0;

    @Default
    private Boolean includeCandidateSeed = Boolean.TRUE;
    
    // ========== D12 §3/§11 规定的补充字段 ==========
    
    /**
     * 错误签名或报错信息，用于精确召回
     * 对应 D12 §3 error_signature、§11 error_signature
     */
    @Builder.Default
    private String errorSignature = null;
    
    /**
     * 运行环境信息 (OS、运行时、数据库、云环境等)
     * 对应 D12 §3 environment、§11 environment
     */
    @Builder.Default
    private String environment = null;
    
    /**
     * 当前项目上下文
     * 对应 D12 §3 project_context、§11 current_space
     */
    @Builder.Default
    private Long projectId = null;
    
    /**
     * 风险等级提示 (R0-R4)
     * 对应 D12 §3 risk_hint、§11 risk_hint
     */
    @Builder.Default
    private String riskHint = null;
    
    /**
     * AI 预期行为: fix / learn / compare / validate / rollback
     * 对应 D12 §11 intent
     */
    @Builder.Default
    private String expectedAction = null;
    
    /**
     * AI 已尝试过的路径，用于避免重复错误
     * 对应 D12 §11 prior_attempts
     */
    @Builder.Default
    private List<String> priorAttempts = null;
    
    /**
     * 问题类型分类: build / deploy / performance / security / permission 等
     * 对应 D12 §11 problem_type
     */
    @Builder.Default
    private String problemType = null;
}
