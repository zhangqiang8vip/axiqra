package com.axiqra.core.event;

import lombok.Getter;

/**
 * Contribution 积分增加事件
 */
@Getter
public class ContributionAddedEvent extends DomainEvent {

    private final Long userId;
    private final Long workspaceId;
    private final String contributionType;
    private final int points;

    public ContributionAddedEvent(Long userId, Long workspaceId, String contributionType, int points) {
        super(EventType.CONTRIBUTION_ADDED.getCode(), workspaceId, userId);
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.contributionType = contributionType;
        this.points = points;
    }
}
