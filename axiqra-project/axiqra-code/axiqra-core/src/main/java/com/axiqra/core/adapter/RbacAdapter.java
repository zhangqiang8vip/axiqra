package com.axiqra.core.adapter;

import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.domain.enums.MemberStatus;
import com.axiqra.common.port.RbacPort;
import com.axiqra.core.mapper.MembershipMapper;
import com.axiqra.core.mapper.WorkspaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * RBAC 权限校验适配器
 *
 * <p>基于 Workspace 成员关系（membership 表）进行角色判断。
 * 角色层级：admin &gt; member &gt; viewer，owner 视为最高角色。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RbacAdapter implements RbacPort {

    private final MembershipMapper membershipMapper;
    private final WorkspaceMapper workspaceMapper;

    /** 角色层级（数值越大权限越高） */
    private static final List<MemberRole> ROLE_HIERARCHY = Arrays.asList(
            MemberRole.VIEWER,
            MemberRole.MEMBER,
            MemberRole.ADMIN,
            MemberRole.OWNER
    );

    @Override
    public boolean hasRole(Long userId, Long workspaceId, MemberRole requiredRole) {
        if (userId == null || workspaceId == null || requiredRole == null) {
            return false;
        }
        MembershipEntity membership = membershipMapper.selectByUserAndWorkspace(userId, workspaceId);
        if (membership == null) {
            return false;
        }
        if (!MemberStatus.ACTIVE.getCode().equals(membership.getStatus())) {
            return false;
        }
        MemberRole userRole = MemberRole.of(membership.getRole());
        if (userRole == null) {
            return false;
        }
        return isRoleAllowed(userRole, requiredRole);
    }

    @Override
    public boolean isOwner(Long userId, Long workspaceId) {
        return hasRole(userId, workspaceId, MemberRole.OWNER);
    }

    @Override
    public boolean isAdmin(Long userId, Long workspaceId) {
        return hasRole(userId, workspaceId, MemberRole.ADMIN);
    }

    @Override
    public MemberRole getRole(Long userId, Long workspaceId) {
        if (userId == null || workspaceId == null) {
            return null;
        }
        MembershipEntity membership = membershipMapper.selectByUserAndWorkspace(userId, workspaceId);
        if (membership == null) {
            return null;
        }
        if (!MemberStatus.ACTIVE.getCode().equals(membership.getStatus())) {
            return null;
        }
        return MemberRole.of(membership.getRole());
    }

    @Override
    public List<MembershipEntity> getMemberships(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return membershipMapper.selectActiveByUserId(userId);
    }

    @Override
    public boolean isMember(Long userId, Long workspaceId) {
        if (userId == null || workspaceId == null) {
            return false;
        }
        MembershipEntity membership = membershipMapper.selectByUserAndWorkspace(userId, workspaceId);
        return membership != null && MemberStatus.ACTIVE.getCode().equals(membership.getStatus());
    }

    /**
     * 判断用户角色是否满足要求的最低角色
     */
    private boolean isRoleAllowed(MemberRole userRole, MemberRole requiredRole) {
        int userLevel = getRoleLevel(userRole);
        int requiredLevel = getRoleLevel(requiredRole);
        return userLevel >= requiredLevel;
    }

    private int getRoleLevel(MemberRole role) {
        int idx = ROLE_HIERARCHY.indexOf(role);
        return idx >= 0 ? idx : -1;
    }
}
