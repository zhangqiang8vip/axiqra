package com.axiqra.core.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 领域事件基类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {

    private String eventId;
    private String eventType;
    private Instant occurredAt;
    private Long workspaceId;
    private Long userId;

    public DomainEvent(String eventType, Long workspaceId, Long userId) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredAt = Instant.now();
        this.workspaceId = workspaceId;
        this.userId = userId;
    }
}
