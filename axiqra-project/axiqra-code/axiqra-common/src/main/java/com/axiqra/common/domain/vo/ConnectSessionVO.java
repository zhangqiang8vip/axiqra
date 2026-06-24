package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Connect 会话视图对象
 * 
 * 对应 D09 文档第 9 节规定的接入会话必须记录的字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    
    // D09 文档要求的补充字段
    
    /** 工具能力声明 */
    private String toolCapability;
    
    /** 授权范围 */
    private String authScope;
    
    /** 诊断结果详情 */
    private String doctorResult;
    
    /** 最后访问时间 */
    private OffsetDateTime lastSeenAt;
    
    /** 接入指令快照 */
    private String instructionSnapshot;
    
    /** 失败原因 */
    private String failureReason;
}
