package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.annotation.RequireWorkspaceRole;
import com.axiqra.api.annotation.WorkspaceRole;
import com.axiqra.common.domain.dto.MemberQueryRequest;
import com.axiqra.common.domain.dto.MemberRoleUpdateRequest;
import com.axiqra.common.domain.dto.WorkspaceCreateRequest;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.domain.vo.MemberVO;
import com.axiqra.common.domain.vo.PageResponse;
import com.axiqra.common.domain.vo.WorkspaceVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Workspace 工作空间控制器
 *
 * <p>提供工作空间的 CRUD 和成员管理接口。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
@Tag(name = "工作空间", description = "工作空间 CRUD + 成员管理")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping
    @Operation(summary = "我的工作空间列表", description = "获取当前用户可见的所有工作空间（个人空间 + 成员空间）")
    public ApiResponse<PageResponse<WorkspaceVO>> listMyWorkspaces() {
        long userId = StpUtil.getLoginIdAsLong();
        PageResponse<WorkspaceVO> result = workspaceService.listMyWorkspaces(userId);
        return ApiResponse.ok(result);
    }

    @PostMapping
    @Operation(summary = "创建工作空间", description = "创建新工作空间，自动将自己设为 owner")
    public ApiResponse<WorkspaceVO> create(@Valid @RequestBody WorkspaceCreateRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        WorkspaceVO workspace = workspaceService.create(
                userId,
                request.getWorkspaceType(),
                request.getWorkspaceName()
        );
        log.info("创建工作空间: userId={}, workspaceId={}", userId, workspace.getId());
        return ApiResponse.ok(workspace);
    }

    @GetMapping("/{workspaceId}")
    @Operation(summary = "工作空间详情", description = "获取指定工作空间详情，需为成员")
    public ApiResponse<WorkspaceVO> getById(@PathVariable Long workspaceId) {
        long userId = StpUtil.getLoginIdAsLong();
        WorkspaceVO workspace = workspaceService.getById(workspaceId, userId);
        return ApiResponse.ok(workspace);
    }

    @DeleteMapping("/{workspaceId}")
    @Operation(summary = "删除工作空间", description = "仅 owner 可删除")
    @RequireWorkspaceRole(role = WorkspaceRole.OWNER, workspaceParam = "workspaceId")
    public ApiResponse<Void> delete(@PathVariable Long workspaceId) {
        long userId = StpUtil.getLoginIdAsLong();
        workspaceService.delete(workspaceId, userId);
        log.info("删除工作空间: workspaceId={}, deletedBy={}", workspaceId, userId);
        return ApiResponse.ok();
    }

    // ==================== 成员管理 ====================

    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "成员列表", description = "获取工作空间成员列表，任意成员可查看")
    public ApiResponse<PageResponse<MemberVO>> listMembers(@PathVariable Long workspaceId) {
        long userId = StpUtil.getLoginIdAsLong();
        PageResponse<MemberVO> members = workspaceService.listMembers(workspaceId, userId);
        return ApiResponse.ok(members);
    }

    @PostMapping("/{workspaceId}/members")
    @Operation(summary = "添加成员", description = "admin 或 owner 可添加成员")
    @RequireWorkspaceRole(role = WorkspaceRole.ADMIN, workspaceParam = "workspaceId")
    public ApiResponse<MemberVO> addMember(
            @PathVariable Long workspaceId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "member") String role) {
        long currentUserId = StpUtil.getLoginIdAsLong();
        MemberRole memberRole = MemberRole.of(role);
        if (memberRole == null) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "无效的角色: " + role + "，可选值: member, admin");
        }
        MemberVO member = workspaceService.addMember(workspaceId, currentUserId, userId, memberRole);
        log.info("添加成员: workspaceId={}, targetUserId={}, role={}", workspaceId, userId, role);
        return ApiResponse.ok(member);
    }

    @PutMapping("/{workspaceId}/members")
    @Operation(summary = "更新成员角色", description = "仅 owner 可更新成员角色")
    @RequireWorkspaceRole(role = WorkspaceRole.OWNER, workspaceParam = "workspaceId")
    public ApiResponse<Void> updateMemberRole(
            @PathVariable Long workspaceId,
            @Valid @RequestBody MemberRoleUpdateRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        MemberRole newRole = request.getRole();
        workspaceService.updateMemberRole(workspaceId, userId, request.getMemberId(), newRole);
        log.info("更新成员角色: workspaceId={}, memberId={}, newRole={}", workspaceId, request.getMemberId(), newRole);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    @Operation(summary = "移除成员", description = "admin 或 owner 可移除成员（不能移除 owner）")
    @RequireWorkspaceRole(role = WorkspaceRole.ADMIN, workspaceParam = "workspaceId")
    public ApiResponse<Void> removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId) {
        long userId = StpUtil.getLoginIdAsLong();
        workspaceService.removeMember(workspaceId, userId, memberId);
        log.info("移除成员: workspaceId={}, memberId={}, removedBy={}", workspaceId, memberId, userId);
        return ApiResponse.ok();
    }
}
