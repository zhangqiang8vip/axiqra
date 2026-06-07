package com.axiqra.common.domain.vo;

import com.axiqra.common.domain.entity.WorkspaceEntity;
import com.axiqra.common.domain.enums.WorkspaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 工作空间信息 VO
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceVO {

    private Long id;
    private String workspaceName;
    private WorkspaceType workspaceType;
    private Long ownerId;
    private Long tenantId;
    private String myRole;
    private Long memberCount;
    private Instant gmtCreate;

    public static WorkspaceVO from(WorkspaceEntity entity) {
        if (entity == null) {
            return null;
        }
        return WorkspaceVO.builder()
                .id(entity.getId())
                .workspaceName(entity.getWorkspaceName())
                .workspaceType(entity.getWorkspaceType())
                .ownerId(entity.getOwnerId())
                .tenantId(entity.getTenantId())
                .gmtCreate(entity.getGmtCreate())
                .build();
    }

    public WorkspaceVO withMyRole(String role) {
        this.myRole = role;
        return this;
    }

    public WorkspaceVO withMemberCount(Long count) {
        this.memberCount = count;
        return this;
    }
}
