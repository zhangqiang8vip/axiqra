package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;

/**
 * Connect 会话表 axiqra_connect_session
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table("axiqra_connect_session")
public class ConnectSessionEntity extends BaseEntity {

    private String sessionId;
    private Long userId;
    private String channel;
    private String toolType;
    @Nullable
    private String targetType;
    @Nullable
    private Long targetId;
    @Nullable
    private Long workspaceId;
    private String status;
    private String riskLevel;
    private Integer confirmationObtained;
    private OffsetDateTime expiresAt;
    @Nullable
    private String doctorStatus;
    @Nullable
    private String doctorSnapshot;
    @Nullable
    private String historySnapshot;
    @Column("tenant_id")
    @Nullable
    private Long tenantId;
    @Column("is_deleted")
    private boolean isDeleted = false;
}
