package com.axiqra.core.adapter;

import com.axiqra.common.port.AlertPort.AlertEvent;
import com.axiqra.common.port.AlertPort.AlertType;
import com.axiqra.common.port.AlertPort.Severity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertAdapter 单元测试")
class AlertAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private AlertAdapter alertAdapter;

    @BeforeEach
    void setUp() {
        alertAdapter = new AlertAdapter(restTemplate, objectMapper);
    }

    @Test
    @DisplayName("空告警事件应直接跳过")
    void shouldSkipNullAlertEvent() {
        alertAdapter.sendAlert(null);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("内网 webhook 地址应被拒绝")
    void shouldRejectInternalWebhookHost() {
        enableWebhook("https://127.0.0.1/alerts");
        AlertEvent event = warningEvent();

        alertAdapter.sendAlert(event);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("公网 IP webhook 地址也应被拒绝")
    void shouldRejectPublicIpWebhookHost() {
        enableWebhook("https://8.8.8.8/alerts");
        AlertEvent event = warningEvent();

        alertAdapter.sendAlert(event);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("有效告警应发送到 webhook")
    void shouldPostWarningAlertToWebhook() throws Exception {
        URI webhookUri = URI.create("https://example.com/alerts");
        enableWebhook(webhookUri.toString());
        AlertEvent event = warningEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");

        alertAdapter.sendAlert(event);

        verify(restTemplate).postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("序列化失败不应阻断调用方")
    void shouldNotThrowWhenSerializationFails() throws Exception {
        enableWebhook("https://example.com/alerts");
        AlertEvent event = warningEvent();
        when(objectMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("boom") {});

        assertDoesNotThrow(() -> alertAdapter.sendAlert(event));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("webhook 网络异常不应阻断调用方")
    void shouldNotThrowWhenWebhookRequestFails() throws Exception {
        URI webhookUri = URI.create("https://example.com/alerts");
        enableWebhook(webhookUri.toString());
        AlertEvent event = warningEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(restTemplate.postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        assertDoesNotThrow(() -> alertAdapter.sendAlert(event));
        verify(restTemplate).postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("webhook 未预期运行时异常不应阻断调用方")
    void shouldNotThrowWhenUnexpectedRuntimeExceptionOccurs() throws Exception {
        URI webhookUri = URI.create("https://example.com/alerts");
        enableWebhook(webhookUri.toString());
        AlertEvent event = warningEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(restTemplate.postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(() -> alertAdapter.sendAlert(event));
        verify(restTemplate).postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class));
    }

    private void enableWebhook(String webhookUrl) {
        ReflectionTestUtils.setField(alertAdapter, "enabled", true);
        ReflectionTestUtils.setField(alertAdapter, "webhookUrl", webhookUrl);
    }

    private AlertEvent warningEvent() {
        return AlertEvent.builder()
                .type(AlertType.POLICY_DENIED)
                .severity(Severity.WARNING)
                .build();
    }
}
