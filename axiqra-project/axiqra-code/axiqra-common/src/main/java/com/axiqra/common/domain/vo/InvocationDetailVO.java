package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Invocation 详情 VO
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvocationDetailVO {

    private Long id;

    private String requestId;

    private Long userId;

    private String targetType;

    private Long targetId;

    private Long workspaceId;

    private String invocationCode;

    private String toolType;

    private String queryHash;

    private Integer riskLevel;

    private Integer requiredConfirmation;

    private Integer confirmationObtained;

    private String resultType;

    private Instant gmtCreate;

    private Instant gmtModified;
}
