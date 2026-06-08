package com.axiqra.core.adapter;

import com.axiqra.common.port.AlertPort;
import com.axiqra.common.port.AlertPort.AlertEvent;
import com.axiqra.common.port.AlertPort.Severity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * HTTP Webhook 告警适配器
 *
 * <p>将告警事件通过 HTTP POST 发送到配置的 webhook URL。
 * 配置项：
 * <ul>
 *   <li>{@code alert.webhook.url} - Webhook HTTPS 端点 URL（启用时必填）</li>
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

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AlertAdapter(@Qualifier("alertRestTemplate") RestTemplate restTemplate,
                        ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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
            if (isUnsafeHost(host)) {
                log.warn("[Alert] Unsafe webhook host rejected, skipping alert: {}", host);
                return null;
            }
            return uri;
        } catch (IllegalArgumentException e) {
            log.warn("[Alert] Malformed webhook URL configured, skipping alert: {}",
                    messageOrUnknown(e));
            return null;
        }
    }

    private boolean isUnsafeHost(String host) {
        String lookupHost = host;
        if (lookupHost.startsWith("[") && lookupHost.endsWith("]")) {
            lookupHost = lookupHost.substring(1, lookupHost.length() - 1);
        }
        String normalizedHost = lookupHost.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalizedHost) || normalizedHost.endsWith(".localhost")) {
            return true;
        }
        if (isIpLiteral(normalizedHost)) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(lookupHost);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()
                    || isUniqueLocalIpv6Address(address);
        } catch (UnknownHostException e) {
            log.warn("[Alert] Unable to resolve webhook host, skipping alert: {}", host);
            return true;
        }
    }

    private boolean isIpLiteral(String host) {
        return host.contains(":") || host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }

    private boolean isUniqueLocalIpv6Address(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }

    private String messageOrUnknown(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "unknown";
    }
}
