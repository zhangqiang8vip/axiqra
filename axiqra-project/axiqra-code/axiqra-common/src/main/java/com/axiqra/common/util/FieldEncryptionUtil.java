package com.axiqra.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 字段加密工具类
 * 实现 AES-256-GCM 加密，用于敏感字段加密存储
 *
 * @author Axiqra Team
 */
@Slf4j
@Component
public class FieldEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    @Value("${axiqra.security.field-encryption.key:}")
    private String encryptionKeyBase64;

    @Value("${axiqra.security.field-encryption.enabled:false}")
    private boolean encryptionEnabled;

    private SecretKey getSecretKey() {
        if (encryptionKeyBase64 == null || encryptionKeyBase64.isBlank()) {
            throw new IllegalStateException("字段加密未配置密钥，请设置 axiqra.security.field-encryption.key");
        }
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * 加密字段值
     *
     * @param plainText 明文
     * @return 密文 (Base64 编码的 IV + 密文 + Tag)
     */
    public String encrypt(String plainText) {
        if (!encryptionEnabled) {
            return plainText;
        }
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + cipherText
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("字段加密失败", e);
            throw new RuntimeException("字段加密失败", e);
        }
    }

    /**
     * 解密字段值
     *
     * @param encryptedText 密文 (Base64 编码)
     * @return 明文
     */
    public String decrypt(String encryptedText) {
        if (!encryptionEnabled) {
            return encryptedText;
        }
        if (encryptedText == null || encryptedText.isBlank()) {
            return encryptedText;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("字段解密失败", e);
            throw new RuntimeException("字段解密失败", e);
        }
    }

    /**
     * 生成新的加密密钥
     * 用于初始化配置
     *
     * @return Base64 编码的 AES-256 密钥
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("密钥生成失败", e);
        }
    }
}
