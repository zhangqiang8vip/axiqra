package com.axiqra.core.event;

import lombok.Getter;

/**
 * Feedback 提交事件
 */
@Getter
public class FeedbackSubmittedEvent extends DomainEvent {

    private final Long feedbackId;
    private final Long solutionId;
    private final String feedbackType;
    private final String resultType;

    public FeedbackSubmittedEvent(Long feedbackId, Long solutionId, String feedbackType, Long userId) {
        super(EventType.FEEDBACK_SUBMITTED.getCode(), null, userId);
        this.feedbackId = feedbackId;
        this.solutionId = solutionId;
        this.feedbackType = feedbackType;
        this.resultType = feedbackType;
    }
}
