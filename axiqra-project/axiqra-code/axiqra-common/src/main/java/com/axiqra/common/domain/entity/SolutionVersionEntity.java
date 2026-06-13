package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.ColumnAlias;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * Solution 版本表 axiqra_solution_version
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_solution_version")
public class SolutionVersionEntity extends BaseEntity {

    private Long solutionId;
    private Integer versionNumber;
    @Nullable
    private String steps;
    @Nullable
    private String applicableContext;
    @Nullable
    private String nonApplicableContext;
    @Nullable
    private String evidence;
    @Nullable
    private String risk;
    @Nullable
    @ColumnAlias("rollback_info")
    private String rollback;
    private boolean isActive;
}
