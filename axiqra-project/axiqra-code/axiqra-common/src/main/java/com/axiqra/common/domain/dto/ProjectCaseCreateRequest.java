package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Project Case 创建请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCaseCreateRequest {

    @NotNull(message = "traceId 不能为空")
    @Min(value = 1, message = "traceId 必须大于 0")
    private Long traceId;

    @NotBlank(message = "licenseScope 不能为空")
    private String licenseScope;

    @NotBlank(message = "redactionStatus 不能为空")
    private String redactionStatus;
}
