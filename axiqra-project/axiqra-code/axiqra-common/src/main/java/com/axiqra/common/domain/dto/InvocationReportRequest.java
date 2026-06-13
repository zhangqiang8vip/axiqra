package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Invocation 调用结果上报请求
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Data
@NoArgsConstructor
public class InvocationReportRequest {

    @NotBlank(message = "请求 ID 不能为空")
    @Size(max = 96, message = "请求 ID 长度不能超过 96")
    private String requestId;

    @NotBlank(message = "调用结果不能为空")
    private String resultType;

    @NotBlank(message = "工具类型不能为空")
    private String toolType;

    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    @NotNull(message = "目标 ID 不能为空")
    private Long targetId;

    @NotNull(message = "Workspace ID 不能为空")
    private Long workspaceId;

    private String invocationCode;

    private Integer riskLevel;

    private Integer requiredConfirmation;

    private Integer confirmationObtained;

    private String feedbackContent;

    private List<String> evidenceRefs;

    private String contextDelta;

    private String boundaryNotes;
}
