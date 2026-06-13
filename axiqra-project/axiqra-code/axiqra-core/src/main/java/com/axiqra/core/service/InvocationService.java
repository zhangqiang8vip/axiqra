package com.axiqra.core.service;

import com.axiqra.common.domain.dto.InvocationReportRequest;
import com.axiqra.common.domain.vo.InvocationDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;

/**
 * Invocation Service 接口
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
public interface InvocationService {

    /**
     * 上报调用结果（幂等）
     *
     * @param userId  用户 ID
     * @param request  上报请求
     * @return Invocation 详情
     */
    InvocationDetailVO reportInvocation(Long userId, InvocationReportRequest request);

    /**
     * 获取 Invocation 详情
     *
     * @param userId        用户 ID
     * @param invocationId  Invocation ID
     * @return Invocation 详情
     */
    InvocationDetailVO getInvocationDetail(Long userId, Long invocationId);

    /**
     * 检查用户是否有权访问指定 Invocation
     *
     * @param userId       用户 ID
     * @param invocationId Invocation ID
     * @return 是否有权
     */
    boolean isUserAuthorized(Long userId, Long invocationId);

    /**
     * 获取 Solution 的反馈统计
     *
     * @param solutionId Solution ID
     * @return 反馈统计
     */
    SolutionFeedbackStatsVO getSolutionFeedbackStats(Long solutionId);
}
