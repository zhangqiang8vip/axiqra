package com.axiqra.common.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Trace 草稿提交请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceCreateRequest {

    @NotNull(message = "workspaceId 不能为空")
    @Min(value = 1, message = "workspaceId 必须大于 0")
    private Long workspaceId;

    @Min(value = 1, message = "projectId 必须大于 0")
    private Long projectId;

    @NotBlank(message = "taskGoal 不能为空")
    private String taskGoal;

    private String toolType;

    private String contextSnapshot;

    private String forwardSteps;

    private String reversePath;

    private String decisions;

    private String rollbackPath;

    @NotBlank(message = "outcome 不能为空")
    private String outcome;

    @NotBlank(message = "riskLevel 不能为空")
    private String riskLevel;

    @Default
    private String visibilityScope = "workspace";

    private Long solutionId;

    private String evolutionSuggestion;

    private String idempotencyKey;

    @Valid
    @NotEmpty(message = "evidences 不能为空")
    private List<TraceEvidenceItem> evidences;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TraceEvidenceItem {

        @NotBlank(message = "evidence uri 不能为空")
        private String uri;

        private String hash;

        @NotBlank(message = "evidence type 不能为空")
        private String type;

        @NotNull(message = "sizeBytes 不能为空")
        @Min(value = 0, message = "sizeBytes 不能小于 0")
        private Long sizeBytes;
    }
}
