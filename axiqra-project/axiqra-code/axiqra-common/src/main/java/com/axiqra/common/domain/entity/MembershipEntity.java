package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

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
    @Column("is_deleted")
    private boolean isDeleted = false;
    @Nullable
    @Column("tenant_id")
    private Long tenantId;
}
