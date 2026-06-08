package com.axiqra.core.adapter;

import com.axiqra.common.port.AlertPort;
import com.axiqra.common.port.AlertPort.AlertEvent;
import com.axiqra.common.port.AlertPort.Severity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * HTTP Webhook 告警适配器
 *
 * <p>将告警事件通过 HTTP POST 发送到配置的 webhook URL。
 * 配置项：
 * <ul>
 *   <li>{@code alert.webhook.url} - Webhook HTTPS 端点 URL（启用时必填）</li>
 *   <li>{@code alert.webhook.allowed-hosts} - Webhook 域名白名单，逗号分隔，启用时必填；仅支持域名精确匹配，IP 字面量不允许</li>
 *   <li>{@code alert.webhook.enabled} - 是否启用（默认 true）</li>
 *   <li>{@code alert.webhook.timeout-ms} - 超时毫秒数（默认 5000）</li>
 * </ul>
 *
 * <p>发送失败时仅记录 warn 日志，不阻断主业务流程。
 *
 * @author Axiqra Team
 * @date 2026-06-09
 */
@Slf4j
@Component
public class AlertAdapter implements AlertPort {

    private static final String HEADER_ALERT_TYPE = "X-Alert-Type";
    private static final String HEADER_ALERT_SEVERITY = "X-Alert-Severity";

    @Value("${alert.webhook.url:}")
    private String webhookUrl;

    @Value("${alert.webhook.enabled:true}")
    private boolean enabled;

    @Value("${alert.webhook.allowed-hosts:}")
    private String allowedHosts;

    private Set<String> allowedHostSet = Collections.emptySet();

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AlertAdapter(@Qualifier("alertRestTemplate") RestTemplate restTemplate,
                        ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void initializeAllowedHosts() {
        if (allowedHosts == null || allowedHosts.isBlank()) {
            allowedHostSet = Collections.emptySet();
            return;
        }

        Set<String> normalizedHosts = new HashSet<>();
        for (String configuredHost : allowedHosts.split(",")) {
            String normalizedHost = normalizeHost(configuredHost);
            if (normalizedHost.isBlank() || isUnsafeHostLiteral(normalizedHost)) {
                log.warn("[Alert] Ignoring unsafe webhook allowlist host: {}", configuredHost);
                continue;
            }
            normalizedHosts.add(normalizedHost);
        }
        allowedHostSet = Collections.unmodifiableSet(normalizedHosts);
    }

    @Override
    public void sendAlert(AlertEvent event) {
        if (event == null) {
            log.warn("[Alert] Null alert event, skipping");
            return;
        }
        if (!enabled) {
            log.debug("[Alert] Webhook disabled, skipping alert type={}", event.type());
            return;
        }
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[Alert] Webhook enabled but URL is blank, skipping alert type={}", event.type());
            return;
        }

        if (event.severity() == Severity.INFO) {
            log.debug("[Alert] INFO alert suppressed, type={}", event.type());
            return;
        }

        URI webhookUri = resolveWebhookUri();
        if (webhookUri == null) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HEADER_ALERT_TYPE, event.type().name());
            headers.set(HEADER_ALERT_SEVERITY, event.severity().name());

            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(webhookUri, request, String.class);

            log.info("[Alert] Sent alert type={}, severity={}, actorId={}",
                    event.type(), event.severity(), event.actorId());
        } catch (JsonProcessingException e) {
            log.warn("[Alert] Failed to serialize alert event, type={}: {}",
                    event.type(), messageOrUnknown(e));
        } catch (ResourceAccessException e) {
            log.warn("[Alert] Failed to access alert webhook, type={}, url={}: {}",
                    event.type(), webhookUri, messageOrUnknown(e));
        } catch (RestClientResponseException e) {
            log.warn("[Alert] Alert webhook returned error, type={}, url={}, status={}: {}",
                    event.type(), webhookUri, e.getStatusCode().value(), messageOrUnknown(e));
        } catch (RestClientException e) {
            log.warn("[Alert] Failed to send alert webhook, type={}, url={}: {}",
                    event.type(), webhookUri, messageOrUnknown(e));
        } catch (RuntimeException e) {
            log.warn("[Alert] Unexpected alert failure, type={}, url={}: {}",
                    event.type(), webhookUri, messageOrUnknown(e));
        }
    }

    private URI resolveWebhookUri() {
        try {
            URI uri = URI.create(webhookUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            boolean supportedScheme = "https".equalsIgnoreCase(scheme);
            if (!supportedScheme || host == null) {
                log.warn("[Alert] Invalid HTTPS webhook URL configured, skipping alert: {}", webhookUrl);
                return null;
            }
            String normalizedHost = normalizeHost(host);
            if (isUnsafeHostLiteral(normalizedHost)) {
                log.warn("[Alert] Unsafe webhook host rejected, skipping alert: {}", host);
                return null;
            }
            if (allowedHostSet.isEmpty()) {
                log.warn("[Alert] Webhook enabled but host allowlist is blank or has no valid domain entries, skipping alert: {}",
                        host);
                return null;
            }
            if (!isAllowedHost(normalizedHost)) {
                log.warn("[Alert] Webhook host not in allowlist, skipping alert: {} (normalized: {})",
                        host, normalizedHost);
                return null;
            }
            return uri;
        } catch (IllegalArgumentException e) {
            log.warn("[Alert] Malformed webhook URL configured, skipping alert: {}",
                    messageOrUnknown(e));
            return null;
        }
    }

    private String normalizeHost(String host) {
        String lookupHost = host == null ? "" : host.trim();
        if (lookupHost.startsWith("[") && lookupHost.endsWith("]")) {
            lookupHost = lookupHost.substring(1, lookupHost.length() - 1);
        }
        lookupHost = lookupHost.toLowerCase(Locale.ROOT);
        while (lookupHost.endsWith(".")) {
            lookupHost = lookupHost.substring(0, lookupHost.length() - 1);
        }
        return lookupHost;
    }

    private boolean isUnsafeHostLiteral(String host) {
        return "localhost".equals(host)
                || host.endsWith(".localhost")
                || isIpLiteral(host);
    }

    private boolean isAllowedHost(String host) {
        return allowedHostSet.contains(host);
    }

    private boolean isIpLiteral(String host) {
        return isIpv4Literal(host) || isIpv6Literal(host);
    }

    private boolean isIpv4Literal(String host) {
        return host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }

    private boolean isIpv6Literal(String host) {
        return host.contains(":");
    }

    private String messageOrUnknown(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "unknown";
    }
}
