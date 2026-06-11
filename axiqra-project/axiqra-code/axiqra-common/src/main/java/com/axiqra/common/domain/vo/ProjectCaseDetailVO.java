package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Project Case 详情视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCaseDetailVO {

    private Long id;
    private Long traceId;
    private Long workspaceId;
    private Long projectId;
    private Long authorId;
    private Long authorizationId;
    private String visibilityScope;
    private String licenseScope;
    private String redactionStatus;
    private String status;
    private Long reviewId;
    private Instant gmtCreate;
    private Instant gmtModified;
}
