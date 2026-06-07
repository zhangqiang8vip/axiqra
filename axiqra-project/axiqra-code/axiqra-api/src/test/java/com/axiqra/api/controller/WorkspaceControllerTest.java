package com.axiqra.api.controller;

import com.axiqra.common.domain.dto.MemberRoleUpdateRequest;
import com.axiqra.common.domain.dto.WorkspaceCreateRequest;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.domain.enums.WorkspaceType;
import com.axiqra.common.domain.vo.MemberVO;
import com.axiqra.common.domain.vo.PageResponse;
import com.axiqra.common.domain.vo.WorkspaceVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.WorkspaceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceController 接口测试")
class WorkspaceControllerTest {

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private WorkspaceController controller;

    private MockedStatic<cn.dev33.satoken.stp.StpUtil> stpUtilMock;

    private static final Long USER_ID = 1L;
    private static final Long WORKSPACE_ID = 100L;

    @BeforeEach
    void setUp() {
        stpUtilMock = mockStatic(cn.dev33.satoken.stp.StpUtil.class);
        stpUtilMock.when(cn.dev33.satoken.stp.StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Nested
    @DisplayName("POST /api/workspaces")
    class CreateTests {

        @Test
        @DisplayName("应调用 service.create 并返回 workspace")
        void shouldCallServiceCreate() {
            WorkspaceVO workspace = createWorkspaceVO(WORKSPACE_ID, "My Space", "personal");
            when(workspaceService.create(any(), any(), any())).thenReturn(workspace);

            WorkspaceCreateRequest request = new WorkspaceCreateRequest();
            request.setWorkspaceType("personal");
            request.setWorkspaceName("My Space");

            var response = controller.create(request);

            assertNotNull(response.getData());
            assertEquals(WORKSPACE_ID, response.getData().getId());
            verify(workspaceService).create(eq(USER_ID), eq("personal"), eq("My Space"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/workspaces/{workspaceId}")
    class DeleteTests {

        @Test
        @DisplayName("应调用 service.delete")
        void shouldCallServiceDelete() {
            doNothing().when(workspaceService).delete(any(), any());

            controller.delete(WORKSPACE_ID);

            verify(workspaceService).delete(eq(WORKSPACE_ID), eq(USER_ID));
        }
    }

    @Nested
    @DisplayName("POST /api/workspaces/{workspaceId}/members")
    class AddMemberTests {

        @Test
        @DisplayName("无效角色应抛 BizException")
        void shouldThrowWhenRoleInvalid() {
            BizException ex = assertThrows(BizException.class,
                    () -> controller.addMember(WORKSPACE_ID, 2L, "invalid_role"));
            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("有效参数应调用 service.addMember")
        void shouldCallServiceAddMember() {
            MemberVO member = createMemberVO(1L, USER_ID, WORKSPACE_ID, "member");
            when(workspaceService.addMember(any(), any(), any(), any())).thenReturn(member);

            var result = controller.addMember(WORKSPACE_ID, 2L, "member");

            assertNotNull(result.getData());
            verify(workspaceService).addMember(eq(WORKSPACE_ID), eq(USER_ID), eq(2L), eq(MemberRole.MEMBER));
        }
    }

    @Nested
    @DisplayName("DELETE /api/workspaces/{workspaceId}/members/{memberId}")
    class RemoveMemberTests {

        @Test
        @DisplayName("应调用 service.removeMember")
        void shouldCallServiceRemoveMember() {
            doNothing().when(workspaceService).removeMember(any(), any(), any());

            controller.removeMember(WORKSPACE_ID, 1L);

            verify(workspaceService).removeMember(eq(WORKSPACE_ID), eq(USER_ID), eq(1L));
        }
    }

    @Nested
    @DisplayName("PUT /api/workspaces/{workspaceId}/members")
    class UpdateMemberRoleTests {

        @Test
        @DisplayName("应调用 service.updateMemberRole")
        void shouldCallServiceUpdateMemberRole() {
            MemberRoleUpdateRequest request = new MemberRoleUpdateRequest();
            request.setMemberId(1L);
            request.setRole(MemberRole.ADMIN);

            doNothing().when(workspaceService).updateMemberRole(any(), any(), any(), any());

            controller.updateMemberRole(WORKSPACE_ID, request);

            verify(workspaceService).updateMemberRole(eq(WORKSPACE_ID), eq(USER_ID), eq(1L), eq(MemberRole.ADMIN));
        }
    }

    // ==================== 辅助方法 ====================

    private WorkspaceVO createWorkspaceVO(Long id, String name, String type) {
        return WorkspaceVO.builder()
                .id(id)
                .workspaceName(name)
                .workspaceType(WorkspaceType.of(type))
                .ownerId(USER_ID)
                .memberCount(1L)
                .myRole("owner")
                .gmtCreate(Instant.now())
                .build();
    }

    private MemberVO createMemberVO(Long memberId, Long userId, Long workspaceId, String role) {
        return MemberVO.builder()
                .memberId(memberId)
                .userId(userId)
                .workspaceId(workspaceId)
                .role(MemberRole.of(role))
                .build();
    }
}
