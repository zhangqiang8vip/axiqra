package com.axiqra.common.port;

import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberRole;

import java.util.List;

/**
 * RBAC 权限校验 Port 接口
 *
 * <p>基于成员角色（owner/admin/member/viewer）进行权限判断。
 * 实际权限由 Workspace 空间成员关系（membership 表）驱动。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
public interface RbacPort {

    /**
     * 检查用户在指定空间中是否具有指定角色
     *
     * @param userId      用户 ID
     * @param workspaceId  空间 ID
     * @param requiredRole 要求的最低角色
     * @return true 表示具备该角色或更高角色
     */
    boolean hasRole(Long userId, Long workspaceId, MemberRole requiredRole);

    /**
     * 检查用户是否为指定空间的 owner
     */
    boolean isOwner(Long userId, Long workspaceId);

    /**
     * 检查用户是否为指定空间的管理员或所有者
     */
    boolean isAdmin(Long userId, Long workspaceId);

    /**
     * 获取用户在指定空间中的角色
     */
    MemberRole getRole(Long userId, Long workspaceId);

    /**
     * 获取用户在所有空间中的成员关系列表
     */
    List<MembershipEntity> getMemberships(Long userId);

    /**
     * 检查用户是否属于某空间
     */
    boolean isMember(Long userId, Long workspaceId);
}
