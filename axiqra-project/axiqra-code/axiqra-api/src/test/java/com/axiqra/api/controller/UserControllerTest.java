package com.axiqra.api.controller;

import com.axiqra.common.domain.entity.UserEntity;
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

import java.util.List;

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
    @DisplayName("GET /users/search")
    class SearchUsersTests {

        @Test
        @DisplayName("用户名存在时应返回用户列表")
        void shouldReturnUsersWhenFound() {
            UserEntity user = createUser(USER_ID, "alice", "Alice", null);
            when(userService.getByUsername("alice")).thenReturn(user);

            var response = controller.searchUsers("alice");

            assertNotNull(response);
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            assertEquals(USER_ID, response.getData().get(0).id());
            assertEquals("alice", response.getData().get(0).username());
        }

        @Test
        @DisplayName("用户不存在时应返回空列表")
        void shouldReturnEmptyListWhenNotFound() {
            when(userService.getByUsername("nonexistent")).thenReturn(null);

            var response = controller.searchUsers("nonexistent");

            assertNotNull(response);
            assertNotNull(response.getData());
            assertTrue(response.getData().isEmpty());
        }

        @Test
        @DisplayName("空字符串应返回空列表")
        void shouldReturnEmptyListForBlankInput() {
            var response = controller.searchUsers("");

            assertNotNull(response);
            assertNotNull(response.getData());
            assertTrue(response.getData().isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /users/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("当前用户存在时应返回用户信息")
        void shouldReturnCurrentUserInfo() {
            UserEntity user = createUser(USER_ID, "alice", "Alice", "alice@example.com");
            when(userService.getById(USER_ID)).thenReturn(user);

            var response = controller.getCurrentUser();

            assertNotNull(response);
            assertNotNull(response.getData());
            assertEquals(USER_ID, response.getData().id());
            assertEquals("alice", response.getData().username());
            verify(userService).getById(USER_ID);
        }

        @Test
        @DisplayName("当前用户不存在时应返回 null")
        void shouldReturnNullWhenUserNotFound() {
            when(userService.getById(USER_ID)).thenReturn(null);

            var response = controller.getCurrentUser();

            assertNotNull(response);
            assertNull(response.getData());
            verify(userService).getById(USER_ID);
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
