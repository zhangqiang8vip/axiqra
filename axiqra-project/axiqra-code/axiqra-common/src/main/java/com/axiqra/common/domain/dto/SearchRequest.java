package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search 查询请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    @NotBlank(message = "query 不能为空")
    private String query;

    @Min(value = 1, message = "workspaceId 必须大于 0")
    private Long workspaceId;

    private String techStack;

    private String domain;

    @Min(value = 1, message = "limit 必须大于 0")
    @Max(value = 50, message = "limit 不能超过 50")
    @Default
    private Integer limit = 10;

    @Min(value = 0, message = "minVerificationLevel 不能小于 0")
    @Max(value = 5, message = "minVerificationLevel 不能大于 5")
    @Default
    private Integer minVerificationLevel = 0;

    @Default
    private Boolean includeCandidateSeed = Boolean.TRUE;
}
