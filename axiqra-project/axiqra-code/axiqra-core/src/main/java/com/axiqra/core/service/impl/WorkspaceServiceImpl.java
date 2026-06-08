package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.WorkspaceCreateRequest;
import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.domain.entity.WorkspaceEntity;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.domain.enums.MemberStatus;
import com.axiqra.common.domain.enums.WorkspaceType;
import com.axiqra.common.domain.vo.MemberVO;
import com.axiqra.common.domain.vo.PageResponse;
import com.axiqra.common.domain.vo.WorkspaceVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.mapper.MembershipMapper;
import com.axiqra.core.mapper.WorkspaceMapper;
import com.axiqra.core.service.RbacService;
import com.axiqra.core.service.WorkspaceService;
import com.axiqra.core.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Workspace 工作空间服务实现
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceMapper workspaceMapper;
    private final MembershipMapper membershipMapper;
    private final UserMapper userMapper;
    private final RbacService rbacService;

    @Override
    public PageResponse<WorkspaceVO> listMyWorkspaces(Long userId) {
        if (userId == null) {
            return PageResponse.empty();
        }

        // 个人空间：owner_id = userId
        List<WorkspaceEntity> owned = workspaceMapper.selectByOwnerId(userId);

        // 成员空间：membership 表中 user_id = userId
        List<MembershipEntity> memberships = rbacService.getMemberships(userId);
        List<Long> workspaceIds = memberships.stream()
                .map(MembershipEntity::getWorkspaceId)
                .collect(Collectors.toList());

        List<WorkspaceEntity> memberSpaces = workspaceIds.isEmpty()
                ? List.of()
                : workspaceMapper.selectByWorkspaceIds(workspaceIds);

        // 合并去重
        Map<Long, WorkspaceVO> merged = owned.stream()
                .map(e -> WorkspaceVO.from(e).withMyRole(MemberRole.OWNER.getCode()))
                .collect(Collectors.toMap(WorkspaceVO::getId, v -> v));

        for (WorkspaceEntity e : memberSpaces) {
            if (!merged.containsKey(e.getId())) {
                MembershipEntity m = memberships.stream()
                        .filter(ms -> ms.getWorkspaceId().equals(e.getId()))
                        .findFirst()
                        .orElse(null);
                String role = m != null ? m.getRole() : MemberRole.MEMBER.getCode();
                merged.put(e.getId(), WorkspaceVO.from(e).withMyRole(role));
            }
        }

        List<WorkspaceVO> result = merged.values().stream()
                .sorted((a, b) -> {
                    boolean aIsOwner = MemberRole.OWNER.getCode().equals(a.getMyRole());
                    boolean bIsOwner = MemberRole.OWNER.getCode().equals(b.getMyRole());
                    if (aIsOwner && !bIsOwner) return -1;
                    if (!aIsOwner && bIsOwner) return 1;
                    return Long.compare(a.getId(), b.getId());
                })
                .collect(Collectors.toList());

        return PageResponse.of(result, (long) result.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceVO create(Long userId, String workspaceTypeStr, String workspaceName) {
        if (userId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "userId 不能为空");
        }

        WorkspaceType workspaceType = WorkspaceType.of(workspaceTypeStr);
        if (workspaceType == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不支持的工作空间类型: " + workspaceTypeStr);
        }

        // 个人空间：workspaceName 可为空，自动用 username
        if (workspaceName == null || workspaceName.isBlank()) {
            if (workspaceType == WorkspaceType.PERSONAL) {
                var user = userMapper.selectActiveById(userId);
                workspaceName = user != null ? user.getUsername() + " 的空间" : "我的空间";
            } else {
                throw new BizException(ErrorCode.PARAM_INVALID, "workspaceName 不能为空");
            }
        }

        // 检查同名空间
        var existing = workspaceMapper.selectByOwnerAndName(userId, workspaceName);
        if (existing != null) {
            throw new BizException(ErrorCode.DUPLICATE_ENTRY, "工作空间名称已存在");
        }

        WorkspaceEntity workspace = new WorkspaceEntity()
                .setOwnerId(userId)
                .setWorkspaceName(workspaceName)
                .setWorkspaceType(workspaceType)
                .setDeleted(false);
        workspaceMapper.insertSelective(workspace);

        // 自动将自己加入成员表，角色为 owner
        MembershipEntity membership = new MembershipEntity()
                .setUserId(userId)
                .setWorkspaceId(workspace.getId())
                .setRole(MemberRole.OWNER.getCode())
                .setStatus(MemberStatus.ACTIVE.getCode());
        membershipMapper.insertSelective(membership);

        log.info("创建工作空间: workspaceId={}, name={}, type={}, ownerId={}",
                workspace.getId(), workspaceName, workspaceType, userId);

        return WorkspaceVO.from(workspace)
                .withMyRole(MemberRole.OWNER.getCode())
                .withMemberCount(1L);
    }

    @Override
    public WorkspaceVO getById(Long workspaceId, Long userId) {
        if (workspaceId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "workspaceId 不能为空");
        }
        WorkspaceEntity workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND);
        }

        boolean canAccess = rbacService.isMember(userId, workspaceId)
                || rbacService.isOwner(userId, workspaceId);
        if (!canAccess) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该工作空间");
        }

        MemberRole role = rbacService.getRole(userId, workspaceId);
        long memberCount = membershipMapper.countByWorkspaceId(workspaceId);

        return WorkspaceVO.from(workspace)
                .withMyRole(role != null ? role.getCode() : null)
                .withMemberCount(memberCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceVO update(Long workspaceId, Long userId, String workspaceName, String workspaceType) {
        if (workspaceId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "workspaceId 不能为空");
        }
        if (userId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "userId 不能为空");
        }

        if (!rbacService.isOwner(userId, workspaceId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有所有者可以更新工作空间");
        }

        WorkspaceEntity existing = workspaceMapper.selectById(workspaceId);
        if (existing == null) {
            throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND);
        }

        boolean hasNameUpdate = workspaceName != null && !workspaceName.isBlank();
        boolean hasTypeUpdate = workspaceType != null && !workspaceType.isBlank();

        if (!hasNameUpdate && !hasTypeUpdate) {
            MemberRole role = rbacService.getRole(userId, workspaceId);
            long memberCount = membershipMapper.countByWorkspaceId(workspaceId);
            return WorkspaceVO.from(existing)
                    .withMyRole(role != null ? role.getCode() : null)
                    .withMemberCount(memberCount);
        }

        WorkspaceType newType = null;
        if (hasTypeUpdate) {
            newType = WorkspaceType.of(workspaceType);
            if (newType == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "不支持的工作空间类型: " + workspaceType);
            }
        }

        if (hasNameUpdate) {
            String trimmedName = workspaceName.trim();
            WorkspaceEntity conflict = workspaceMapper.selectByOwnerAndName(existing.getOwnerId(), trimmedName);
            if (conflict != null && !conflict.getId().equals(workspaceId)) {
                throw new BizException(ErrorCode.DUPLICATE_ENTRY, "工作空间名称已存在");
            }
        }

        String cleanedName = hasNameUpdate ? workspaceName.trim() : null;
        String cleanedType = hasTypeUpdate ? newType.getCode() : null;

        int rows;
        try {
            rows = workspaceMapper.updateSelective(workspaceId, cleanedName, cleanedType,
                    existing.getVersion(), Instant.now());
        } catch (DataAccessException e) {
            log.error("更新工作空间数据库异常: workspaceId={}, userId={}", workspaceId, userId, e);
            throw new BizException(ErrorCode.DATABASE_ERROR, "更新失败，请稍后重试");
        }

        if (rows == 0) {
            throw new BizException(ErrorCode.CONCURRENT_MODIFICATION, "数据已被其他人修改，请刷新后重试");
        }

        WorkspaceEntity updated = workspaceMapper.selectById(workspaceId);
        if (updated == null) {
            throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND);
        }
        long memberCount = membershipMapper.countByWorkspaceId(workspaceId);
        log.info("更新工作空间: workspaceId={}, userId={}", workspaceId, userId);
        return WorkspaceVO.from(updated)
                .withMyRole(MemberRole.OWNER.getCode())
                .withMemberCount(memberCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long workspaceId, Long userId) {
        if (workspaceId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "workspaceId 不能为空");
        }
        if (!rbacService.isOwner(userId, workspaceId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有所有者可以删除工作空间");
        }
        WorkspaceEntity workspace = workspaceMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BizException(ErrorCode.WORKSPACE_NOT_FOUND);
        }

        // 软删除工作空间（幂等：version 检查 + is_deleted 保护）
        int rows = workspaceMapper.softDeleteById(workspaceId, workspace.getVersion(), Instant.now());
        if (rows == 0) {
            throw new BizException(ErrorCode.CONCURRENT_MODIFICATION, "工作空间已被修改或删除，请刷新后重试");
        }

        // 软删除所有成员关系
        membershipMapper.softDeleteByWorkspaceId(workspaceId, MemberStatus.SUSPENDED.getCode(), workspace.getVersion());

        log.info("删除工作空间: workspaceId={}, deletedBy={}", workspaceId, userId);
    }

    @Override
    public PageResponse<MemberVO> listMembers(Long workspaceId, Long userId) {
        if (workspaceId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "workspaceId 不能为空");
        }
        // 任意成员可见成员列表
        if (!rbacService.isMember(userId, workspaceId) && !rbacService.isOwner(userId, workspaceId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非成员无权查看成员列表");
        }

        List<MembershipEntity> memberships = membershipMapper.selectActiveByWorkspaceId(workspaceId);
        if (memberships.isEmpty()) {
            return PageResponse.of(List.of(), 0L);
        }
        // 批量查询用户，消除 N+1 问题（1 次查询获取所有用户）
        List<Long> userIds = memberships.stream().map(MembershipEntity::getUserId).collect(Collectors.toList());
        Map<Long, UserEntity> userMap = userMapper.selectActiveByIds(userIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, u -> u));
        List<MemberVO> members = memberships.stream()
                .map(m -> {
                    MemberVO vo = MemberVO.from(m);
                    UserEntity user = userMap.get(m.getUserId());
                    if (user != null) {
                        vo.setUsername(user.getUsername());
                        vo.setNickname(user.getNickname());
                        vo.setEmail(user.getEmail());
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        return PageResponse.of(members, (long) members.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberVO addMember(Long workspaceId, Long userId, Long targetUserId, MemberRole role) {
        if (!rbacService.isAdmin(userId, workspaceId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有管理员可以添加成员");
        }
        if (targetUserId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "targetUserId 不能为空");
        }
        if (role == null) {
            role = MemberRole.MEMBER;
        }
        if (role == MemberRole.OWNER) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能直接添加所有者");
        }

        // 检查目标用户是否存在
        var targetUser = userMapper.selectActiveById(targetUserId);
        if (targetUser == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查是否已存在活跃成员关系
        var existing = membershipMapper.selectActiveByUserAndWorkspace(targetUserId, workspaceId);
        if (existing != null) {
            throw new BizException(ErrorCode.DUPLICATE_ENTRY, "该用户已是空间的活跃成员");
        }

        MembershipEntity membership = new MembershipEntity()
                .setUserId(targetUserId)
                .setWorkspaceId(workspaceId)
                .setRole(role.getCode())
                .setStatus(MemberStatus.ACTIVE.getCode());
        membershipMapper.insertSelective(membership);

        log.info("添加成员: workspaceId={}, memberId={}, userId={}, role={}",
                workspaceId, membership.getId(), targetUserId, role);

        return MemberVO.from(membership)
                .withUsername(targetUser.getUsername())
                .withNickname(targetUser.getNickname())
                .withEmail(targetUser.getEmail());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberRole(Long workspaceId, Long userId, Long memberId, MemberRole newRole) {
        if (!rbacService.isOwner(userId, workspaceId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有所有者可以更新成员角色");
        }
        MembershipEntity membership = membershipMapper.selectById(memberId);
        if (membership == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "成员记录不存在");
        }
        if (!membership.getWorkspaceId().equals(workspaceId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "成员不属于该工作空间");
        }
        if (MemberRole.OWNER.getCode().equals(membership.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能修改所有者的角色");
        }
        if (newRole == MemberRole.OWNER) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能将成员提升为所有者");
        }

        membershipMapper.updateRole(memberId, newRole.getCode());
        log.info("更新成员角色: memberId={}, newRole={}, updatedBy={}", memberId, newRole, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long workspaceId, Long userId, Long targetMemberId) {
        if (!rbacService.isAdmin(userId, workspaceId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有管理员可以移除成员");
        }
        MembershipEntity membership = membershipMapper.selectById(targetMemberId);
        if (membership == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "成员记录不存在");
        }
        if (!membership.getWorkspaceId().equals(workspaceId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "成员不属于该工作空间");
        }
        if (MemberRole.OWNER.getCode().equals(membership.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能移除所有者");
        }
        if (userId.equals(membership.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能将您自己从工作空间中移除");
        }

        int rows = membershipMapper.softDelete(targetMemberId, MemberStatus.SUSPENDED.getCode(), membership.getVersion());
        if (rows == 0) {
            throw new BizException(ErrorCode.CONCURRENT_MODIFICATION, "成员记录已被修改或删除，请刷新后重试");
        }
        log.info("移除成员: memberId={}, removedBy={}", targetMemberId, userId);
    }
}
