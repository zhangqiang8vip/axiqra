package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 用户表 axiqra_user
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_user")
public class UserEntity extends BaseEntity {

    private String username;
    private String passwordHash;
    private String email;
    private String nickname;
    private String avatar;
    @Column("tenant_id")
    private Long tenantId;
    @Column("is_deleted")
    private boolean isDeleted = false;
}
