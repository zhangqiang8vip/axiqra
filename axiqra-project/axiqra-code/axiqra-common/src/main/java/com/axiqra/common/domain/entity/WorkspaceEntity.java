package com.axiqra.common.domain.entity;

import com.axiqra.common.domain.enums.WorkspaceType;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 工作空间表 axiqra_workspace
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_workspace")
public class WorkspaceEntity extends BaseEntity {

    @NotNull
    private Long ownerId;
    @NotBlank
    private String workspaceName;
    @NotNull
    private WorkspaceType workspaceType;
    @Nullable
    @Column("tenant_id")
    private Long tenantId;
    @Column("is_deleted")
    private boolean isDeleted = false;
}
