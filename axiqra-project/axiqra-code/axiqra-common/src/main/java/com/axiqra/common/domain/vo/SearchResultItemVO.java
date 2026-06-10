package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search 结果条目
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
}
