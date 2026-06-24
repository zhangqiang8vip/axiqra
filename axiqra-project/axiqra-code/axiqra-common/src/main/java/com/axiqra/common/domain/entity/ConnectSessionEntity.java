package com.axiqra.common.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Connect 会话表 axiqra_connect_session
 * 
 * 对应 D09 文档第 9 节规定的接入会话必须记录的字段
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
    
    // D09 文档要求的补充字段
    
    /** 工具能力声明 (supports_mcp, supports_cli, supports_local_cache 等) */
    @Nullable
    private String toolCapability;
    
    /** 授权范围 (search, read, submit, feedback 等) */
    @Nullable
    private String authScope;
    
    /** 诊断结果详情 (JSON 格式) */
    @Nullable
    @Column("doctor_result")
    private String doctorResult;
    
    /** 最后访问时间 */
    @Nullable
    @Column("last_seen_at")
    private Instant lastSeenAt;
    
    /** 接入指令快照 */
    @Nullable
    @Column("instruction_snapshot")
    private String instructionSnapshot;
    
    /** 失败原因 (当状态为 failed 时) */
    @Nullable
    private String failureReason;
}
