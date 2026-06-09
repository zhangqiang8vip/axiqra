package com.axiqra.api.controller;

import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.UserService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 接口测试")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    private MockedStatic<cn.dev33.satoken.stp.StpUtil> stpUtilMock;

    private static final Long USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

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
    @DisplayName("GET /users/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("当前用户存在时应返回完整用户信息")
        void shouldReturnCurrentUserInfo() {
            UserEntity user = createUser(USER_ID, "alice", "Alice", "alice@example.com");
            when(userService.getById(USER_ID)).thenReturn(user);

            var response = controller.getCurrentUser();

            assertNotNull(response);
            assertNotNull(response.getData());
            assertEquals(USER_ID, response.getData().getUserId());
            assertEquals("alice", response.getData().getUsername());
            assertEquals("alice@example.com", response.getData().getEmail());
            verify(userService).getById(USER_ID);
        }

        @Test
        @DisplayName("当前用户不存在时应抛统一 USER_NOT_FOUND 异常")
        void shouldThrowUserNotFoundWhenCurrentUserMissing() {
            when(userService.getById(USER_ID)).thenReturn(null);

            BizException exception = assertThrows(BizException.class, () -> controller.getCurrentUser());

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
            verify(userService).getById(USER_ID);
        }
    }

    @Nested
    @DisplayName("GET /users/{userId}")
    class GetUserByIdTests {

        @Test
        @DisplayName("目标用户存在时应返回公开用户信息")
        void shouldReturnPublicUserInfo() {
            UserEntity user = createUser(TARGET_USER_ID, "bob", "Bob", "bob@example.com");
            when(userService.getById(TARGET_USER_ID)).thenReturn(user);

            var response = controller.getUserById(TARGET_USER_ID);

            assertNotNull(response);
            assertNotNull(response.getData());
            assertEquals(TARGET_USER_ID, response.getData().getUserId());
            assertEquals("bob", response.getData().getUsername());
            assertEquals("Bob", response.getData().getNickname());
            verify(userService).getById(TARGET_USER_ID);
        }

        @Test
        @DisplayName("目标用户不存在时应抛统一 USER_NOT_FOUND 异常")
        void shouldThrowUserNotFoundWhenTargetUserMissing() {
            when(userService.getById(TARGET_USER_ID)).thenReturn(null);

            BizException exception = assertThrows(BizException.class,
                    () -> controller.getUserById(TARGET_USER_ID));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
            verify(userService).getById(TARGET_USER_ID);
        }
    }

    private UserEntity createUser(Long id, String username, String nickname, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setEmail(email);
        return user;
    }
}
