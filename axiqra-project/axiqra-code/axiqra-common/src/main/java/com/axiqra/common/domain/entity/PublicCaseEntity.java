package com.axiqra.common.domain.entity;

import com.axiqra.common.domain.enums.WorkspaceType;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Public Case 表 axiqra_public_case
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_public_case")
public class PublicCaseEntity extends BaseEntity {

    private Long sourceCaseId;
    private Long workspaceId;
    private Long authorId;
    private String redactionStatus;
    @Nullable
    private Long reviewId;
    private com.axiqra.common.domain.enums.PublicCaseStatus status;
    @Column("is_deleted")
    private boolean isDeleted = false;
}
