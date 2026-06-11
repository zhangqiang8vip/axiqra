package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trace 用户确认请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceConfirmRequest {

    @NotBlank(message = "userConfirmation 不能为空")
    private String userConfirmation;
}
