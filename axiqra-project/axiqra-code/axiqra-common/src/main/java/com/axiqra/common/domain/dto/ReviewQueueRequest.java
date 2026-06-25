package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审核队列请求 DTO
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewQueueRequest {

    /** 内容类型: solution, public_case, project_case */
    @NotBlank(message = "contentType 不能为空")
    private String contentType;

    /** 内容 ID */
    private Long contentId;

    /** 审核类型: submit, appeal, quarantine */
    private String reviewType;

    /** 优先级: high, normal, low */
    private String priority;

    /** 备注 */
    private String notes;
}
