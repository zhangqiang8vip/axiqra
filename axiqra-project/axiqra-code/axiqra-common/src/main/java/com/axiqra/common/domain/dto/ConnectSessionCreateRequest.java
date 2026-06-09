package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Connect 会话创建请求
 */
@Data
@NoArgsConstructor
public class ConnectSessionCreateRequest {

    @NotBlank(message = "channel 不能为空")
    private String channel;

    @NotBlank(message = "toolType 不能为空")
    private String toolType;

    @NotBlank(message = "targetType 不能为空")
    private String targetType;

    @NotNull(message = "targetId 不能为空")
    @Min(value = 1, message = "targetId 必须大于 0")
    private Long targetId;

    private Long workspaceId;

    private String riskLevel;

    private Boolean confirmationObtained;
}
