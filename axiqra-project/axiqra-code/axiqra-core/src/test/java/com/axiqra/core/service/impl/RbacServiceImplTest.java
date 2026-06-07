package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.port.PolicyEnginePort;
import com.axiqra.common.port.RbacPort;
import com.axiqra.core.service.RbacService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RbacServiceImpl 单元测试")
class RbacServiceImplTest {

    @Mock
    private RbacPort rbacPort;

    @Mock
    private PolicyEnginePort policyEnginePort;

    @InjectMocks
    private RbacServiceImpl rbacService;

    private static final Long USER_ID = 1L;
    private static final Long WORKSPACE_ID = 100L;

    @Nested
    @DisplayName("hasRole")
    class HasRoleTests {

        @Test
        @DisplayName("应代理到 rbacPort.hasRole")
        void shouldDelegateToRbacPort() {
            when(rbacPort.hasRole(USER_ID, WORKSPACE_ID, MemberRole.ADMIN)).thenReturn(true);

            boolean result = rbacService.hasRole(USER_ID, WORKSPACE_ID, MemberRole.ADMIN);

            assertTrue(result);
            verify(rbacPort).hasRole(USER_ID, WORKSPACE_ID, MemberRole.ADMIN);
        }

        @Test
        @DisplayName("应返回 rbacPort 的结果")
        void shouldReturnFalseWhenPortReturnsFalse() {
            when(rbacPort.hasRole(USER_ID, WORKSPACE_ID, MemberRole.OWNER)).thenReturn(false);

            boolean result = rbacService.hasRole(USER_ID, WORKSPACE_ID, MemberRole.OWNER);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("isOwner")
    class IsOwnerTests {

        @Test
        @DisplayName("应代理到 rbacPort.isOwner")
        void shouldDelegateToRbacPort() {
            when(rbacPort.isOwner(USER_ID, WORKSPACE_ID)).thenReturn(true);

            boolean result = rbacService.isOwner(USER_ID, WORKSPACE_ID);

            assertTrue(result);
            verify(rbacPort).isOwner(USER_ID, WORKSPACE_ID);
        }
    }

    @Nested
    @DisplayName("isAdmin")
    class IsAdminTests {

        @Test
        @DisplayName("应代理到 rbacPort.isAdmin")
        void shouldDelegateToRbacPort() {
            when(rbacPort.isAdmin(USER_ID, WORKSPACE_ID)).thenReturn(false);

            boolean result = rbacService.isAdmin(USER_ID, WORKSPACE_ID);

            assertFalse(result);
            verify(rbacPort).isAdmin(USER_ID, WORKSPACE_ID);
        }
    }

    @Nested
    @DisplayName("getRole")
    class GetRoleTests {

        @Test
        @DisplayName("应返回 rbacPort.getRole 的结果")
        void shouldReturnRoleFromPort() {
            when(rbacPort.getRole(USER_ID, WORKSPACE_ID)).thenReturn(MemberRole.MEMBER);

            MemberRole result = rbacService.getRole(USER_ID, WORKSPACE_ID);

            assertEquals(MemberRole.MEMBER, result);
        }

        @Test
        @DisplayName("无成员关系应返回 null")
        void shouldReturnNullWhenNoMembership() {
            when(rbacPort.getRole(USER_ID, WORKSPACE_ID)).thenReturn(null);

            MemberRole result = rbacService.getRole(USER_ID, WORKSPACE_ID);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("getMemberships")
    class GetMembershipsTests {

        @Test
        @DisplayName("应返回 rbacPort.getMemberships 的结果")
        void shouldReturnMembershipsFromPort() {
            MembershipEntity m = new MembershipEntity();
            m.setId(1L);
            m.setUserId(USER_ID);
            m.setWorkspaceId(WORKSPACE_ID);
            when(rbacPort.getMemberships(USER_ID)).thenReturn(List.of(m));

            List<MembershipEntity> result = rbacService.getMemberships(USER_ID);

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId());
        }
    }

    @Nested
    @DisplayName("isMember")
    class IsMemberTests {

        @Test
        @DisplayName("应代理到 rbacPort.isMember")
        void shouldDelegateToRbacPort() {
            when(rbacPort.isMember(USER_ID, WORKSPACE_ID)).thenReturn(true);

            boolean result = rbacService.isMember(USER_ID, WORKSPACE_ID);

            assertTrue(result);
            verify(rbacPort).isMember(USER_ID, WORKSPACE_ID);
        }
    }

    @Nested
    @DisplayName("hasScope")
    class HasScopeTests {

        @Test
        @DisplayName("应代理到 policyEnginePort.hasScope")
        void shouldDelegateToPolicyEnginePort() {
            when(policyEnginePort.hasScope(USER_ID, "connect:write")).thenReturn(true);

            boolean result = rbacService.hasScope(USER_ID, "connect:write");

            assertTrue(result);
            verify(policyEnginePort).hasScope(USER_ID, "connect:write");
        }

        @Test
        @DisplayName("应返回 policyEnginePort 的结果")
        void shouldReturnFalseWhenPortReturnsFalse() {
            when(policyEnginePort.hasScope(USER_ID, "admin:all")).thenReturn(false);

            boolean result = rbacService.hasScope(USER_ID, "admin:all");

            assertFalse(result);
        }
    }
}
