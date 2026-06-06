package com.axiqra.common.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加密工具类
 * <p>
 * 用于加密 api_key、token、password 等敏感字段。
 * 加密后存入数据库，查询时自动解密。
 * <p>
 * 算法：AES-256-GCM（带认证标签，防止篡改）
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public class AesEncryptUtil {

    public static final String ALGORITHM = "AES/GCM/NoPadding";
    public static final int GCM_IV_LENGTH = 12;    // 96-bit IV
    public static final int GCM_TAG_LENGTH = 128;  // 128-bit tag

    private AesEncryptUtil() {
    }

    /**
     * 加密字符串
     *
     * @param plaintext 明文
     * @param key       Base64 编码的 AES-256 密钥（32 字节）
     * @return Base64 编码的 IV + 密文（格式：Base64(iv):Base64(ciphertext)）
     */
    public static String encrypt(String plaintext, String key) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("plaintext must not be null or blank");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + ciphertext，返回 Base64(iv):Base64(ciphertext)
            String encoded = Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(ciphertext);
            return encoded;
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    /**
     * 解密字符串
     *
     * @param ciphertext Base64 编码的 IV + 密文
     * @param key        Base64 编码的 AES-256 密钥
     * @return 明文
     */
    public static String decrypt(String ciphertext, String key) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return ciphertext;
        }
        try {
            String[] parts = ciphertext.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("加密数据格式无效");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败", e);
        }
    }

    /**
     * 生成 AES-256 随机密钥
     *
     * @return Base64 编码的 32 字节密钥
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("AES 密钥生成失败", e);
        }
    }

    /**
     * 从 Base64 字符串验证密钥格式
     */
    public static boolean isValidKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(key);
            return bytes.length == 32;
        } catch (Exception e) {
            return false;
        }
    }
}
