package com.axiqra.core.event;

import lombok.Getter;

/**
 * Solution 审核通过事件
 */
@Getter
public class SolutionReviewApprovedEvent extends DomainEvent {

    private final Long solutionId;
    private final String previousStatus;
    private final String newStatus;
    private final Long reviewerId;

    public SolutionReviewApprovedEvent(Long solutionId, String previousStatus, Long reviewerId) {
        super(EventType.SOLUTION_REVIEW_APPROVED.getCode(), null, reviewerId);
        this.solutionId = solutionId;
        this.previousStatus = previousStatus;
        this.newStatus = "REVIEWED";
        this.reviewerId = reviewerId;
    }
}
