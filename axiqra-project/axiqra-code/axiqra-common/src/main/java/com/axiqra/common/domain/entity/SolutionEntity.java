package com.axiqra.common.domain.entity;

import com.axiqra.common.domain.enums.LicenseScope;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.SolutionStatus;
import com.axiqra.common.domain.enums.VerificationLevel;
import com.axiqra.common.domain.enums.VisibilityScope;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Solution 表 axiqra_solution
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_solution")
public class SolutionEntity extends BaseEntity {

    private Long authorId;
    private Long workspaceId;
    @Nullable
    private Long projectId;
    private String solutionCode;
    private String title;
    @Nullable
    private String domain;
    @Nullable
    private String techStack;
    private VerificationLevel verificationLevel;
    private RiskLevel riskLevel;
    private SolutionStatus status;
    private VisibilityScope visibilityScope;
    @Nullable
    private LicenseScope licenseScope;
    @Nullable
    @Column("tenant_id")
    private Long tenantId;
    @Nullable
    private Long sourceCaseId;
    @Column("is_deleted")
    private boolean isDeleted = false;
}
