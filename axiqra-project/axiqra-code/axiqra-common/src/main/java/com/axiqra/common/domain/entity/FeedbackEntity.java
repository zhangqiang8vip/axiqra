package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Feedback 反馈表 axiqra_feedback
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_feedback")
public class FeedbackEntity extends BaseEntity {

    private Long invocationId;
    private Long userId;
    private String feedbackType;
    @Nullable
    private String feedbackContent;
    @Nullable
    private String evidenceRefs;
    @Nullable
    private String contextDelta;
    @Nullable
    private String boundaryNotes;
    private String status;
    @Column("is_deleted")
    private boolean isDeleted = false;
    @Nullable
    @Column("tenant_id")
    private Long tenantId;
}
