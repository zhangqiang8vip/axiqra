package com.axiqra.core.service;

import com.axiqra.common.domain.dto.ReviewQueueRequest;
import com.axiqra.common.domain.vo.ReviewQueueItemVO;
import com.axiqra.common.domain.vo.ReviewQueueStatsVO;

import java.util.List;

/**
 * 审核队列服务接口
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
public interface ReviewQueueService {

    /**
     * 将内容加入审核队列
     */
    ReviewQueueItemVO enqueue(Long userId, ReviewQueueRequest request);

    /**
     * 获取审核队列列表
     */
    List<ReviewQueueItemVO> getQueue(String status, int limit);

    /**
     * 获取用户待审核的队列项
     */
    List<ReviewQueueItemVO> getMyAssignments(Long reviewerId);

    /**
     * 认领审核任务
     */
    ReviewQueueItemVO claimTask(Long reviewerId, String queueItemId);

    /**
     * 执行审核
     */
    ReviewQueueItemVO review(Long reviewerId, String queueItemId, String result, String reasonCode, String notes);

    /**
     * 获取审核队列统计
     */
    ReviewQueueStatsVO getQueueStats();

    /**
     * 获取队列项详情
     */
    ReviewQueueItemVO getQueueItem(String queueItemId);

    /**
     * 拒绝审核任务
     */
    ReviewQueueItemVO rejectTask(Long reviewerId, String queueItemId, String reason);
}
