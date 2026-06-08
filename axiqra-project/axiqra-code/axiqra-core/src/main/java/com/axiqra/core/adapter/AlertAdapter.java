package com.axiqra.core.adapter;

import com.axiqra.common.port.AlertPort;
import com.axiqra.common.port.AlertPort.AlertEvent;
import com.axiqra.common.port.AlertPort.Severity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP Webhook 告警适配器
 *
 * <p>将告警事件通过 HTTP POST 发送到配置的 webhook URL。
 * 配置项：
 * <ul>
 *   <li>{@code alert.webhook.url} - Webhook 端点 URL（必填）</li>
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
@RequiredArgsConstructor
public class AlertAdapter implements AlertPort {

    @Value("${alert.webhook.url:}")
    private String webhookUrl;

    @Value("${alert.webhook.enabled:true}")
    private boolean enabled;

    @Value("${alert.webhook.timeout-ms:5000}")
    private int timeoutMs;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void sendAlert(AlertEvent event) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("[Alert] Webhook not configured or disabled, skipping alert type={}", event.type());
            return;
        }

        if (event.severity() == Severity.INFO) {
            log.debug("[Alert] INFO alert suppressed, type={}", event.type());
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Alert-Type", event.type().name());
            headers.set("X-Alert-Severity", event.severity().name());

            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(webhookUrl, request, String.class);

            log.info("[Alert] Sent alert type={}, severity={}, actorId={}",
                    event.type(), event.severity(), event.actorId());
        } catch (JsonProcessingException e) {
            log.warn("[Alert] Failed to serialize alert event, type={}: {}",
                    event.type(), e.getMessage());
        } catch (Exception e) {
            log.warn("[Alert] Failed to send alert webhook, type={}, url={}: {}",
                    event.type(), webhookUrl, e.getMessage());
        }
    }
}
