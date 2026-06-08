package com.axiqra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Alert Webhook RestTemplate 配置
 *
 * @author Axiqra Team
 * @date 2026-06-09
 */
@Configuration
public class AlertConfig {

    @Value("${alert.webhook.timeout-ms:5000}")
    private int timeoutMs;

    @Bean
    public RestTemplate alertRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return builder.requestFactory(() -> factory).build();
    }
}
