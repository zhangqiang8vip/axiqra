package com.axiqra.core.service;

import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberRole;

import java.util.List;

/**
 * RBAC 权限服务接口
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
public interface RbacService {

    /**
     * 检查用户在指定空间中是否具有指定角色
     */
    boolean hasRole(Long userId, Long workspaceId, MemberRole requiredRole);

    /**
     * 检查用户是否为空间所有者
     */
    boolean isOwner(Long userId, Long workspaceId);

    /**
     * 检查用户是否为空间管理员或所有者
     */
    boolean isAdmin(Long userId, Long workspaceId);

    /**
     * 获取用户在指定空间的角色
     */
    MemberRole getRole(Long userId, Long workspaceId);

    /**
     * 获取用户所有成员关系
     */
    List<MembershipEntity> getMemberships(Long userId);

    /**
     * 检查用户是否属于某空间（任意角色）
     */
    boolean isMember(Long userId, Long workspaceId);

    /**
     * 检查用户是否持有指定 scope
     */
    boolean hasScope(Long userId, String scope);
}
