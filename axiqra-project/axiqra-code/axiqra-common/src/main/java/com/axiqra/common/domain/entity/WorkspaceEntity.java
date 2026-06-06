package com.axiqra.common.domain.entity;

import com.axiqra.common.domain.enums.WorkspaceType;
import com.mybatisflex.annotation.Table;
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

    private Long ownerId;
    private String workspaceName;
    private WorkspaceType workspaceType;
    private Long tenantId;
    private Integer isDeleted;
}
