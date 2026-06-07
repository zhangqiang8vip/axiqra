package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.port.PolicyEnginePort;
import com.axiqra.common.port.RbacPort;
import com.axiqra.core.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RBAC 权限服务实现
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private final RbacPort rbacPort;
    private final PolicyEnginePort policyEnginePort;

    @Override
    public boolean hasRole(Long userId, Long workspaceId, MemberRole requiredRole) {
        return rbacPort.hasRole(userId, workspaceId, requiredRole);
    }

    @Override
    public boolean isOwner(Long userId, Long workspaceId) {
        return rbacPort.isOwner(userId, workspaceId);
    }

    @Override
    public boolean isAdmin(Long userId, Long workspaceId) {
        return rbacPort.isAdmin(userId, workspaceId);
    }

    @Override
    public MemberRole getRole(Long userId, Long workspaceId) {
        return rbacPort.getRole(userId, workspaceId);
    }

    @Override
    public List<MembershipEntity> getMemberships(Long userId) {
        return rbacPort.getMemberships(userId);
    }

    @Override
    public boolean isMember(Long userId, Long workspaceId) {
        return rbacPort.isMember(userId, workspaceId);
    }

    @Override
    public boolean hasScope(Long userId, String scope) {
        return policyEnginePort.hasScope(userId, scope);
    }
}
