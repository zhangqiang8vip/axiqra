package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search 结果条目
 * 
 * 对应 D12 文档 §7/§14 规定的搜索结果结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultItemVO {

    private Long solutionId;
    private String solutionCode;
    private String title;
    private String summary;
    private String domain;
    private String techStack;
    private String verificationLevel;
    private String riskLevel;
    private String status;
    private String visibilityScope;
    private Long workspaceId;
    private Double score;
    private String scoreReason;
    
    // ========== D12 §7/§14 规定的补充字段 ==========
    
    /**
     * 结果类型: Solution / Public Case / Project Case / Candidate Seed
     * 对应 D12 §7 result_type
     */
    private String resultType;
    
    /**
     * 命中的关键词或错误签名
     * 对应 D12 §14 matched_terms
     */
    private String matchedTerms;
    
    /**
     * 为什么推荐这个结果
     * 对应 D12 §14 fit_reason
     */
    private String fitReason;
    
    /**
     * 谨慎原因
     * 对应 D12 §14 caution_reason
     */
    private String cautionReason;
    
    /**
     * 证据摘要
     * 对应 D12 §14 evidence_summary
     */
    private String evidenceSummary;
    
    /**
     * 为什么是当前验证等级的解释
     * 对应 D12 §14 verification_explanation
     */
    private String verificationExplanation;
    
    /**
     * 风险提示
     * 对应 D12 §14 risk_explanation
     */
    private String riskExplanation;
    
    /**
     * 来源空间: project / team / enterprise / public
     * 对应 D12 §14 space_source
     */
    private String spaceSource;
    
    /**
     * 建议下一步操作: execute / learn / confirm / create_seed
     * 对应 D12 §14 recommended_next_action
     */
    private String recommendedNextAction;
    
    /**
     * 样本量 (调用次数)
     * 对应 D12 §7 sample_count
     */
    private Integer sampleCount;
    
    /**
     * 成功率 (worked 比例)
     * 当 sampleCount < 5 时应显示 N/A
     */
    private Double successRate;
    
    /**
     * 失败路径列表
     * 对应 D12 §7 failure_paths
     */
    private String failurePaths;
}
