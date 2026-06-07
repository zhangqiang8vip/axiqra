package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Candidate Seed 表 axiqra_candidate_seed
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_candidate_seed")
public class CandidateSeedEntity extends BaseEntity {

    private Long workspaceId;
    private Long authorId;
    private String queryHash;
    private String taskGoal;
    @Nullable
    private String techStack;
    @Nullable
    private String coverageGap;
    private String status;
    private Integer isDeleted;
    @Nullable
    private Long assigneeId;
    @Nullable
    private Long solutionId;
    @Nullable
    private Long tenantId;
}
