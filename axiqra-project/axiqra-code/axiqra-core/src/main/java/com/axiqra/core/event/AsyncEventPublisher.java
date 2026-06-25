package com.axiqra.core.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 异步事件发布者
 * 将业务事件发布到 RabbitMQ，实现服务解耦
 *
 * @author Axiqra Team
 */
@Slf4j
@Component
public class AsyncEventPublisher {

    private static final String EXCHANGE_NAME = "axiqra.events";
    private static final String ROUTING_KEY_PREFIX = "axiqra.";

    private final RabbitTemplate rabbitTemplate;

    @Value("${axiqra.event.enabled:false}")
    private boolean eventEnabled;

    @Value("${axiqra.event.async:true}")
    private boolean asyncEnabled;

    public AsyncEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发布领域事件
     */
    public void publish(DomainEvent event) {
        if (!eventEnabled) {
            log.debug("事件发布已禁用: eventType={}", event.getEventType());
            return;
        }

        String routingKey = ROUTING_KEY_PREFIX + event.getEventType();

        try {
            if (asyncEnabled) {
                rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, event);
            } else {
                rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, event);
            }
            log.info("事件已发布: eventId={}, eventType={}, routingKey={}",
                    event.getEventId(), event.getEventType(), routingKey);
        } catch (AmqpException e) {
            log.error("事件发布失败: eventId={}, eventType={}, error={}",
                    event.getEventId(), event.getEventType(), e.getMessage());
            // 事件发布失败不应影响主业务流程，记录日志即可
        }
    }

    /**
     * 发布 Solution 审核通过事件
     */
    public void publishSolutionReviewApproved(Long solutionId, String previousStatus, Long reviewerId) {
        SolutionReviewApprovedEvent event = new SolutionReviewApprovedEvent(solutionId, previousStatus, reviewerId);
        publish(event);
    }

    /**
     * 发布 Feedback 提交事件
     */
    public void publishFeedbackSubmitted(Long feedbackId, Long solutionId, String feedbackType, Long userId) {
        FeedbackSubmittedEvent event = new FeedbackSubmittedEvent(feedbackId, solutionId, feedbackType, userId);
        publish(event);
    }

    /**
     * 发布 Contribution 积分增加事件
     */
    public void publishContributionAdded(Long userId, Long workspaceId, String contributionType, int points) {
        ContributionAddedEvent event = new ContributionAddedEvent(userId, workspaceId, contributionType, points);
        publish(event);
    }
}
