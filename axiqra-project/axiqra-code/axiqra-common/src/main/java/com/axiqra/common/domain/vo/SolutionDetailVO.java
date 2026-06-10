package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Solution 详情视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolutionDetailVO {

    private Long id;
    private String solutionCode;
    private String title;
    private String domain;
    private String techStack;
    private Long authorId;
    private Long workspaceId;
    private Long projectId;
    private String verificationLevel;
    private String riskLevel;
    private String status;
    private String visibilityScope;
    private String licenseScope;
    private Long sourceCaseId;
    private Instant gmtCreate;
    private Instant gmtModified;
    private SolutionVersionVO activeVersion;
    private List<SolutionVersionVO> versions;
    private SolutionFeedbackStatsVO feedbackStats;
}
