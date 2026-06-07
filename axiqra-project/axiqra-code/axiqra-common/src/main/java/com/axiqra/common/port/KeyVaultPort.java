package com.axiqra.common.port;

/**
 * Key Vault 密钥存储 Port 接口
 *
 * <p>抽象密钥的存储与读取，屏蔽具体实现差异。
 * 当前阶段使用环境变量回退（DEV 模式），S2 可无缝替换为
 * AWS Secrets Manager / HashiCorp Vault / Spring Cloud Config 等生产方案。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
public interface KeyVaultPort {

    /**
     * 根据应用 ID 获取密钥
     *
     * <p>Caller <b>must</b> provide a non-null, non-blank {@code appId}.
     * Implementations of {@code KeyVaultPort#getSecret} <b>must</b> throw
     * {@link IllegalArgumentException} when {@code appId} is null or blank
     * (empty or whitespace-only after trimming). A missing secret is returned
     * as {@code null}.
     *
     * @param appId 应用标识（必填，非 null，非空白）
     * @return 密钥值，若不存在则返回 null
     * @throws IllegalArgumentException if {@code appId} is null or blank
     */
    String getSecret(String appId);

    /**
     * 检查是否已配置有效的密钥
     *
     * <p>有效的密钥（valid secret）定义为：值存在、非 null、
     * 且去除首尾空白字符后包含非空白内容。
     * 实现类（如 {@code KeyVaultAdapter}）仅在存储的密钥满足上述
     * 非 null、非空白条件时才应返回 {@code true}。
     *
     * @param appId 应用标识
     * @return true 表示已配置有效密钥
     */
    boolean hasSecret(String appId);
}
