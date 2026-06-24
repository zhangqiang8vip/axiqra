package com.axiqra.api.config;

import java.util.Set;

/**
 * Public web endpoints that intentionally bypass login/API-signature checks.
 */
public final class PublicEndpointPaths {

    private PublicEndpointPaths() {
    }

    public static final String[] AUTH_EXCLUDES = {
            "/internal/health",
            "/internal/health/verify",
            "/api/internal/health/**",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/doc.html",
            "/favicon.ico",
            "/auth/login",
            "/auth/register",
            "/auth/captcha",
            "/public-cases",
            "/public-cases/*",
            "/solutions/public",
            "/solutions/public/*",
            "/search/public",
            "/v1/tool-models/leaderboard"
    };

    public static final String[] SPRING_PUBLIC_GET = {
            "/internal/health/**",
            "/api/internal/health/**",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/doc.html",
            "/favicon.ico",
            "/public-cases",
            "/public-cases/*",
            "/solutions/public",
            "/solutions/public/*",
            "/v1/tool-models/leaderboard"
    };

    public static final Set<String> API_SIGNATURE_PUBLIC_PREFIXES = Set.of(
            "/actuator/health",
            "/actuator/info",
            "/internal/health",
            "/api/internal/health",
            "/v3/api-docs",
            "/api/v3/api-docs",
            "/swagger-ui",
            "/api/swagger-ui",
            "/doc.html",
            "/api/doc.html",
            "/favicon.ico",
            "/api/favicon.ico",
            "/auth/login",
            "/auth/register",
            "/auth/captcha",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/captcha",
            "/auth/me",
            "/auth/logout",
            "/api/auth/me",
            "/api/auth/logout",
            "/public-cases",
            "/api/public-cases",
            "/solutions/public",
            "/api/solutions/public",
            "/search/public",
            "/api/search/public",
            "/v1/tool-models/leaderboard",
            "/api/v1/tool-models/leaderboard"
    );
}
