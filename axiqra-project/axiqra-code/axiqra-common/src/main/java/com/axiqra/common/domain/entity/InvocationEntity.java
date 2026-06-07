package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Invocation 调用记录表 axiqra_invocation
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_invocation")
public class InvocationEntity extends BaseEntity {

    private String requestId;
    private Long userId;
    private String targetType;
    private Long targetId;
    private Long workspaceId;
    private String invocationCode;
    private String toolType;
    @Nullable
    private String queryHash;
    private Integer riskLevel;
    private Integer requiredConfirmation;
    private Integer confirmationObtained;
    @Nullable
    private String resultType;
    @Nullable
    private Long tenantId;
    private boolean isDeleted = false;
}
