package com.axiqra.core.service;

import com.axiqra.common.domain.dto.SolutionCreateFromProjectCaseRequest;
import com.axiqra.common.domain.vo.SolutionDetailVO;
import com.axiqra.common.domain.vo.SearchResultItemVO;

import java.util.List;

/**
 * Solution 服务接口
 */
public interface SolutionService {

    SolutionDetailVO createFromProjectCase(Long userId, SolutionCreateFromProjectCaseRequest request);

    SolutionDetailVO getDetail(Long userId, Long solutionId);

    SolutionDetailVO getPublicDetail(Long solutionId);

    List<SearchResultItemVO> listPublicSolutions(String query,
                                                 String domain,
                                                 String techStack,
                                                 Integer minVerificationLevel,
                                                 Integer limit);

    /**
     * 提交审核（状态流转：DRAFT/CANDIDATE → NEEDS_REVIEW）
     */
    void transitionToNeedsReview(Long userId, Long solutionId);

    /**
     * 审核通过后更新 Solution 状态（NEEDS_REVIEW → REVIEWED）
     */
    void transitionAfterReviewApproved(Long solutionId, Long reviewerId);

    /**
     * 审核拒绝后更新 Solution 状态（NEEDS_REVIEW → REJECTED）
     */
    void transitionAfterReviewRejected(Long solutionId, Long reviewerId, String reasonCode);

    /**
     * 隔离 Solution（任意状态 → QUARANTINED）
     */
    void transitionToQuarantined(Long solutionId, Long reviewerId, String reasonCode);

    /**
     * 归档 Solution（DEPRECATED → ARCHIVED）
     */
    void transitionToArchived(Long solutionId, Long userId);
}
