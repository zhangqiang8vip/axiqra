package com.axiqra.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AesEncryptUtil 单元测试
 * 验证 AES-256-GCM 加密/解密正确性
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
class AesEncryptUtilTest {

    private static final String TEST_KEY;

    static {
        TEST_KEY = AesEncryptUtil.generateKey();
    }

    @Test
    @DisplayName("加密后解密应还原为原文")
    void encryptDecrypt_roundTrip() {
        String plaintext = "test-api-key-12345";
        String ciphertext = AesEncryptUtil.encrypt(plaintext, TEST_KEY);
        String decrypted = AesEncryptUtil.decrypt(ciphertext, TEST_KEY);
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("每次加密结果应不同（随机 IV）")
    void encrypt_twice_differentCiphertext() {
        String plaintext = "test-api-key-12345";
        String c1 = AesEncryptUtil.encrypt(plaintext, TEST_KEY);
        String c2 = AesEncryptUtil.encrypt(plaintext, TEST_KEY);
        assertNotEquals(c1, c2, "相同明文加密两次，结果应不同（IV 随机）");
    }

    @Test
    @DisplayName("不同密钥解密应失败")
    void decrypt_wrongKey_shouldThrow() {
        String plaintext = "test-api-key-12345";
        String ciphertext = AesEncryptUtil.encrypt(plaintext, TEST_KEY);
        String wrongKey = AesEncryptUtil.generateKey();
        assertThrows(RuntimeException.class, () -> AesEncryptUtil.decrypt(ciphertext, wrongKey));
    }

    @Test
    @DisplayName("null 明文应抛出 IllegalArgumentException")
    void encrypt_null_shouldThrow() {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> AesEncryptUtil.encrypt(null, TEST_KEY)
        );
        assertEquals("plaintext must not be null or blank", thrown.getMessage());
    }

    @Test
    @DisplayName("空白明文应抛出 IllegalArgumentException")
    void encrypt_blank_shouldThrow() {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> AesEncryptUtil.encrypt("  ", TEST_KEY)
        );
        assertEquals("plaintext must not be null or blank", thrown.getMessage());
    }

    @Test
    @DisplayName("generateKey 应生成 32 字节密钥")
    void generateKey_length() {
        String key = AesEncryptUtil.generateKey();
        byte[] decoded = java.util.Base64.getDecoder().decode(key);
        assertEquals(32, decoded.length, "AES-256 需要 32 字节密钥");
    }

    @Test
    @DisplayName("isValidKey 应正确校验密钥格式")
    void isValidKey() {
        assertTrue(AesEncryptUtil.isValidKey(TEST_KEY));
        assertFalse(AesEncryptUtil.isValidKey(null));
        assertFalse(AesEncryptUtil.isValidKey(""));
        assertFalse(AesEncryptUtil.isValidKey("too-short"));
        assertFalse(AesEncryptUtil.isValidKey(
                java.util.Base64.getEncoder().encodeToString(new byte[16])));
        assertFalse(AesEncryptUtil.isValidKey("not-valid-base64!!!"));
    }

    @Test
    @DisplayName("无效 Base64 密钥加密应抛出 RuntimeException")
    void encrypt_invalidBase64Key_shouldThrow() {
        String invalidKey = "!!!not-base64!!!";
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> AesEncryptUtil.encrypt("plaintext", invalidKey));
        assertTrue(thrown.getMessage().contains("AES"));
    }

    @Test
    @DisplayName("加密后密文格式应包含冒号分隔符")
    void encrypt_outputFormat() {
        String ciphertext = AesEncryptUtil.encrypt("test", TEST_KEY);
        assertTrue(ciphertext.contains(":"), "密文应为 Base64(iv):Base64(ciphertext) 格式");
        assertEquals(2, ciphertext.split(":").length);
    }

    @Test
    @DisplayName("Unicode 字符串加密解密应正常")
    void encryptDecrypt_unicode() {
        String plaintext = "你好 Axiqra! 🎉 测试密码: P@ssw0rd";
        String ciphertext = AesEncryptUtil.encrypt(plaintext, TEST_KEY);
        String decrypted = AesEncryptUtil.decrypt(ciphertext, TEST_KEY);
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("长字符串加密解密应正常")
    void encryptDecrypt_longText() {
        String plaintext = "A".repeat(10000);
        String ciphertext = AesEncryptUtil.encrypt(plaintext, TEST_KEY);
        String decrypted = AesEncryptUtil.decrypt(ciphertext, TEST_KEY);
        assertEquals(plaintext, decrypted);
    }
}
