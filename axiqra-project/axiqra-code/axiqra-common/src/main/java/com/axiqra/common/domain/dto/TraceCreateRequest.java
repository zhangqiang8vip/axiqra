package com.axiqra.common.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    @JsonAlias("workspace_id")
    private Long workspaceId;

    @Min(value = 1, message = "projectId 必须大于 0")
    @JsonAlias("project_id")
    private Long projectId;

    @NotBlank(message = "taskGoal 不能为空")
    @JsonAlias({"task_goal", "goal"})
    private String taskGoal;

    @JsonAlias({"tool_type", "tool"})
    private String toolType;

    @JsonAlias({"context_snapshot", "context"})
    private String contextSnapshot;

    @JsonAlias({"forward_steps", "forward_path"})
    private String forwardSteps;

    /** 正向路径 (JSON): V10 新字段，V5 的 forward_path 替代物（D06 §3） */
    @JsonAlias("forward_path")
    private String forwardPath;

    @JsonAlias("reverse_path")
    private String reversePath;

    /** 回滚路径 (JSON): V10 新字段 */
    @JsonAlias("rollback_path")
    private String rollbackPath;

    @JsonAlias("decision_path")
    private String decisions;

    /**
     * 决策路径 (JSON): 关键决策点和选择理由
     */
    @JsonAlias("decision_path")
    private String decisionPath;

    /** 本次案例中尝试过但不成立的方向 (D07 §10) */
    @JsonAlias("attempted_failure_path")
    private String attemptedFailurePath;

    /**
     * 演进提示 (TEXT): 方案的演进方向和优化建议
     */
    @JsonAlias("evolution_hint")
    private String evolutionHint;

    @NotBlank(message = "outcome 不能为空")
    private String outcome;

    @NotBlank(message = "riskLevel 不能为空")
    @JsonAlias("risk_level")
    private String riskLevel;

    @Default
    @JsonAlias("visibility_scope")
    private String visibilityScope = "workspace";

    @JsonAlias("solution_id")
    private Long solutionId;

    @JsonAlias("evolution_suggestion")
    private String evolutionSuggestion;

    /** V10: 授权边界 FK → axiqra_authorization.id */
    @JsonAlias("authorization_id")
    private Long authorizationId;

    /** V10: AI 工具类型 */
    @JsonAlias("source_tool")
    private String sourceTool;

    @JsonAlias("idempotency_key")
    private String idempotencyKey;

    @NotNull(message = "evidences 不能为空")
    @Valid
    private List<TraceEvidenceItem> evidences;

    @JsonSetter("evidence_refs")
    public void setEvidenceRefs(List<?> evidenceRefs) {
        if (evidenceRefs == null) {
            this.evidences = null;
            return;
        }
        List<TraceEvidenceItem> normalized = new ArrayList<>();
        for (Object ref : evidenceRefs) {
            TraceEvidenceItem evidence = normalizeEvidence(ref);
            if (evidence != null) {
                normalized.add(evidence);
            }
        }
        this.evidences = normalized;
    }

    @JsonSetter("forward_path")
    public void setForwardPath(Object forwardPath) {
        this.forwardSteps = stringifyJsonish(forwardPath);
    }

    @JsonSetter("decision_path")
    public void setDecisionPath(Object decisionPath) {
        this.decisionPath = stringifyJsonish(decisionPath);
    }

    private TraceEvidenceItem normalizeEvidence(Object ref) {
        if (ref == null) {
            return null;
        }
        if (ref instanceof TraceEvidenceItem item) {
            return item;
        }
        if (ref instanceof Map<?, ?> map) {
            Object uri = firstPresent(map, "uri", "url", "path", "ref");
            Object hash = map.get("hash");
            Object type = firstPresent(map, "type", "kind");
            Object sizeBytes = firstPresent(map, "sizeBytes", "size_bytes");
            Long parsedSizeBytes = parseLong(sizeBytes);
            return TraceEvidenceItem.builder()
                    .uri(Objects.toString(uri, null))
                    .hash(Objects.toString(hash, null))
                    .type(Objects.toString(type, "file"))
                    .sizeBytes(parsedSizeBytes == null ? 0L : parsedSizeBytes)
                    .build();
        }
        return TraceEvidenceItem.builder()
                .uri(Objects.toString(ref, null))
                .type("file")
                .sizeBytes(0L)
                .build();
    }

    private Object firstPresent(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String stringifyJsonish(Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TraceEvidenceItem {

        @NotBlank(message = "证据 URI 不能为空")
        private String uri;

        private String hash;

        private String type;

        @NotNull(message = "文件大小不能为空")
        @Min(value = 0, message = "文件大小不能为负数")
        @JsonAlias("size_bytes")
        private Long sizeBytes;
    }
}
