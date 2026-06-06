package com.axiqra.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordHashUtil 单元测试
 * 验证 BCrypt 密码哈希/验证正确性
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
class PasswordHashUtilTest {

    @Test
    @DisplayName("哈希后 matches 应返回 true")
    void hashThenMatches_success() {
        String raw = "MyStr0ngP@ssw0rd!";
        String hashed = PasswordHashUtil.hash(raw);
        assertTrue(PasswordHashUtil.matches(raw, hashed));
    }

    @Test
    @DisplayName("错误密码应返回 false")
    void matches_wrongPassword() {
        String raw = "CorrectPassword";
        String hashed = PasswordHashUtil.hash(raw);
        assertFalse(PasswordHashUtil.matches("WrongPassword", hashed));
    }

    @Test
    @DisplayName("相同密码每次哈希结果不同（随机盐）")
    void hash_samePassword_differentHash() {
        String raw = "SamePassword123";
        String h1 = PasswordHashUtil.hash(raw);
        String h2 = PasswordHashUtil.hash(raw);
        assertNotEquals(h1, h2, "相同密码哈希结果应不同（BCrypt 随机盐）");
    }

    @Test
    @DisplayName("空密码应抛出异常")
    void hash_nullPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHashUtil.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordHashUtil.hash(""));
    }

    @Test
    @DisplayName("matches null 应返回 false")
    void matches_nullInputs() {
        String hashed = PasswordHashUtil.hash("Password123");
        assertFalse(PasswordHashUtil.matches(null, hashed));
        assertFalse(PasswordHashUtil.matches("Password123", null));
    }

    @Test
    @DisplayName("isValidHash 应正确识别 BCrypt 格式")
    void isValidHash() {
        String hashed = PasswordHashUtil.hash("Test");
        assertTrue(PasswordHashUtil.isValidHash(hashed));
        assertFalse(PasswordHashUtil.isValidHash(null));
        assertFalse(PasswordHashUtil.isValidHash(""));
        assertFalse(PasswordHashUtil.isValidHash("not-a-bcrypt-hash"));
    }

    @Test
    @DisplayName("哈希长度应符合 BCrypt 格式（60 字符）")
    void hash_length() {
        String hashed = PasswordHashUtil.hash("TestPassword");
        assertEquals(60, hashed.length(), "BCrypt 哈希长度应为 60 字符");
    }

    @Test
    @DisplayName("特殊字符密码应正常处理")
    void hash_specialCharacters() {
        String raw = "P@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?";
        String hashed = PasswordHashUtil.hash(raw);
        assertTrue(PasswordHashUtil.matches(raw, hashed));
    }
}
