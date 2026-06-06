package com.axiqra.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataMaskingUtil 单元测试
 * 验证 JSON 敏感字段脱敏正确性
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
class DataMaskingUtilTest {

    @Test
    @DisplayName("API Key 应被脱敏")
    void maskJson_apiKey() {
        String json = "{\"api_key\": \"sk-1234567890abcdef\"}";
        String masked = DataMaskingUtil.maskJson(json);
        assertFalse(masked.contains("sk-1234567890abcdef"));
        assertTrue(masked.contains("SECRET_REDACTED"));
    }

    @Test
    @DisplayName("apiKey 应被脱敏")
    void maskJson_apiKeyCamel() {
        String json = "{\"apiKey\": \"sk-live-abcdefghijklmnop\"}";
        String masked = DataMaskingUtil.maskJson(json);
        assertFalse(masked.contains("sk-live-"));
        assertTrue(masked.contains("SECRET_REDACTED"));
    }

    @Test
    @DisplayName("token 应被脱敏")
    void maskJson_token() {
        String json = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U\"}";
        String masked = DataMaskingUtil.maskJson(json);
        assertFalse(masked.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"));
        assertTrue(masked.contains("TOKEN_REDACTED"));
    }

    @Test
    @DisplayName("password 应被脱敏")
    void maskJson_password() {
        String json = "{\"password\": \"MySecretPassword123!\"}";
        String masked = DataMaskingUtil.maskJson(json);
        assertFalse(masked.contains("MySecretPassword123!"));
        assertTrue(masked.contains("PASSWORD_REDACTED"));
    }

    @Test
    @DisplayName("内网 IP 应被脱敏")
    void maskJson_internalIp() {
        String json = "{\"server_ip\": \"192.168.1.100\", \"db_ip\": \"10.0.0.50\"}";
        String masked = DataMaskingUtil.maskJson(json);
        assertFalse(masked.contains("192.168.1.100"));
        assertFalse(masked.contains("10.0.0.50"));
        assertTrue(masked.contains("INTERNAL_IP"));
    }

    @Test
    @DisplayName("私钥内容应被整体脱敏")
    void maskJson_privateKey() {
        String json = """
            {
              "private_key": "-----BEGIN RSA PRIVATE KEY-----\\nMIIBOgIBAAJBALRiMLAHudeSA2aC8xq8x9gKjN8aA9Gv...\\n-----END RSA PRIVATE KEY-----"
            }
            """;
        String masked = DataMaskingUtil.maskJson(json);
        assertFalse(masked.contains("-----BEGIN RSA PRIVATE KEY-----"));
        assertTrue(masked.contains("PRIVATE_KEY_REDACTED"));
    }

    @Test
    @DisplayName("AWS 凭证应被脱敏")
    void maskJson_awsCredentials() {
        String json = "{\"aws_key\": \"AKIAIOSFODNN7EXAMPLE\"}";
        String masked = DataMaskingUtil.maskJson(json);
        assertFalse(masked.contains("AKIAIOSFODNN7EXAMPLE"));
        assertTrue(masked.contains("CLOUD_CREDENTIALS_REDACTED"));
    }

    @Test
    @DisplayName("非敏感内容不应被修改")
    void maskJson_nonSensitive() {
        String json = "{\"username\": \"alice\", \"title\": \"Fix bug #123\"}";
        String masked = DataMaskingUtil.maskJson(json);
        assertEquals(json, masked, "非敏感字段不应被修改");
    }

    @Test
    @DisplayName("空字符串应原样返回")
    void maskJson_nullAndBlank() {
        assertNull(DataMaskingUtil.maskJson(null));
        assertEquals("", DataMaskingUtil.maskJson(""));
    }

    @Test
    @DisplayName("脱敏邮箱应显示 EMAIL_REDACTED")
    void maskEmail() {
        String text = "Contact: alice@example.com for support";
        String masked = DataMaskingUtil.maskEmail(text);
        assertFalse(masked.contains("alice@example.com"));
        assertTrue(masked.contains("EMAIL_REDACTED"));
    }

    @Test
    @DisplayName("脱敏仓库 URL 应隐藏路径")
    void maskRepoUrl() {
        String text = "https://github.com/my-org/private-repo.git";
        String masked = DataMaskingUtil.maskRepoUrl(text);
        assertFalse(masked.contains("my-org/private-repo"));
        assertTrue(masked.contains("PRIVATE_REPOSITORY"));
    }

    @Test
    @DisplayName("GitLab repository URL should be masked")
    void maskRepoUrl_gitlab() {
        String text = "https://gitlab.com/my-org/private-repo.git";
        String masked = DataMaskingUtil.maskRepoUrl(text);
        assertFalse(masked.contains("my-org/private-repo"));
        assertTrue(masked.contains("PRIVATE_REPOSITORY"));
    }

    @Test
    @DisplayName("脱敏本地路径应隐藏用户目录")
    void maskLocalPath() {
        String text = "File at /home/alice/projects/axiqra/config.yaml";
        String masked = DataMaskingUtil.maskLocalPath(text);
        assertFalse(masked.contains("/home/alice"));
        assertTrue(masked.contains("[USER_DIR]"));
    }

    @Test
    @DisplayName("多层嵌套 JSON 应正确脱敏")
    void maskJson_nested() {
        String json = """
            {
              "config": {
                "api_key": "sk-prod-key-12345",
                "database": {
                  "password": "db_secret_pass",
                  "host": "192.168.1.10"
                }
              }
            }
            """;
        String masked = DataMaskingUtil.maskJson(json);
        assertTrue(masked.contains("SECRET_REDACTED"));
        assertTrue(masked.contains("PASSWORD_REDACTED"));
        assertTrue(masked.contains("INTERNAL_IP"));
        assertFalse(masked.contains("sk-prod-key-12345"));
        assertFalse(masked.contains("db_secret_pass"));
    }
}
