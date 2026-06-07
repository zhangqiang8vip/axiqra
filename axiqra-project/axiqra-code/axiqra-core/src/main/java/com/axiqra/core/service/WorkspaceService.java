package com.axiqra.core.service;

import com.axiqra.common.domain.entity.WorkspaceEntity;
import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.domain.vo.PageResponse;
import com.axiqra.common.domain.vo.WorkspaceVO;
import com.axiqra.common.domain.vo.MemberVO;

/**
 * Workspace 工作空间服务接口
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
public interface WorkspaceService {

    /**
     * 获取当前用户可见的所有工作空间（累加式：个人空间 + 成员空间）
     */
    PageResponse<WorkspaceVO> listMyWorkspaces(Long userId);

    /**
     * 创建工作空间（同时将自己设为 owner）
     */
    WorkspaceVO create(Long userId, String workspaceType, String workspaceName);

    /**
     * 获取工作空间详情
     */
    WorkspaceVO getById(Long workspaceId, Long userId);

    /**
     * 更新工作空间（仅 owner 可操作）
     */
    WorkspaceVO update(Long workspaceId, Long userId, String workspaceName, String workspaceType);

    /**
     * 删除工作空间（仅 owner 可操作）
     */
    void delete(Long workspaceId, Long userId);

    // ==================== 成员管理 ====================

    /**
     * 获取工作空间成员列表
     */
    PageResponse<MemberVO> listMembers(Long workspaceId, Long userId);

    /**
     * 添加成员到工作空间（admin/owner 可操作）
     */
    MemberVO addMember(Long workspaceId, Long userId, Long targetUserId, MemberRole role);

    /**
     * 更新成员角色（仅 owner 可操作）
     */
    void updateMemberRole(Long workspaceId, Long userId, Long memberId, MemberRole newRole);

    /**
     * 移除成员（admin/owner 可操作，不能移除 owner）
     */
    void removeMember(Long workspaceId, Long userId, Long targetMemberId);
}
