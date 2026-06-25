package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 覆盖范围统计 VO
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverageStatsVO {

    /** 总 Solution 数量 */
    private long totalSolutions;

    /** 已验证的 Solution 数量 */
    private long verifiedSolutions;

    /** 总错误类型数量 */
    private long totalErrorTypes;

    /** 已覆盖的错误类型数量 */
    private long coveredErrorTypes;

    /** 错误类型覆盖率 (0-100) */
    private double errorCoveragePercent;

    /** 总技术栈数量 */
    private long totalTechStacks;

    /** 已覆盖的技术栈数量 */
    private long coveredTechStacks;

    /** 技术栈覆盖率 (0-100) */
    private double techStackCoveragePercent;

    /** Candidate Seed 数量 */
    private long candidateSeedCount;

    /** 统计生成时间 */
    private Long generatedAt;

    /**
     * 覆盖缺口
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoverageGap {
        /** 缺口类型: error_type / tech_stack / framework */
        private String gapType;

        /** 缺口名称 */
        private String gapName;

        /** 缺口描述 */
        private String description;

        /** 相关 Solution 数量 */
        private long relatedSolutionCount;

        /** 优先级: high / medium / low */
        private String priority;
    }

    /**
     * 技术栈覆盖统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechStackCoverage {
        /** 技术栈名称 */
        private String techStack;

        /** 覆盖数量 */
        private long solutionCount;

        /** 覆盖率 (0-100) */
        private double coveragePercent;

        /** 排名 */
        private int rank;
    }
}
