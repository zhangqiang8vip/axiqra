package com.axiqra.core.service;

import com.axiqra.common.domain.vo.PublicCaseDetailVO;
import com.axiqra.common.domain.vo.ReviewStatusVO;

import java.util.List;

/**
 * Public Case 服务接口
 */
public interface PublicCaseService {

    /**
     * 将 Project Case 发布为 Public Case
     */
    PublicCaseDetailVO publish(Long userId, Long projectCaseId);

    /**
     * 提交 Public Case 审核
     */
    ReviewSubmitResult submitForReview(Long userId, Long publicCaseId);

    /**
     * 获取审核状态
     */
    ReviewStatusVO getReviewStatus(Long userId, Long publicCaseId);

    /**
     * 获取 Public Case 详情（登录用户）
     */
    PublicCaseDetailVO getDetail(Long userId, Long publicCaseId);

    /**
     * 列出用户可访问的 Public Case
     */
    List<PublicCaseDetailVO> listPublicCases(Long userId, Integer limit);

    /**
     * 获取公开 Public Case 详情（匿名用户）
     */
    PublicCaseDetailVO getPublicDetail(Long publicCaseId);

    /**
     * 列出公开可读的 Public Case
     */
    List<PublicCaseDetailVO> listPublicCases(Integer limit);

    /**
     * 审核提交结果
     */
    record ReviewSubmitResult(
            Long publicCaseId,
            String reviewId,
            String status,
            String message
    ) {}
}
