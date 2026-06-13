package com.axiqra.core.service;

import com.axiqra.common.domain.vo.ContributionSummaryVO;

import java.util.List;

/**
 * Contribution Service 接口
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
public interface ContributionService {

    /**
     * 获取用户贡献汇总
     *
     * @param actorId 贡献者 ID
     * @return 贡献汇总
     */
    ContributionSummaryVO getContributionSummary(Long actorId);

    /**
     * 获取用户贡献记录
     *
     * @param actorId 贡献者 ID
     * @param limit   数量限制
     * @return 贡献记录列表
     */
    List<ContributionSummaryVO.ContributionRecord> getContributionRecords(Long actorId, int limit);

    /**
     * 记录贡献事件
     *
     * @param actorId    贡献者 ID
     * @param eventType  事件类型
     * @param objectType 对象类型
     * @param objectId   对象 ID
     * @param points     积分
     * @param evidenceRefs 证据引用
     */
    void recordContribution(Long actorId, String eventType, String objectType, Long objectId, int points, String evidenceRefs);
}
