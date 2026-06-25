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

    @JsonAlias("reverse_path")
    private String reversePath;

    @JsonAlias("decision_path")
    private String decisions;

    @JsonAlias("rollback_path")
    private String rollbackPath;

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

    @JsonAlias("idempotency_key")
    private String idempotencyKey;

    @JsonAlias("evidence_refs")
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
        this.decisions = stringifyJsonish(decisionPath);
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

        private String uri;

        private String hash;

        private String type;

        @JsonAlias("size_bytes")
        private Long sizeBytes;
    }
}
