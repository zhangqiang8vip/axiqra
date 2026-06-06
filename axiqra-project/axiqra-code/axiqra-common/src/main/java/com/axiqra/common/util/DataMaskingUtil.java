package com.axiqra.common.util;

import java.util.regex.Pattern;

/**
 * 数据脱敏工具类
 * <p>
 * 用于 Trace Package 提交时自动脱敏敏感字段。
 * <p>
 * 脱敏规则：
 * - api_key / apiKey / API_KEY     → SECRET_REDACTED
 * - token / bearer_token           → TOKEN_REDACTED
 * - password / pwd                 → PASSWORD_REDACTED
 * - secret / app_secret            → SECRET_REDACTED
 * - email                          → 脱敏邮箱
 * - 内网 IP                        → INTERNAL_IP
 * - 私钥内容                       → PRIVATE_KEY_REDACTED
 * - AWS/GCP 凭证                   → CLOUD_CREDENTIALS_REDACTED
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public class DataMaskingUtil {

    public static final String REDACTED_API_KEY     = "SECRET_REDACTED";
    public static final String REDACTED_TOKEN       = "TOKEN_REDACTED";
    public static final String REDACTED_PASSWORD     = "PASSWORD_REDACTED";
    public static final String REDACTED_EMAIL       = "EMAIL_REDACTED";
    public static final String REDACTED_INTERNAL_IP  = "INTERNAL_IP";
    public static final String REDACTED_PRIVATE_KEY = "PRIVATE_KEY_REDACTED";
    public static final String REDACTED_CLOUD_CREDS  = "CLOUD_CREDENTIALS_REDACTED";
    public static final String REDACTED_REPO         = "PRIVATE_REPOSITORY";

    // 匹配 API Key 模式（各种常见格式）
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|apikey|api[_-]?secret|api[_-]?token)\\s*[:=]\\s*[\"']?([\\w\\-]{8,})[\"']?",
            Pattern.CASE_INSENSITIVE);

    // 匹配 Token 模式
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(?i)(bearer[_-]?token|access[_-]?token|refresh[_-]?token|auth[_-]?token)\\s*[:=]\\s*[\"']?([\\w\\-\\.]{16,})[\"']?",
            Pattern.CASE_INSENSITIVE);

    // 匹配密码字段
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(?i)(password|pwd|passwd|secret|app[_-]?secret)\\s*[:=]\\s*[\"']?([\\S]{4,})[\"']?",
            Pattern.CASE_INSENSITIVE);

    // 匹配邮箱
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    // 匹配内网 IP（10.x.x.x / 172.16-31.x.x / 192.168.x.x）
    private static final Pattern INTERNAL_IP_PATTERN = Pattern.compile(
            "\\b(10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|"
                    + "172\\.(1[6-9]|2\\d|3[0-1])\\.\\d{1,3}\\.\\d{1,3}|"
                    + "192\\.168\\.\\d{1,3}\\.\\d{1,3})\\b");

    // 匹配私钥内容
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
            "-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----",
            Pattern.CASE_INSENSITIVE);

    // 匹配 AWS/GCP 凭证
    private static final Pattern AWS_CREDS_PATTERN = Pattern.compile(
            "(?i)(AKIA|ABIA|ACID|AGPA|ASIA)[A-Z0-9]{12,}");
    private static final Pattern GCP_CREDS_PATTERN = Pattern.compile(
            "\"[^\"]*credential[^\"]*\\s*:\\s*\\{[^\"]*\"type\"\\s*:\\s*\"service_account\"[^\"]*\\}");

    private DataMaskingUtil() {
    }

    /**
     * 脱敏 JSON 字符串中的敏感字段
     *
     * @param json JSON 字符串
     * @return 脱敏后的 JSON
     */
    public static String maskJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }

        String masked = json;

        // API Key 脱敏
        masked = API_KEY_PATTERN.matcher(masked)
                .replaceAll("$1: \"" + REDACTED_API_KEY + "\"");

        // Token 脱敏
        masked = TOKEN_PATTERN.matcher(masked)
                .replaceAll("$1: \"" + REDACTED_TOKEN + "\"");

        // 密码/秘钥脱敏
        masked = PASSWORD_PATTERN.matcher(masked)
                .replaceAll("$1: \"" + REDACTED_PASSWORD + "\"");

        // 内网 IP 脱敏
        masked = INTERNAL_IP_PATTERN.matcher(masked)
                .replaceAll(REDACTED_INTERNAL_IP);

        // 私钥内容脱敏（整个文件内容）
        if (PRIVATE_KEY_PATTERN.matcher(masked).find()) {
            masked = masked.replaceAll(
                    "-----BEGIN[\\s\\S]*?-----END[\\s\\S]*?PRIVATE KEY-----",
                    REDACTED_PRIVATE_KEY);
        }

        // AWS 凭证脱敏
        masked = AWS_CREDS_PATTERN.matcher(masked)
                .replaceAll(REDACTED_CLOUD_CREDS);

        return masked;
    }

    /**
     * 脱敏邮箱地址
     */
    public static String maskEmail(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return EMAIL_PATTERN.matcher(text)
                .replaceAll(REDACTED_EMAIL);
    }

    /**
     * 脱敏仓库地址（隐藏私有仓库路径）
     */
    public static String maskRepoUrl(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        // 隐藏 GitHub/GitLab 私有仓库格式
        return text.replaceAll(
                "(https?://[^@/]+@)?[^/]*github\\.com[^/]*/[\\w\\-\\.]+/[\\w\\-\\.]+(\\.git)?",
                REDACTED_REPO);
    }

    /**
     * 脱敏本地路径（隐藏用户名和具体路径）
     */
    public static String maskLocalPath(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        // 脱敏用户目录
        return text.replaceAll(
                "(?i)(/home/\\w+|/Users/\\w+|C:\\\\Users\\\\\\w+|D:\\\\Users\\\\\\w+)",
                "[USER_DIR]");
    }
}
