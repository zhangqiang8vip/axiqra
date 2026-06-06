package com.axiqra.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码哈希工具类
 * <p>
 * 使用 BCrypt（cost=12）哈希密码。
 * 验证时用 matches() 方法。
 * <p>
 * BCrypt 特性：
 * - 自动生成盐值
 * - cost 越高计算越慢，抗彩虹表
 * - 内置版本标识（$2a$ / $2b$）
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Component
public class PasswordHashUtil {

    private static final int BCRYPT_COST = 12;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(BCRYPT_COST);

    private PasswordHashUtil() {
    }

    /**
     * 哈希密码
     *
     * @param rawPassword 明文密码
     * @return BCrypt 哈希字符串
     */
    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return ENCODER.encode(rawPassword);
    }

    /**
     * 验证密码
     *
     * @param rawPassword     明文密码
     * @param hashedPassword BCrypt 哈希字符串
     * @return true=匹配，false=不匹配
     */
    public static boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return ENCODER.matches(rawPassword, hashedPassword);
    }

    /**
     * 检查哈希值是否为有效 BCrypt 格式
     */
    public static boolean isValidHash(String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }
        return hashedPassword.startsWith("$2a$")
                || hashedPassword.startsWith("$2b$")
                || hashedPassword.startsWith("$2y$");
    }
}
