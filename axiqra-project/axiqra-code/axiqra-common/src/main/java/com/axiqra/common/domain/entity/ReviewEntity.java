package com.axiqra.common.domain.entity;

import com.axiqra.common.domain.enums.ReviewQueue;
import com.axiqra.common.domain.enums.RiskLevel;
import com.axiqra.common.domain.enums.ReviewResult;
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
}
