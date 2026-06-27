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
    /**
     * 决策路径 (JSON): 关键决策点和选择理由
     */
    private String decisionPath;
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
    /**
     * 演进提示 (TEXT): 方案的演进方向和优化建议
     */
    private String evolutionHint;
    /**
     * 证据路径 - 证据文件引用列表 (JSON array)
     * 包含日志、diff、测试结果、截图等证据文件引用
     */
    private String evidencePath;
    private Instant gmtCreate;
    private Instant gmtModified;
    private List<TraceEvidenceVO> evidences;
}
