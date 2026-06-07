package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 创建工作空间请求 DTO
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceCreateRequest {

    @NotBlank(message = "workspaceType 不能为空")
    private String workspaceType;

    @NotBlank(message = "workspaceName 不能为空")
    @Size(max = 255, message = "workspaceName 最多 255 字符")
    private String workspaceName;
}
