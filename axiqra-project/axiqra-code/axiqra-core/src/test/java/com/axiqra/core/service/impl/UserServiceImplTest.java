package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.util.PasswordHashUtil;
import com.axiqra.core.mapper.UserMapper;
import com.axiqra.core.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 单元测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordHashUtil passwordHashUtil;

    private UserService userService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper, passwordHashUtil);
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfileTests {

        @Test
        @DisplayName("userId 为 null 应抛参数异常")
        void shouldThrowWhenUserIdIsNull() {
            BizException ex = assertThrows(BizException.class,
                    () -> userService.updateProfile(null, "nick", "email@test.com"));
            assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("用户不存在应抛用户不存在异常")
        void shouldThrowWhenUserNotFound() {
            when(userMapper.selectActiveById(USER_ID)).thenReturn(null);

            BizException ex = assertThrows(BizException.class,
                    () -> userService.updateProfile(USER_ID, "nick", "email@test.com"));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("nickname 和 email 均为空应直接返回现有用户")
        void shouldReturnExistingWhenBothBlank() {
            UserEntity existing = createUser(USER_ID, "alice");
            when(userMapper.selectActiveById(USER_ID)).thenReturn(existing);

            UserEntity result = userService.updateProfile(USER_ID, null, null);

            assertNotNull(result);
            assertEquals("alice", result.getUsername());
            verify(userMapper, never()).updateSelective(anyLong(), any(), any());
        }

        @Test
        @DisplayName("email 被其他用户占用应抛重复异常")
        void shouldThrowWhenEmailAlreadyTaken() {
            UserEntity existing = createUser(USER_ID, "alice");
            when(userMapper.selectActiveById(USER_ID)).thenReturn(existing);
            UserEntity anotherUser = createUser(999L, "bob");
            when(userMapper.selectByEmail("used@test.com")).thenReturn(anotherUser);

            BizException ex = assertThrows(BizException.class,
                    () -> userService.updateProfile(USER_ID, null, "used@test.com"));
            assertEquals(ErrorCode.DUPLICATE_ENTRY.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("更新自己的 email 应成功")
        void shouldUpdateOwnEmailSuccessfully() {
            UserEntity existing = createUser(USER_ID, "alice");
            when(userMapper.selectActiveById(USER_ID)).thenReturn(existing);
            when(userMapper.selectByEmail("new@test.com")).thenReturn(null);
            when(userMapper.updateSelective(eq(USER_ID), isNull(), eq("new@test.com"))).thenReturn(1);
            UserEntity updated = createUser(USER_ID, "alice");
            updated.setEmail("new@test.com");
            when(userMapper.selectActiveById(USER_ID)).thenReturn(updated);

            UserEntity result = userService.updateProfile(USER_ID, null, "new@test.com");

            assertNotNull(result);
            assertEquals("new@test.com", result.getEmail());
        }

        @Test
        @DisplayName("数据库更新返回 0 行应抛用户不存在异常")
        void shouldThrowWhenUpdateReturnsZero() {
            UserEntity existing = createUser(USER_ID, "alice");
            when(userMapper.selectActiveById(USER_ID)).thenReturn(existing);
            when(userMapper.updateSelective(eq(USER_ID), eq("newNick"), isNull())).thenReturn(0);

            BizException ex = assertThrows(BizException.class,
                    () -> userService.updateProfile(USER_ID, "newNick", null));
            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常更新 nickname 应成功并记录日志")
        void shouldUpdateNicknameSuccessfully() {
            UserEntity existing = createUser(USER_ID, "alice");
            when(userMapper.selectActiveById(USER_ID)).thenReturn(existing);
            when(userMapper.updateSelective(eq(USER_ID), eq("newNick"), isNull())).thenReturn(1);
            UserEntity updated = createUser(USER_ID, "alice");
            updated.setNickname("newNick");
            when(userMapper.selectActiveById(USER_ID)).thenReturn(updated);

            UserEntity result = userService.updateProfile(USER_ID, "newNick", null);

            assertNotNull(result);
            assertEquals("newNick", result.getNickname());
        }
    }

    private UserEntity createUser(Long id, String username) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(username);
        u.setEmail(username + "@test.com");
        return u;
    }
}
