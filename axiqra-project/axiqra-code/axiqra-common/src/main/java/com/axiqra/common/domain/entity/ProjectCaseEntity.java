package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Project Case 表 axiqra_project_case
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_project_case")
public class ProjectCaseEntity extends BaseEntity {

    private Long traceId;
    private Long workspaceId;
    @Nullable
    private Long projectId;
    private Long authorId;
    @Nullable
    private Long authorizationId;
    private String visibilityScope;
    @Nullable
    private String licenseScope;
    private String redactionStatus;
    private String status;
    @Nullable
    private Long reviewId;
    @Column("is_deleted")
    private boolean isDeleted = false;
}
