package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 贡献统计 VO
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionStatsVO {

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 总积分 */
    private long totalPoints;

    /** 本月积分 */
    private long monthlyPoints;

    /** 本周积分 */
    private long weeklyPoints;

    /** 排名 */
    private int rank;

    /** 贡献类型统计 */
    private Map<String, Long> contributionByType;

    /** 贡献次数 */
    private long contributionCount;

    /** 统计生成时间 */
    private Long generatedAt;
}
