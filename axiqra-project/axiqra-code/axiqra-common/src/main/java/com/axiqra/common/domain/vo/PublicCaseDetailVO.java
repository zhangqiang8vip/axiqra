package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Public Case 详情视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicCaseDetailVO {

    private Long id;
    private Long sourceCaseId;
    private Long workspaceId;
    private Long authorId;
    private String redactionStatus;
    private Long reviewId;
    private String status;
    private Instant gmtCreate;
    private Instant gmtModified;
}
