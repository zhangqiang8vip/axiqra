package com.axiqra.core.service.impl;

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
import com.axiqra.core.mapper.UserMapper;
import com.axiqra.core.mapper.WorkspaceMapper;
import com.axiqra.core.service.RbacService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceServiceImpl 单元测试")
class WorkspaceServiceImplTest {

    @Mock
    private WorkspaceMapper workspaceMapper;

    @Mock
    private MembershipMapper membershipMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RbacService rbacService;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private static final Long USER_ID = 1L;
    private static final Long WORKSPACE_ID = 100L;
    private static final Long OTHER_USER_ID = 2L;

    @Nested
    @DisplayName("listMyWorkspaces")
    class ListMyWorkspacesTests {

        @Test
        @DisplayName("userId 为 null 应返回空分页")
        void shouldReturnEmptyWhenUserIdIsNull() {
            PageResponse<WorkspaceVO> result = workspaceService.listMyWorkspaces(null);
            assertNotNull(result);
            assertTrue(result.getRecords().isEmpty());
        }

        @Test
        @DisplayName("仅个人空间时返回 owner 的空间")
        void shouldReturnOwnedWorkspaces() {
            when(workspaceMapper.selectByOwnerId(USER_ID)).thenReturn(List.of(
                    createWorkspace(1L, "My Space", WorkspaceType.PERSONAL, USER_ID)
            ));
            when(rbacService.getMemberships(USER_ID)).thenReturn(List.of());

            PageResponse<WorkspaceVO> result = workspaceService.listMyWorkspaces(USER_ID);

            assertEquals(1, result.getTotal());
            assertEquals("owner", result.getRecords().get(0).getMyRole());
        }

        @Test
        @DisplayName("仅成员空间时返回成员空间")
        void shouldReturnMemberWorkspaces() {
            when(workspaceMapper.selectByOwnerId(USER_ID)).thenReturn(List.of());
            MembershipEntity membership = createMembership(1L, USER_ID, WORKSPACE_ID, MemberRole.MEMBER);
            when(rbacService.getMemberships(USER_ID)).thenReturn(List.of(membership));
            when(workspaceMapper.selectByWorkspaceIds(List.of(WORKSPACE_ID))).thenReturn(
                    List.of(createWorkspace(WORKSPACE_ID, "Team Space", WorkspaceType.ORGANIZATION, 999L))
            );

            PageResponse<WorkspaceVO> result = workspaceService.listMyWorkspaces(USER_ID);

            assertEquals(1, result.getTotal());
            assertEquals("member", result.getRecords().get(0).getMyRole());
        }

        @Test
        @DisplayName("owner 空间应在 member 空间之前排序")
        void shouldSortOwnerBeforeMember() {
            when(workspaceMapper.selectByOwnerId(USER_ID)).thenReturn(
                    List.of(createWorkspace(2L, "My Space", WorkspaceType.PERSONAL, USER_ID))
            );
            MembershipEntity membership = createMembership(1L, USER_ID, 1L, MemberRole.MEMBER);
            when(rbacService.getMemberships(USER_ID)).thenReturn(List.of(membership));
            when(workspaceMapper.selectByWorkspaceIds(List.of(1L))).thenReturn(
                    List.of(createWorkspace(1L, "Team Space", WorkspaceType.ORGANIZATION, 999L))
            );

            PageResponse<WorkspaceVO> result = workspaceService.listMyWorkspaces(USER_ID);

            assertEquals(2, result.getTotal());
            assertEquals("owner", result.getRecords().get(0).getMyRole());
            assertEquals("member", result.getRecords().get(1).getMyRole());
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("userId 为 null 应抛参数异常")
        void shouldThrowWhenUserIdIsNull() {
            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.create(null, "personal", "My Space"));
            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("无效 workspaceType 应抛参数异常")
        void shouldThrowWhenWorkspaceTypeIsInvalid() {
            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.create(USER_ID, "invalid_type", "My Space"));
            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("同名空间应抛重复异常")
        void shouldThrowWhenNameDuplicate() {
            when(workspaceMapper.selectByOwnerAndName(USER_ID, "My Space"))
                    .thenReturn(createWorkspace(WORKSPACE_ID, "My Space", WorkspaceType.PERSONAL, USER_ID));

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.create(USER_ID, "personal", "My Space"));
            assertEquals(ErrorCode.DUPLICATE_ENTRY.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非个人空间且 name 为空应抛参数异常")
        void shouldThrowWhenOrgWithNullName() {
            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.create(USER_ID, "organization", null));
            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("个人空间 name 为空应自动使用 username")
        void shouldUseUsernameWhenNameBlank() {
            UserEntity user = new UserEntity();
            user.setId(USER_ID);
            user.setUsername("alice");

            when(workspaceMapper.selectByOwnerAndName(USER_ID, "alice 的空间")).thenReturn(null);
            when(userMapper.selectActiveById(USER_ID)).thenReturn(user);
            when(workspaceMapper.insertSelective(any())).thenAnswer(inv -> {
                WorkspaceEntity w = inv.getArgument(0);
                w.setId(WORKSPACE_ID);
                return 1;
            });
            when(membershipMapper.insertSelective(any())).thenReturn(1);

            WorkspaceVO result = workspaceService.create(USER_ID, "personal", null);

            assertNotNull(result);
            assertEquals("alice 的空间", result.getWorkspaceName());
            ArgumentCaptor<WorkspaceEntity> captor = ArgumentCaptor.forClass(WorkspaceEntity.class);
            verify(workspaceMapper).insertSelective(captor.capture());
            assertEquals("alice 的空间", captor.getValue().getWorkspaceName());
        }

        @Test
        @DisplayName("正常创建应插入 workspace 和 membership")
        void shouldCreateWorkspaceAndMembership() {
            when(workspaceMapper.selectByOwnerAndName(USER_ID, "My Space")).thenReturn(null);
            when(workspaceMapper.insertSelective(any())).thenAnswer(inv -> {
                WorkspaceEntity w = inv.getArgument(0);
                w.setId(WORKSPACE_ID);
                return 1;
            });
            when(membershipMapper.insertSelective(any())).thenReturn(1);

            WorkspaceVO result = workspaceService.create(USER_ID, "personal", "My Space");

            assertNotNull(result);
            assertEquals(WORKSPACE_ID, result.getId());
            assertEquals("owner", result.getMyRole());
            assertEquals(1L, result.getMemberCount());
            verify(membershipMapper).insertSelective(any());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("workspaceId 为 null 应抛参数异常")
        void shouldThrowWhenWorkspaceIdIsNull() {
            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.getById(null, USER_ID));
            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("workspace 不存在应抛资源不存在异常")
        void shouldThrowWhenWorkspaceNotFound() {
            when(workspaceMapper.selectById(WORKSPACE_ID)).thenReturn(null);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.getById(WORKSPACE_ID, USER_ID));
            assertEquals(ErrorCode.WORKSPACE_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非成员无权访问应抛禁止异常")
        void shouldThrowWhenNotMember() {
            when(workspaceMapper.selectById(WORKSPACE_ID))
                    .thenReturn(createWorkspace(WORKSPACE_ID, "My Space", WorkspaceType.PERSONAL, 999L));
            when(rbacService.isMember(USER_ID, WORKSPACE_ID)).thenReturn(false);
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(false);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.getById(WORKSPACE_ID, USER_ID));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("成员访问应返回 workspace 详情")
        void shouldReturnWorkspaceDetailForMember() {
            when(workspaceMapper.selectById(WORKSPACE_ID))
                    .thenReturn(createWorkspace(WORKSPACE_ID, "My Space", WorkspaceType.PERSONAL, 999L));
            when(rbacService.isMember(USER_ID, WORKSPACE_ID)).thenReturn(true);
            when(rbacService.getRole(USER_ID, WORKSPACE_ID)).thenReturn(MemberRole.MEMBER);
            when(membershipMapper.countByWorkspaceId(WORKSPACE_ID)).thenReturn(3L);

            WorkspaceVO result = workspaceService.getById(WORKSPACE_ID, USER_ID);

            assertNotNull(result);
            assertEquals(WORKSPACE_ID, result.getId());
            assertEquals("member", result.getMyRole());
            assertEquals(3L, result.getMemberCount());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("workspaceId 为 null 应抛参数异常")
        void shouldThrowWhenWorkspaceIdIsNull() {
            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.delete(null, USER_ID));
            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非 owner 无权删除应抛禁止异常")
        void shouldThrowWhenNotOwner() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(false);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.delete(WORKSPACE_ID, USER_ID));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("owner 删除应执行软删除")
        void shouldSoftDeleteWhenOwner() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            when(workspaceMapper.selectById(WORKSPACE_ID))
                    .thenReturn(createWorkspace(WORKSPACE_ID, "My Space", WorkspaceType.PERSONAL, USER_ID));
            when(workspaceMapper.softDeleteById(eq(WORKSPACE_ID), eq(1L), any(Instant.class))).thenReturn(1);

            workspaceService.delete(WORKSPACE_ID, USER_ID);

            verify(workspaceMapper).softDeleteById(eq(WORKSPACE_ID), eq(1L), any(Instant.class));
            verify(membershipMapper).softDeleteByWorkspaceId(WORKSPACE_ID, MemberStatus.SUSPENDED.getCode());
        }

        @Test
        @DisplayName("软删除返回 0 行应抛并发修改异常")
        void shouldThrowWhenSoftDeleteReturnsZero() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            when(workspaceMapper.selectById(WORKSPACE_ID))
                    .thenReturn(createWorkspace(WORKSPACE_ID, "My Space", WorkspaceType.PERSONAL, USER_ID));
            when(workspaceMapper.softDeleteById(eq(WORKSPACE_ID), eq(1L), any(Instant.class))).thenReturn(0);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.delete(WORKSPACE_ID, USER_ID));
            assertEquals(ErrorCode.CONCURRENT_MODIFICATION.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("addMember")
    class AddMemberTests {

        @Test
        @DisplayName("非 admin 无权添加成员应抛禁止异常")
        void shouldThrowWhenNotAdmin() {
            when(rbacService.isAdmin(USER_ID, WORKSPACE_ID)).thenReturn(false);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.addMember(WORKSPACE_ID, USER_ID, OTHER_USER_ID, MemberRole.MEMBER));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("添加 owner 角色应抛禁止异常")
        void shouldThrowWhenAddingOwnerRole() {
            when(rbacService.isAdmin(USER_ID, WORKSPACE_ID)).thenReturn(true);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.addMember(WORKSPACE_ID, USER_ID, OTHER_USER_ID, MemberRole.OWNER));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("添加不存在的用户应抛用户不存在异常")
        void shouldThrowWhenUserNotFound() {
            when(rbacService.isAdmin(USER_ID, WORKSPACE_ID)).thenReturn(true);
            when(userMapper.selectActiveById(OTHER_USER_ID)).thenReturn(null);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.addMember(WORKSPACE_ID, USER_ID, OTHER_USER_ID, MemberRole.MEMBER));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("添加已有活跃成员应抛重复异常")
        void shouldThrowWhenMemberAlreadyActive() {
            when(rbacService.isAdmin(USER_ID, WORKSPACE_ID)).thenReturn(true);
            when(userMapper.selectActiveById(OTHER_USER_ID)).thenReturn(createUser(OTHER_USER_ID));
            when(membershipMapper.selectActiveByUserAndWorkspace(OTHER_USER_ID, WORKSPACE_ID))
                    .thenReturn(createMembership(1L, OTHER_USER_ID, WORKSPACE_ID, MemberRole.MEMBER));

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.addMember(WORKSPACE_ID, USER_ID, OTHER_USER_ID, MemberRole.MEMBER));
            assertEquals(ErrorCode.DUPLICATE_ENTRY.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("admin 添加有效成员应成功")
        void shouldAddMemberSuccessfully() {
            when(rbacService.isAdmin(USER_ID, WORKSPACE_ID)).thenReturn(true);
            when(userMapper.selectActiveById(OTHER_USER_ID)).thenReturn(createUser(OTHER_USER_ID));
            when(membershipMapper.selectActiveByUserAndWorkspace(OTHER_USER_ID, WORKSPACE_ID)).thenReturn(null);
            when(membershipMapper.insertSelective(any())).thenAnswer(inv -> {
                MembershipEntity m = inv.getArgument(0);
                m.setId(50L);
                return 1;
            });

            MemberVO result = workspaceService.addMember(WORKSPACE_ID, USER_ID, OTHER_USER_ID, MemberRole.MEMBER);

            assertNotNull(result);
            assertEquals(50L, result.getMemberId());
            assertEquals(OTHER_USER_ID, result.getUserId());
            verify(membershipMapper).insertSelective(any());
        }
    }

    @Nested
    @DisplayName("removeMember")
    class RemoveMemberTests {

        @Test
        @DisplayName("不能移除 owner 应抛禁止异常")
        void shouldThrowWhenRemovingOwner() {
            when(rbacService.isAdmin(USER_ID, WORKSPACE_ID)).thenReturn(true);
            MembershipEntity ownerMembership = createMembership(1L, OTHER_USER_ID, WORKSPACE_ID, MemberRole.OWNER);
            when(membershipMapper.selectById(1L)).thenReturn(ownerMembership);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.removeMember(WORKSPACE_ID, USER_ID, 1L));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("不能移除自己应抛禁止异常")
        void shouldThrowWhenRemovingSelf() {
            when(rbacService.isAdmin(USER_ID, WORKSPACE_ID)).thenReturn(true);
            MembershipEntity selfMembership = createMembership(1L, USER_ID, WORKSPACE_ID, MemberRole.MEMBER);
            when(membershipMapper.selectById(1L)).thenReturn(selfMembership);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.removeMember(WORKSPACE_ID, USER_ID, 1L));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("不能将您自己"));
        }

        @Test
        @DisplayName("admin 移除有效成员应成功")
        void shouldRemoveMemberSuccessfully() {
            when(rbacService.isAdmin(USER_ID, WORKSPACE_ID)).thenReturn(true);
            MembershipEntity member = createMembership(1L, OTHER_USER_ID, WORKSPACE_ID, MemberRole.MEMBER);
            when(membershipMapper.selectById(1L)).thenReturn(member);
            when(membershipMapper.softDelete(eq(1L), eq(MemberStatus.SUSPENDED.getCode()), eq(0L))).thenReturn(1);

            workspaceService.removeMember(WORKSPACE_ID, USER_ID, 1L);

            verify(membershipMapper).softDelete(1L, MemberStatus.SUSPENDED.getCode(), 0L);
        }
    }

    @Nested
    @DisplayName("updateMemberRole")
    class UpdateMemberRoleTests {

        @Test
        @DisplayName("不能修改 owner 角色应抛禁止异常")
        void shouldThrowWhenUpdatingOwnerRole() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            MembershipEntity ownerMembership = createMembership(1L, OTHER_USER_ID, WORKSPACE_ID, MemberRole.OWNER);
            when(membershipMapper.selectById(1L)).thenReturn(ownerMembership);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.updateMemberRole(WORKSPACE_ID, USER_ID, 1L, MemberRole.ADMIN));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("不能将成员提升为 owner 应抛禁止异常")
        void shouldThrowWhenPromotingToOwner() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            MembershipEntity memberMembership = createMembership(1L, OTHER_USER_ID, WORKSPACE_ID, MemberRole.MEMBER);
            when(membershipMapper.selectById(1L)).thenReturn(memberMembership);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.updateMemberRole(WORKSPACE_ID, USER_ID, 1L, MemberRole.OWNER));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("owner 变更成员角色应成功")
        void shouldUpdateMemberRoleSuccessfully() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            MembershipEntity member = createMembership(1L, OTHER_USER_ID, WORKSPACE_ID, MemberRole.MEMBER);
            when(membershipMapper.selectById(1L)).thenReturn(member);

            workspaceService.updateMemberRole(WORKSPACE_ID, USER_ID, 1L, MemberRole.ADMIN);

            verify(membershipMapper).updateRole(1L, MemberRole.ADMIN.getCode());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("workspaceId 为 null 应抛参数异常")
        void shouldThrowWhenWorkspaceIdIsNull() {
            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.update(null, USER_ID, "New Name", "personal"));
            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非 owner 无权更新应抛禁止异常")
        void shouldThrowWhenNotOwner() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(false);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.update(WORKSPACE_ID, USER_ID, "New Name", null));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("workspace 不存在应抛资源不存在异常")
        void shouldThrowWhenWorkspaceNotFound() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            when(workspaceMapper.selectById(WORKSPACE_ID)).thenReturn(null);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.update(WORKSPACE_ID, USER_ID, "New Name", null));
            assertEquals(ErrorCode.WORKSPACE_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("无效 workspaceType 应抛参数异常")
        void shouldThrowWhenWorkspaceTypeIsInvalid() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            when(workspaceMapper.selectById(WORKSPACE_ID))
                    .thenReturn(createWorkspace(WORKSPACE_ID, "My Space", WorkspaceType.PERSONAL, USER_ID));

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.update(WORKSPACE_ID, USER_ID, null, "invalid_type"));
            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("同名空间应抛重复异常")
        void shouldThrowWhenNameDuplicate() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            when(workspaceMapper.selectById(WORKSPACE_ID))
                    .thenReturn(createWorkspace(WORKSPACE_ID, "Old Name", WorkspaceType.PERSONAL, USER_ID));
            when(workspaceMapper.selectByOwnerAndName(USER_ID, "New Name"))
                    .thenReturn(createWorkspace(999L, "New Name", WorkspaceType.PERSONAL, USER_ID));

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.update(WORKSPACE_ID, USER_ID, "New Name", null));
            assertEquals(ErrorCode.DUPLICATE_ENTRY.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("name 和 type 均为空应直接返回当前 workspace")
        void shouldReturnCurrentWhenBothFieldsBlank() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            WorkspaceEntity existing = createWorkspace(WORKSPACE_ID, "My Space", WorkspaceType.PERSONAL, USER_ID);
            when(workspaceMapper.selectById(WORKSPACE_ID)).thenReturn(existing);
            when(rbacService.getRole(USER_ID, WORKSPACE_ID)).thenReturn(MemberRole.OWNER);
            when(membershipMapper.countByWorkspaceId(WORKSPACE_ID)).thenReturn(1L);

            WorkspaceVO result = workspaceService.update(WORKSPACE_ID, USER_ID, null, null);

            assertNotNull(result);
            assertEquals("My Space", result.getWorkspaceName());
            verify(workspaceMapper, never()).updateSelective(anyLong(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("混排/带空格的工作空间类型应持久化规范 code 而非原始字符串")
        void shouldPersistCanonicalTypeCodeForMixedCaseInput() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            WorkspaceEntity existing = createWorkspace(WORKSPACE_ID, "My Space", WorkspaceType.PERSONAL, USER_ID);
            WorkspaceEntity updated = createWorkspace(WORKSPACE_ID, "My Space", WorkspaceType.ORGANIZATION, USER_ID);
            when(workspaceMapper.selectById(WORKSPACE_ID)).thenReturn(existing, updated);
            lenient().when(workspaceMapper.updateSelective(eq(WORKSPACE_ID), isNull(), eq("personal"), any(), any())).thenReturn(1);
            when(membershipMapper.countByWorkspaceId(WORKSPACE_ID)).thenReturn(0L);

            WorkspaceVO result = workspaceService.update(WORKSPACE_ID, USER_ID, null, " Personal ");

            assertNotNull(result);
            verify(workspaceMapper).updateSelective(eq(WORKSPACE_ID), isNull(), eq("personal"), any(), any());
        }

        @Test
        @DisplayName("正常更新 workspaceName 应成功")
        void shouldUpdateNameSuccessfully() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            WorkspaceEntity existing = createWorkspace(WORKSPACE_ID, "Old Name", WorkspaceType.PERSONAL, USER_ID);
            WorkspaceEntity updated = createWorkspace(WORKSPACE_ID, "New Name", WorkspaceType.PERSONAL, USER_ID);
            when(workspaceMapper.selectById(WORKSPACE_ID)).thenReturn(existing, updated);
            when(workspaceMapper.selectByOwnerAndName(USER_ID, "New Name")).thenReturn(null);
            when(workspaceMapper.updateSelective(eq(WORKSPACE_ID), eq("New Name"), isNull(), any(), any())).thenReturn(1);
            when(membershipMapper.countByWorkspaceId(WORKSPACE_ID)).thenReturn(1L);

            WorkspaceVO result = workspaceService.update(WORKSPACE_ID, USER_ID, "New Name", null);

            assertNotNull(result);
            assertEquals("New Name", result.getWorkspaceName());
            assertEquals("owner", result.getMyRole());
        }

        @Test
        @DisplayName("数据库更新返回 0 行应抛资源不存在异常")
        void shouldThrowWhenUpdateReturnsZero() {
            when(rbacService.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);
            WorkspaceEntity existing = createWorkspace(WORKSPACE_ID, "Old Name", WorkspaceType.PERSONAL, USER_ID);
            when(workspaceMapper.selectById(WORKSPACE_ID)).thenReturn(existing);
            when(workspaceMapper.selectByOwnerAndName(USER_ID, "New Name")).thenReturn(null);
            when(workspaceMapper.updateSelective(eq(WORKSPACE_ID), eq("New Name"), isNull(), any(), any())).thenReturn(0);

            BizException ex = assertThrows(BizException.class,
                    () -> workspaceService.update(WORKSPACE_ID, USER_ID, "New Name", null));
            assertEquals(ErrorCode.CONCURRENT_MODIFICATION.getCode(), ex.getCode());
        }
    }

    // ==================== 辅助方法 ====================

    private WorkspaceEntity createWorkspace(Long id, String name, WorkspaceType type, Long ownerId) {
        WorkspaceEntity e = new WorkspaceEntity();
        e.setId(id);
        e.setWorkspaceName(name);
        e.setWorkspaceType(type);
        e.setOwnerId(ownerId);
        e.setDeleted(false);
        e.setVersion(1L);
        e.setGmtCreate(Instant.now());
        return e;
    }

    private MembershipEntity createMembership(Long id, Long userId, Long workspaceId, MemberRole role) {
        MembershipEntity m = new MembershipEntity();
        m.setId(id);
        m.setUserId(userId);
        m.setWorkspaceId(workspaceId);
        m.setRole(role.getCode());
        m.setStatus(MemberStatus.ACTIVE.getCode());
        m.setGmtCreate(Instant.now());
        m.setVersion(0L);
        return m;
    }

    private UserEntity createUser(Long id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setUsername("user_" + id);
        u.setNickname("User " + id);
        u.setEmail("user" + id + "@test.com");
        return u;
    }
}
