package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 空间成员查询请求 DTO
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Getter
@Setter
@NoArgsConstructor
public class MemberQueryRequest {

    @NotNull(message = "workspaceId 不能为空")
    @Positive(message = "workspaceId 必须为正数")
    private Long workspaceId;

    private String role;
    private String status;
    private String keyword;

    @Min(value = 1, message = "page 必须 >= 1")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize 必须 >= 1")
    @Max(value = 1000, message = "pageSize 必须 <= 1000")
    private Integer pageSize = 20;
}
