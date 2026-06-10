package com.axiqra.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 隐私相关工具。
 */
public final class PrivacyUtils {

    private PrivacyUtils() {
    }

    public static String pseudonymizeUserId(Long userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.valueOf(userId).getBytes(StandardCharsets.UTF_8));
            return "%02x%02x%02x%02x".formatted(hash[0], hash[1], hash[2], hash[3]);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
