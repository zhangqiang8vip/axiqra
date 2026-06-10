package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Candidate Seed 视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSeedVO {

    private Long id;
    private Long workspaceId;
    private Long authorId;
    private String queryHash;
    private String taskGoal;
    private String techStack;
    private String coverageGap;
    private String status;
    private Long assigneeId;
    private Long solutionId;
}
