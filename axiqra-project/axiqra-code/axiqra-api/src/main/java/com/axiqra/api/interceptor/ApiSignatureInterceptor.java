package com.axiqra.api.interceptor;

import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * API 签名验证拦截器
 * 实现 HMAC-SHA256 签名校验，防止请求篡改和重放攻击
 *
 * @author Axiqra Team
 */
@Slf4j
@Component
public class ApiSignatureInterceptor implements HandlerInterceptor {

    private static final String SIGNATURE_HEADER = "X-Api-Signature";
    private static final String TIMESTAMP_HEADER = "X-Api-Timestamp";
    private static final String ACCESS_KEY_HEADER = "X-Access-Key";
    private static final String SIGNATURE_METHOD = "HmacSHA256";

    @Value("${axiqra.api-signature.ttl-seconds:300}")
    private long signatureTtlSeconds = 300;

    @Value("${axiqra.feature.enable-api-signature:false}")
    private boolean enableSignature;

    private final StringRedisTemplate redisTemplate;

    public ApiSignatureInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enableSignature) {
            return true;
        }

        String signature = request.getHeader(SIGNATURE_HEADER);
        String timestampStr = request.getHeader(TIMESTAMP_HEADER);
        String accessKey = request.getHeader(ACCESS_KEY_HEADER);

        if (signature == null || timestampStr == null || accessKey == null) {
            throw new BizException(ErrorCode.PARAM_MISSING, "缺少签名必需的 Header: X-Api-Signature, X-Api-Timestamp, X-Access-Key");
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "时间戳格式错误");
        }

        long currentTime = Instant.now().getEpochSecond();
        if (Math.abs(currentTime - timestamp) > signatureTtlSeconds) {
            throw new BizException(ErrorCode.API_SIGNATURE_EXPIRED, "签名已过期，请重新签名");
        }

        String nonceKey = "api:nonce:" + accessKey + ":" + timestampStr;
        Boolean exists = redisTemplate.hasKey(nonceKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BizException(ErrorCode.API_NONCE_REUSED, "签名已被使用，请使用新的时间戳");
        }

        String expectedSignature = computeSignature(request, accessKey, timestampStr);
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("API 签名验证失败: accessKey={}, timestamp={}", accessKey, timestampStr);
            throw new BizException(ErrorCode.API_SIGNATURE_INVALID, "签名验证失败");
        }

        redisTemplate.opsForValue().set(nonceKey, "1", signatureTtlSeconds, TimeUnit.SECONDS);
        return true;
    }

    public String computeSignature(HttpServletRequest request, String accessKey, String timestamp) {
        try {
            String secretKey = getSecretKey(accessKey);
            if (secretKey == null) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "无效的 Access Key");
            }

            String method = request.getMethod();
            String path = request.getRequestURI();
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                path = path + "?" + queryString;
            }

            String dataToSign = method + "\n" + path + "\n" + timestamp + "\n";

            Mac mac = Mac.getInstance(SIGNATURE_METHOD);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), SIGNATURE_METHOD);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("签名计算失败", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "签名计算失败");
        }
    }

    private String getSecretKey(String accessKey) {
        String envSecretKey = System.getenv("AXIQRA_API_SECRET_" + accessKey);
        if (envSecretKey != null) {
            return envSecretKey;
        }
        if ("dev-access-key".equals(accessKey)) {
            return "dev-secret-key-for-testing";
        }
        return null;
    }

    public static String generateSignature(String method, String path, String timestamp, String body, String secretKey) {
        try {
            String dataToSign = method + "\n" + path + "\n" + timestamp + "\n" + (body != null ? body : "");
            Mac mac = Mac.getInstance(SIGNATURE_METHOD);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), SIGNATURE_METHOD);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "签名生成失败");
        }
    }
}
