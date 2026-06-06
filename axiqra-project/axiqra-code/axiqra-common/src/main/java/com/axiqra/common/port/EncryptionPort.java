package com.axiqra.common.port;

/**
 * 加密端口（Encryption Port）
 * <p>
 * 定义加密/解密接口，支持算法可插拔。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public interface EncryptionPort {

    /**
     * 加密明文
     *
     * @param plaintext 明文
     * @return 密文（Base64 编码，含随机 IV，格式为 IV:密文）
     */
    String encrypt(String plaintext);

    /**
     * 解密密文
     *
     * @param ciphertext 密文（Base64 编码，格式为 IV:密文）
     * @return 明文
     */
    String decrypt(String ciphertext);

    /**
     * 生成随机加密密钥（用于新数据加密）
     *
     * @return 密钥（Base64 编码）
     */
    String generateKey();

    /**
     * 用指定密钥加密（用于数据迁移或特殊场景）
     *
     * @param plaintext 明文
     * @param key       密钥（Base64 编码）
     * @return 密文（Base64 编码，含随机 IV，格式为 IV:密文）
     */
    String encryptWithKey(String plaintext, String key);

    /**
     * 用指定密钥解密
     *
     * @param ciphertext 密文（格式为 IV:密文）
     * @param key         密钥（Base64 编码）
     * @return 明文
     */
    String decryptWithKey(String ciphertext, String key);
}
