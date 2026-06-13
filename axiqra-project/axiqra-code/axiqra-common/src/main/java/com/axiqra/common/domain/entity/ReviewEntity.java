package com.axiqra.common.domain.entity;

import com.axiqra.common.domain.enums.ReviewQueue;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.ReviewResult;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Review 审核任务表 axiqra_review
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_review")
public class ReviewEntity extends BaseEntity {

    private String objectType;
    private Long objectId;
    private ReviewQueue queue;
    @Nullable
    private Long reviewerId;
    private RiskLevel riskLevel;
    private ReviewResult status;
    @Nullable
    @Column("reason_code")
    private String reasonCode;
    @Nullable
    @Column("notes")
    private String notes;
    @Nullable
    @Column("appeal_content")
    private String appealContent;
    @Column("is_deleted")
    private boolean isDeleted = false;
    @Nullable
    @Column("tenant_id")
    private Long tenantId;
}
