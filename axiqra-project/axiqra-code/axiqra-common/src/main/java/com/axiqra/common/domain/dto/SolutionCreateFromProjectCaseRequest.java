package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 从 Project Case 生成 MVP Solution 的请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolutionCreateFromProjectCaseRequest {

    @NotNull(message = "projectCaseId 不能为空")
    @Min(value = 1, message = "projectCaseId 必须大于 0")
    private Long projectCaseId;

    @NotBlank(message = "title 不能为空")
    @Size(max = 500, message = "title 最多 500 字符")
    private String title;

    @Size(max = 255, message = "domain 最多 255 字符")
    private String domain;

    @Size(max = 255, message = "techStack 最多 255 字符")
    private String techStack;

    private List<String> steps;

    private String applicableContext;

    private String nonApplicableContext;

    private List<String> evidenceRefs;

    private String risk;

    private String rollback;

    /**
     * private/workspace/public。MVP 默认 workspace，即个人空间内可见。
     */
    private String visibilityScope;
}
