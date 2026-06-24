package com.axiqra.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties({HealthProbeProperties.class, CorsProperties.class})
public class SecurityConfig {

    private final CorsProperties corsProperties;

    public SecurityConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, PublicEndpointPaths.SPRING_PUBLIC_GET).permitAll()
                        .requestMatchers(HttpMethod.POST, "/search/public").permitAll()
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/auth/captcha"
                        ).permitAll()
                        // 所有其他接口由 Sa-Token 拦截器在 Controller 层做认证检查
                        // Spring Security 只负责 CORS 和健康检查放行，禁用自身的认证判断
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = corsProperties.getAllowedOrigins();
        if (origins != null && !origins.isEmpty()) {
            boolean hasWildcard = origins.stream().anyMatch(o -> "*".equals(o.trim()));
            if (hasWildcard && Boolean.TRUE.equals(corsProperties.getAllowCredentials())) {
                throw new IllegalStateException(
                        "CORS config contains wildcard origin '*' with allowCredentials=true, which is not allowed");
            }
            config.setAllowedOrigins(origins);
            config.setAllowCredentials(Boolean.TRUE.equals(corsProperties.getAllowCredentials()));
        }
        List<String> methods = corsProperties.getAllowedMethods();
        config.setAllowedMethods(methods != null ? methods : List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        List<String> headers = corsProperties.getAllowedHeaders();
        config.setAllowedHeaders(headers != null ? headers : List.of("*"));
        List<String> exposed = corsProperties.getExposedHeaders();
        config.setExposedHeaders(exposed != null ? exposed : List.of("Authorization", "X-Trace-Id", "X-Request-Id"));
        Long maxAge = corsProperties.getMaxAge();
        config.setMaxAge(maxAge != null ? maxAge : 3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
