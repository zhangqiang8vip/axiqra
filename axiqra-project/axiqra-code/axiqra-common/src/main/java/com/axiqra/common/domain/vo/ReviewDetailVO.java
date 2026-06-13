package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Review 详情 VO
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDetailVO {

    private Long id;

    private String objectType;

    private Long objectId;

    private String queue;

    private String queueDesc;

    private Long reviewerId;

    private String riskLevel;

    private String riskLevelDesc;

    private String status;

    private String statusDesc;

    private String reasonCode;

    private String notes;

    private String appealContent;

    private Long version;

    private Instant gmtCreate;

    private Instant gmtModified;
}
