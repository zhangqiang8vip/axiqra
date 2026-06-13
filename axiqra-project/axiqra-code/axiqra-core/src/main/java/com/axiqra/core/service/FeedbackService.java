package com.axiqra.core.service;

import com.axiqra.common.domain.dto.FeedbackSubmitRequest;
import com.axiqra.common.domain.vo.FeedbackDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;

import java.util.List;

/**
 * Feedback Service 接口
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
public interface FeedbackService {

    /**
     * 提交反馈
     *
     * @param userId  用户 ID
     * @param request 反馈请求
     * @return 反馈详情
     */
    FeedbackDetailVO submitFeedback(Long userId, FeedbackSubmitRequest request);

    /**
     * 获取反馈列表
     *
     * @param userId    用户 ID
     * @param targetType 目标类型
     * @param targetId  目标 ID
     * @return 反馈列表
     */
    List<FeedbackDetailVO> listFeedbacks(Long userId, String targetType, Long targetId);

    /**
     * 获取 Solution 的反馈统计
     *
     * @param solutionId Solution ID
     * @return 反馈统计
     */
    SolutionFeedbackStatsVO getSolutionFeedbackStats(Long solutionId);
}
