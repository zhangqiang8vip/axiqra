package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 更新工作空间请求 DTO
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceUpdateRequest {

    @Size(max = 255, message = "workspaceName 最多 255 字符")
    private String workspaceName;

    private String workspaceType;
}
