package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 贡献排行榜视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionRankingVO {

    private Integer rank;
    private Long userId;
    private String username;
    private Long totalPoints;
    private Long contributionCount;
}
