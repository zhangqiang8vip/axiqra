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
 * - X-Axiqra-Timestamp: Unix 时间戳（秒，非毫秒）
 * - X-Axiqra-Signature: HMAC-SHA256(appId|timestamp|nonce|body, appSecret)，字段以 ASCII "|" 分隔
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

    @Value("${security.api-signature.max-body-size:1048576}")
    private int maxBodySize;

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

        // 对需要读取 body 的请求，先包装缓存（后续 getRequestBody 不再重复包装）
        CachedBodyHttpServletRequest wrappedRequest;
        try {
            wrappedRequest = new CachedBodyHttpServletRequest(request, maxBodySize);
        } catch (CachedBodyHttpServletRequest.PayloadTooLargeException e) {
            log.warn("【签名验证】请求体超限，size={}", e.getMessage());
            writeError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, ErrorCode.REQUEST_ENTITY_TOO_LARGE);
            return;
        } catch (IOException e) {
            log.warn("【签名验证】读取请求体失败，无法创建缓存请求", e);
            writeError(response, ErrorCode.API_SIGNATURE_INVALID);
            return;
        }

        // 获取签名头
        String appId = wrappedRequest.getHeader(appIdHeader);
        String timestamp = wrappedRequest.getHeader(timestampHeader);
        String signature = wrappedRequest.getHeader(signatureHeader);
        String nonce = wrappedRequest.getHeader(nonceHeader);

        // 1. 校验时间戳
        if (!isTimestampValid(timestamp)) {
            log.warn("【签名验证】时间戳超限，appId={}, timestamp={}", appId, timestamp);
            writeError(response, ErrorCode.API_SIGNATURE_EXPIRED);
            return;
        }

        // 2. 校验签名（从已缓存的 wrapper 读取 body）
        if (!isSignatureValid(appId, timestamp, nonce, signature, wrappedRequest)) {
            log.warn("[Sig] Signature mismatch, appId={}", appId);
            writeError(response, ErrorCode.API_SIGNATURE_INVALID);
            return;
        }

        // 3. 校验 Nonce（仅在签名通过后写入 Redis，避免未认证流量污染；key 按 appId 隔离）
        if (!isNonceUnique(appId, nonce)) {
            log.warn("[Sig] Nonce reused, appId={}, nonce={}", appId, nonce);
            writeError(response, ErrorCode.API_NONCE_REUSED);
            return;
        }

        log.debug("【签名验证】验证通过，appId={}", appId);
        chain.doFilter(wrappedRequest, response);
    }

    private boolean isPublicPath(String path) {
        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private boolean isTimestampValid(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return false;
        }
        try {
            long tsSeconds = Long.parseLong(timestamp);
            long nowSeconds = System.currentTimeMillis() / 1000;
            return Math.abs(nowSeconds - tsSeconds) <= clockSkewSeconds;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isNonceUnique(String appId, String nonce) {
        if (nonce == null || nonce.isBlank()) {
            return false;
        }
        // Redis SETNX with TTL=5min; scoped by appId to avoid cross-app collisions
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent("nonce:" + appId + ":" + nonce, "1", NONCE_TTL);
        return Boolean.TRUE.equals(success);
    }

    private boolean isSignatureValid(String appId, String timestamp, String nonce,
                                     String signature, HttpServletRequest request) {
        if (appId == null || signature == null) {
            return false;
        }

        // Timing-safe: always compute HMAC even if appSecret is missing (invalid).
        // Returning early would leak appId existence via timing difference.
        String appSecret = getAppSecret(appId);
        String payload = appId + "|" + timestamp + "|" + nonce + "|" + getRequestBody(request);
        String expected = hmacSha256(payload, appSecret != null ? appSecret : DUMMY_SECRET);

        // Constant-time comparison prevents timing attacks
        return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    // Dummy secret used only when appId is unknown, to maintain constant-time behavior
    private static final String DUMMY_SECRET = "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000";

    @SuppressWarnings("unchecked")
    private String getAppSecret(String appId) {
        // TODO (S2): replace with secure key vault (e.g. Spring Cloud Config + Vault,
        //            AWS Secrets Manager, or a dedicated KMS). Environment variables are
        //            acceptable for local dev only — never rely on them in production.
        String secret = System.getenv("AXIQRA_APP_SECRET_" + appId);
        if (secret == null || secret.isBlank()) {
            return null;
        }
        return secret;
    }

    private String getRequestBody(HttpServletRequest request) {
        try {
            byte[] body = request.getInputStream().readAllBytes();
            return new String(body, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("【签名验证】读取请求体失败", e);
            return "";
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
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
            // Init or compute failed — return dummy so caller still gets timing-safe comparison
            log.warn("[Sig] HMAC compute failed, using dummy signature for timing-safe compare", e);
            return DUMMY_SECRET.replace("\u0000", "00");
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        writeError(response, HttpServletResponse.SC_FORBIDDEN, errorCode);
    }

    private void writeError(HttpServletResponse response, int status, ErrorCode errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = Map.of(
                "code", errorCode.getCode(),
                "message", errorCode.getMessage(),
                "timestamp", java.time.OffsetDateTime.now().toString()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
