package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solution 版本视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolutionVersionVO {

    private Long id;
    private Integer versionNumber;
    private String steps;
    private String applicableContext;
    private String nonApplicableContext;
    private String evidence;
    private String risk;
    private String rollback;
    private boolean active;
}
