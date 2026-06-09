package com.axiqra.common.domain.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Connect 会话事件
 */
@Data
@NoArgsConstructor
@Builder
public class ConnectSessionEventVO {

    private String fromStatus;
    private String toStatus;
    private String action;
    private String reason;
    private OffsetDateTime occurredAt;

    public ConnectSessionEventVO(String fromStatus, String toStatus, String action, String reason, OffsetDateTime occurredAt) {
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.action = action;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public static List<ConnectSessionEventVO> createHistory(String initialStatus, String finalStatus, String reason, OffsetDateTime occurredAt) {
        List<ConnectSessionEventVO> history = new ArrayList<>();
        history.add(ConnectSessionEventVO.builder()
                .fromStatus("INIT")
                .toStatus(initialStatus)
                .action("DOCTOR_EVALUATED")
                .reason(reason)
                .occurredAt(occurredAt)
                .build());
        if (!initialStatus.equals(finalStatus)) {
            history.add(ConnectSessionEventVO.builder()
                    .fromStatus(initialStatus)
                    .toStatus(finalStatus)
                    .action("SESSION_CREATED")
                    .reason(reason)
                    .occurredAt(occurredAt)
                    .build());
        }
        return history;
    }
}
