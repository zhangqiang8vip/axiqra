package com.axiqra.common.domain.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Connect 会话视图对象
 */
@Data
@NoArgsConstructor
@Builder
public class ConnectSessionVO {

    private String sessionId;
    private Long userId;
    private String channel;
    private String toolType;
    private String targetType;
    private Long targetId;
    private Long workspaceId;
    private String status;
    private String riskLevel;
    private Boolean confirmationObtained;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private ConnectDoctorVO doctor;
    private List<ConnectSessionEventVO> history;

    public ConnectSessionVO(String sessionId,
                            Long userId,
                            String channel,
                            String toolType,
                            String targetType,
                            Long targetId,
                            Long workspaceId,
                            String status,
                            String riskLevel,
                            Boolean confirmationObtained,
                            OffsetDateTime createdAt,
                            OffsetDateTime expiresAt,
                            ConnectDoctorVO doctor,
                            List<ConnectSessionEventVO> history) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.channel = channel;
        this.toolType = toolType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.workspaceId = workspaceId;
        this.status = status;
        this.riskLevel = riskLevel;
        this.confirmationObtained = confirmationObtained;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.doctor = doctor;
        this.history = history;
    }
}
