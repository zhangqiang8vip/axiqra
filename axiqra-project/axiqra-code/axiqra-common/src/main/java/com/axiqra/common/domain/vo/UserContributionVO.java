package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 用户贡献统计视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserContributionVO {

    private Long userId;
    private Long totalPoints;
    private Long monthlyPoints;
    private Long weeklyPoints;
    private Integer rank;
    private Long contributionCount;
    private Map<String, Long> contributionByType;
}
