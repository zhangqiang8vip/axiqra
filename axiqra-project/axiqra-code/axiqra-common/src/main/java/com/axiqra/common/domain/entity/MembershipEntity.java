package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

import java.time.Instant;

/**
 * 空间成员关系表 axiqra_membership
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_membership")
public class MembershipEntity extends BaseEntity {

    private Long userId;
    private Long workspaceId;
    private String role;
    private String status;
    private Integer isDeleted;
    @Nullable
    private Long tenantId;
}
