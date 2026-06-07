package com.axiqra.core.adapter;

import com.axiqra.common.port.KeyVaultPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 环境变量 KeyVault 适配器（DEV / 本地开发阶段）
 *
 * <p>密钥从环境变量读取，变量命名规范：AXIQRA_APP_SECRET_&lt;APP_ID&gt;
 * 仅适用于本地开发，S2 阶段应替换为生产级密钥服务。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@Component
public class KeyVaultAdapter implements KeyVaultPort {

    private static final String ENV_PREFIX = "AXIQRA_APP_SECRET_";

    @Override
    public String getSecret(String appId) {
        if (appId == null || appId.isBlank()) {
            return null;
        }
        String secret = System.getenv(ENV_PREFIX + appId);
        if (secret == null || secret.isBlank()) {
            return null;
        }
        return secret;
    }

    @Override
    public boolean hasSecret(String appId) {
        return getSecret(appId) != null;
    }
}
