package com.axiqra.core.service;

import com.axiqra.common.domain.vo.ReviewDetailVO;

import java.util.List;

/**
 * Review Service 接口
 *
 * @author Axiqra Team
 * @date 2026-06-11
 */
public interface ReviewService {

    /**
     * 获取待审核队列
     *
     * @param userId  审核员 ID
     * @param queue   队列类型: human/auto/quarantined
     * @param limit   数量限制
     * @return 待审核列表
     */
    List<ReviewDetailVO> getPendingReviews(Long userId, String queue, int limit);

    /**
     * 审核通过
     *
     * @param reviewerId 审核员 ID
     * @param reviewId  审核 ID
     * @param reasonCode 原因码
     * @param notes     备注
     * @return 审核详情
     */
    ReviewDetailVO approve(Long reviewerId, Long reviewId, String reasonCode, String notes);

    /**
     * 审核拒绝
     *
     * @param reviewerId 审核员 ID
     * @param reviewId  审核 ID
     * @param reasonCode 原因码
     * @param notes     备注
     * @return 审核详情
     */
    ReviewDetailVO reject(Long reviewerId, Long reviewId, String reasonCode, String notes);

    /**
     * 隔离内容
     *
     * @param reviewerId 审核员 ID
     * @param reviewId   审核 ID
     * @param reasonCode 原因码
     * @param notes      备注
     * @return 审核详情
     */
    ReviewDetailVO quarantine(Long reviewerId, Long reviewId, String reasonCode, String notes);

    /**
     * 申诉
     *
     * @param userId    用户 ID
     * @param reviewId  审核 ID
     * @param appealContent 申诉内容
     * @return 审核详情
     */
    ReviewDetailVO appeal(Long userId, Long reviewId, String appealContent);

    /**
     * 获取审核详情
     *
     * @param userId   用户 ID
     * @param reviewId 审核 ID
     * @return 审核详情
     */
    ReviewDetailVO getReviewDetail(Long userId, Long reviewId);
}
