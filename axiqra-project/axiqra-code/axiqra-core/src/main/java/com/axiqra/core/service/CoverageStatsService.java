package com.axiqra.core.service;

import com.axiqra.common.domain.vo.CoverageStatsVO;

import java.util.List;
import java.util.Map;

/**
 * 覆盖范围统计服务接口
 *
 * 统计 Solution 的技术栈、错误类型、框架覆盖情况
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
public interface CoverageStatsService {

    /**
     * 获取整体覆盖范围统计
     */
    CoverageStatsVO getOverallCoverage();

    /**
     * 按技术栈获取覆盖统计
     */
    Map<String, Long> getCoverageByTechStack();

    /**
     * 按错误类型获取覆盖统计
     */
    Map<String, Long> getCoverageByErrorType();

    /**
     * 按框架获取覆盖统计
     */
    Map<String, Long> getCoverageByFramework();

    /**
     * 获取解决方案缺口列表（未被覆盖的错误类型）
     */
    List<CoverageStatsVO.CoverageGap> getCoverageGaps();

    /**
     * 获取技术栈覆盖率排名
     */
    List<CoverageStatsVO.TechStackCoverage> getTechStackRanking();

    /**
     * 获取Candidate Seed 缺口统计
     */
    long getCandidateSeedCount();
}
