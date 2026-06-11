package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Trace 详情视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceDetailVO {

    private Long id;
    private Long workspaceId;
    private Long projectId;
    private Long authorId;
    private String toolType;
    private String taskGoal;
    private String contextSnapshot;
    private String forwardSteps;
    private String reversePath;
    private String decisions;
    private String rollbackPath;
    private String outcome;
    private String riskLevel;
    private String status;
    private String userConfirmation;
    private String idempotencyKey;
    private String visibilityScope;
    private String indexStatus;
    private Long reviewId;
    private Long solutionId;
    private String evolutionSuggestion;
    private Instant gmtCreate;
    private Instant gmtModified;
    private List<TraceEvidenceVO> evidences;
}
