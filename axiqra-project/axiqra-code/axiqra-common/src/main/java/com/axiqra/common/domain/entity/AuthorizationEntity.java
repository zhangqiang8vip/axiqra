package com.axiqra.common.domain.entity;

import jakarta.validation.constraints.NotNull;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

import java.time.Instant;

/**
 * Authorization 授权快照表 axiqra_authorization
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_authorization")
public class AuthorizationEntity extends BaseEntity {

    private Long ownerId;
    private String scope;
    private String licenseScope;
    private String status;
    @Nullable
    private Instant revokedAt;
    @NotNull
    private Long tenantId;
    private boolean isDeleted = false;
}
