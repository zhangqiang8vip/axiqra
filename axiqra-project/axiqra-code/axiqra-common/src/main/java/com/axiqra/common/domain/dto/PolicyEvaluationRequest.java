package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 策略评估请求 DTO（ABAC 策略引擎入参）
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Getter
@Setter
@NoArgsConstructor
public class PolicyEvaluationRequest {

    /** 操作主体 ID（userId） */
    @NotNull(message = "subjectId 不能为空")
    @Positive(message = "subjectId 必须为正数")
    private Long subjectId;

    /** 操作主体类型 */
    @NotBlank(message = "subjectType 不能为空")
    private String subjectType = "user";

    /** 资源类型（如 solution, trace, workspace） */
    @NotBlank(message = "objectType 不能为空")
    private String objectType;

    /** 资源 ID（可为 null，表示任意资源） */
    private Long objectId;

    /** 操作（如 read, write, delete, publish） */
    @NotBlank(message = "action 不能为空")
    private String action;

    /** 额外上下文（结构化 KV，如 riskLevel, workspaceId 等） */
    private Map<String, Object> context;
}
