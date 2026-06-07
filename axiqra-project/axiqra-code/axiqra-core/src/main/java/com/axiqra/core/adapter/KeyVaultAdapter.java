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
            throw new IllegalArgumentException("appId must not be null or blank");
        }
        String envName = ENV_PREFIX + sanitizeEnvName(appId);
        String secret = System.getenv(envName);
        if (secret == null || secret.isBlank()) {
            return null;
        }
        return secret;
    }

    @Override
    public boolean hasSecret(String appId) {
        return getSecret(appId) != null;
    }

    /**
     * Sanitizes {@code appId} to a POSIX-compliant environment variable name segment.
     * Transforms the value by trimming, converting to upper-case, and replacing
     * any invalid characters (hyphens, spaces, dots, etc.) with underscores.
     * @throws IllegalArgumentException if the sanitized result is empty or starts
     *         with a digit (illegal for env vars) or if {@code appId} is null/blank
     */
    private String sanitizeEnvName(String appId) {
        String trimmed = appId.trim();
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = Character.toUpperCase(trimmed.charAt(i));
            boolean valid;
            if (i == 0) {
                valid = Character.isLetter(c) || c == '_';
            } else {
                valid = Character.isLetterOrDigit(c) || c == '_';
            }
            sb.append(valid ? c : '_');
        }
        String result = sb.toString();
        if (result.isEmpty() || Character.isDigit(result.charAt(0))) {
            log.debug("[KeyVault] Sanitized appId '{}' resulted in invalid env var name, rejecting", appId);
            throw new IllegalArgumentException(
                    "appId '" + appId + "' cannot be mapped to a valid environment variable name");
        }
        return result;
    }
}
