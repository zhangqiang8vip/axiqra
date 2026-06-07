package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 工程项目表 axiqra_project
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_project")
public class ProjectEntity extends BaseEntity {

    private Long workspaceId;
    private String projectName;
    private String techStack;
    private String environment;
    private Long ownerId;
    private String status;
    private boolean isDeleted = false;
}
