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
     * @param appId 应用标识
     * @return 密钥值，若不存在则返回 null
     */
    String getSecret(String appId);

    /**
     * 检查是否已配置有效的密钥
     *
     * @param appId 应用标识
     * @return true 表示已配置有效密钥
     */
    boolean hasSecret(String appId);
}
