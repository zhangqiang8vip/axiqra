package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.domain.enums.MemberStatus;
import com.axiqra.common.domain.vo.NavItemVO;
import com.axiqra.common.domain.vo.NavResponseVO;
import com.axiqra.core.mapper.WorkspaceMapper;
import com.axiqra.core.service.NavService;
import com.axiqra.core.service.RbacService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NavServiceImpl 单元测试")
class NavServiceImplTest {

    @Mock
    private RbacService rbacService;

    @Mock
    private WorkspaceMapper workspaceMapper;

    private NavService navService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        navService = new NavServiceImpl(rbacService, workspaceMapper);
        when(workspaceMapper.selectByWorkspaceIds(anyList())).thenReturn(Collections.emptyList());
    }

    @Nested
    @DisplayName("getNav")
    class GetNavTests {

        @Test
        @DisplayName("userId 为 null 应返回空导航")
        void shouldReturnEmptyNavWhenUserIdIsNull() {
            NavResponseVO result = navService.getNav(null);

            assertNotNull(result);
            assertTrue(result.getBaseUserNav().isEmpty());
            assertTrue(result.getSpaceMembershipNav().isEmpty());
        }

        @Test
        @DisplayName("登录用户应返回基础菜单")
        void shouldReturnBaseNavForLoggedInUser() {
            NavResponseVO result = navService.getNav(USER_ID);

            assertNotNull(result);
            assertFalse(result.getBaseUserNav().isEmpty());

            List<String> baseIds = result.getBaseUserNav().stream()
                    .map(NavItemVO::getId)
                    .toList();
            assertTrue(baseIds.contains("home"));
            assertTrue(baseIds.contains("profile"));
            assertTrue(baseIds.contains("workspaces"));
        }

        @Test
        @DisplayName("无成员空间的用户应返回空 spaceMembershipNav")
        void shouldReturnEmptySpaceNavWhenNoMemberships() {
            when(rbacService.getMemberships(USER_ID)).thenReturn(List.of());

            NavResponseVO result = navService.getNav(USER_ID);

            assertNotNull(result.getSpaceMembershipNav());
            assertTrue(result.getSpaceMembershipNav().isEmpty());
        }

        @Test
        @DisplayName("admin 成员应有成员管理菜单")
        void shouldIncludeMemberManagementForAdmin() {
            MembershipEntity adminMembership = createMembership(1L, USER_ID, 100L,
                    MemberRole.ADMIN.getCode());
            when(rbacService.getMemberships(USER_ID)).thenReturn(List.of(adminMembership));

            NavResponseVO result = navService.getNav(USER_ID);

            List<String> ids = result.getSpaceMembershipNav().stream()
                    .map(NavItemVO::getId)
                    .toList();
            assertTrue(ids.contains("workspace-100"));
            assertTrue(ids.contains("workspace-100-members"));
        }

        @Test
        @DisplayName("owner 成员应有成员管理菜单")
        void shouldIncludeMemberManagementForOwner() {
            MembershipEntity ownerMembership = createMembership(1L, USER_ID, 200L,
                    MemberRole.OWNER.getCode());
            when(rbacService.getMemberships(USER_ID)).thenReturn(List.of(ownerMembership));

            NavResponseVO result = navService.getNav(USER_ID);

            List<String> ids = result.getSpaceMembershipNav().stream()
                    .map(NavItemVO::getId)
                    .toList();
            assertTrue(ids.contains("workspace-200"));
            assertTrue(ids.contains("workspace-200-members"));
        }

        @Test
        @DisplayName("viewer 成员不应有成员管理菜单")
        void shouldNotIncludeMemberManagementForViewer() {
            MembershipEntity viewerMembership = createMembership(1L, USER_ID, 300L,
                    MemberRole.VIEWER.getCode());
            when(rbacService.getMemberships(USER_ID)).thenReturn(List.of(viewerMembership));

            NavResponseVO result = navService.getNav(USER_ID);

            List<String> ids = result.getSpaceMembershipNav().stream()
                    .map(NavItemVO::getId)
                    .toList();
            assertTrue(ids.contains("workspace-300"));
            assertFalse(ids.contains("workspace-300-members"));
        }

        @Test
        @DisplayName("有 search scope 的用户应返回搜索菜单")
        void shouldReturnSearchMenuWhenScopeGranted() {
            when(rbacService.getMemberships(USER_ID)).thenReturn(List.of());
            when(rbacService.hasScope(USER_ID, "search")).thenReturn(true);
            when(rbacService.hasScope(USER_ID, "solution")).thenReturn(false);
            when(rbacService.hasScope(USER_ID, "trace")).thenReturn(false);

            NavResponseVO result = navService.getNav(USER_ID);

            List<String> scopeIds = result.getGrantedScopeNav().stream()
                    .map(NavItemVO::getId)
                    .toList();
            assertTrue(scopeIds.contains("search"));
            assertFalse(scopeIds.contains("solutions"));
        }

        @Test
        @DisplayName("无任何 scope 的用户应返回空 grantedScopeNav")
        void shouldReturnEmptyScopeNavWhenNoScopes() {
            when(rbacService.getMemberships(USER_ID)).thenReturn(List.of());
            when(rbacService.hasScope(USER_ID, "search")).thenReturn(false);
            when(rbacService.hasScope(USER_ID, "solution")).thenReturn(false);
            when(rbacService.hasScope(USER_ID, "trace")).thenReturn(false);

            NavResponseVO result = navService.getNav(USER_ID);

            assertNotNull(result.getGrantedScopeNav());
            assertTrue(result.getGrantedScopeNav().isEmpty());
        }

        @Test
        @DisplayName("治理菜单和管理菜单 S1 为空列表")
        void shouldReturnEmptyGovernanceAndAdminNav() {
            NavResponseVO result = navService.getNav(USER_ID);

            assertNotNull(result.getGovernanceNav());
            assertNotNull(result.getAdminNav());
            assertTrue(result.getGovernanceNav().isEmpty());
            assertTrue(result.getAdminNav().isEmpty());
        }

        @Test
        @DisplayName("memberships 返回 null 应不抛异常")
        void shouldNotThrowWhenMembershipsIsNull() {
            when(rbacService.getMemberships(USER_ID)).thenReturn(null);

            NavResponseVO result = navService.getNav(USER_ID);

            assertNotNull(result);
            assertTrue(result.getSpaceMembershipNav().isEmpty());
        }

        @Test
        @DisplayName("memberships 包含 null 元素应跳过")
        void shouldSkipNullMemberships() {
            List<MembershipEntity> listWithNull = new ArrayList<>();
            listWithNull.add(null);
            when(rbacService.getMemberships(USER_ID)).thenReturn(listWithNull);

            NavResponseVO result = navService.getNav(USER_ID);

            assertNotNull(result);
            assertTrue(result.getSpaceMembershipNav().isEmpty());
        }

        @Test
        @DisplayName("SUSPENDED 成员不应出现在 spaceMembershipNav")
        void shouldExcludeSuspendedMemberships() {
            MembershipEntity activeMembership = createMembership(1L, USER_ID, 100L,
                    MemberRole.ADMIN.getCode());
            MembershipEntity suspendedMembership = createMembership(2L, USER_ID, 200L,
                    MemberRole.OWNER.getCode());
            suspendedMembership.setStatus(MemberStatus.SUSPENDED.getCode());

            when(rbacService.getMemberships(USER_ID))
                    .thenReturn(List.of(activeMembership, suspendedMembership));

            NavResponseVO result = navService.getNav(USER_ID);

            List<String> ids = result.getSpaceMembershipNav().stream()
                    .map(NavItemVO::getId)
                    .toList();
            assertTrue(ids.contains("workspace-100"));
            assertFalse(ids.contains("workspace-200"));
        }
    }

    private MembershipEntity createMembership(Long id, Long userId, Long workspaceId, String role) {
        MembershipEntity m = new MembershipEntity();
        m.setId(id);
        m.setUserId(userId);
        m.setWorkspaceId(workspaceId);
        m.setRole(role);
        m.setStatus(MemberStatus.ACTIVE.getCode());
        return m;
    }
}
