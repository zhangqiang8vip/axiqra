package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Connect 会话创建请求
 * 
 * 对应 D09 文档第 9 节规定的接入会话必须记录的字段
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
    
    /**
     * 工具能力声明 (supports_mcp, supports_cli, supports_local_cache 等)
     * 对应 D09 文档第 13 节工具能力 Manifest
     */
    private String toolCapability;
    
    /**
     * 授权范围 (search, read, submit, feedback 等)
     */
    private String authScope;
}
