package com.axiqra.api.filter;

import com.axiqra.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * API 签名认证过滤器
 * <p>
 * 职责：
 * 1. 验证请求头中的 HMAC-SHA256 签名
 * 2. 验证时间戳是否在 5 分钟窗口内（防重放）
 * 3. 验证 Nonce 是否唯一（防重放，存入 Redis TTL=5min）
 * 4. 公开接口白名单跳过验证
 * <p>
 * 请求头：
 * - X-Axiqra-App-Id: 应用 ID
 * - X-Axiqra-Timestamp: 时间戳（毫秒）
 * - X-Axiqra-Signature: HMAC-SHA256(appId + timestamp + nonce + requestBody, appSecret)
 * - X-Axiqra-Nonce: UUID（防重放）
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiSignatureFilter implements Filter {

    private static final Duration NONCE_TTL = Duration.ofMinutes(5);

    @Value("${security.api-signature.app-id-header:X-Axiqra-App-Id}")
    private String appIdHeader;

    @Value("${security.api-signature.timestamp-header:X-Axiqra-Timestamp}")
    private String timestampHeader;

    @Value("${security.api-signature.signature-header:X-Axiqra-Signature}")
    private String signatureHeader;

    @Value("${security.api-signature.nonce-header:X-Axiqra-Nonce}")
    private String nonceHeader;

    @Value("${security.api-signature.clock-skew-seconds:300}")
    private long clockSkewSeconds;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 公开接口白名单（路径匹配则跳过签名验证）
    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/actuator/health",
            "/actuator/info",
            "/internal/health",
            "/api/internal/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/doc.html",
            "/favicon.ico"
    );

    public ApiSignatureFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String path = request.getRequestURI();

        // 公开接口跳过签名验证
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 获取签名头
        String appId = request.getHeader(appIdHeader);
        String timestamp = request.getHeader(timestampHeader);
        String signature = request.getHeader(signatureHeader);
        String nonce = request.getHeader(nonceHeader);

        // 1. 校验时间戳
        if (!isTimestampValid(timestamp)) {
            log.warn("【签名验证】时间戳超限，appId={}, timestamp={}", appId, timestamp);
            writeError(response, ErrorCode.API_SIGNATURE_EXPIRED);
            return;
        }

        // 2. 校验 Nonce
        if (!isNonceUnique(nonce)) {
            log.warn("【签名验证】Nonce 已使用，appId={}, nonce={}", appId, nonce);
            writeError(response, ErrorCode.API_NONCE_REUSED);
            return;
        }

        // 3. 校验签名
        if (!isSignatureValid(appId, timestamp, nonce, signature, request)) {
            log.warn("【签名验证】签名不匹配，appId={}", appId);
            writeError(response, ErrorCode.API_SIGNATURE_INVALID);
            return;
        }

        log.debug("【签名验证】验证通过，appId={}", appId);
        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isTimestampValid(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return false;
        }
        try {
            long ts = Long.parseLong(timestamp);
            long now = System.currentTimeMillis();
            return Math.abs(now - ts) <= clockSkewSeconds * 1000L;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isNonceUnique(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            return false;
        }
        // Redis SETNX，TTL=5min；返回 true=新 nonce，false=已存在（重放）
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent("nonce:" + nonce, "1", NONCE_TTL);
        return Boolean.TRUE.equals(success);
    }

    private boolean isSignatureValid(String appId, String timestamp, String nonce,
                                     String signature, HttpServletRequest request) {
        if (appId == null || signature == null) {
            return false;
        }

        // 从数据库或配置获取 appSecret（暂时用内存模拟，S2 改为 DB 查询）
        String appSecret = getAppSecret(appId);
        if (appSecret == null) {
            log.warn("【签名验证】未找到 appSecret，appId={}", appId);
            return false;
        }

        // 构造签名字符串：appId + timestamp + nonce + requestBody
        String payload = appId + timestamp + nonce + getRequestBody(request);

        // 计算 HMAC-SHA256
        String expected = hmacSha256(payload, appSecret);

        // 常量时间比较（防时序攻击）
        return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    @SuppressWarnings("unchecked")
    private String getAppSecret(String appId) {
        // TODO (S2): 从数据库查询 appSecret
        // 必须通过环境变量配置，否则拒绝验证（防止硬编码密钥上线）
        String secret = System.getenv("AXIQRA_APP_SECRET_" + appId);
        if (secret == null || secret.isBlank()) {
            log.error("【签名验证】未配置 appSecret，appId={}，请设置环境变量 AXIQRA_APP_SECRET_{}",
                    appId, appId);
            return null;
        }
        return secret;
    }

    private String getRequestBody(HttpServletRequest request) {
        try {
            CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
            byte[] body = wrapped.getInputStream().readAllBytes();
            return new String(body, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("【签名验证】读取请求体失败", e);
            return "";
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 计算失败", e);
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = Map.of(
                "code", errorCode.getCode(),
                "message", errorCode.getMessage(),
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
