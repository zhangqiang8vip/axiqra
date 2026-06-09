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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
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
    @DisplayName("即使在白名单内，回环 IP webhook 地址也应被拒绝")
    void shouldRejectWebhookHostWhenLoopbackIpIsAllowlisted() {
        enableWebhook("https://127.0.0.1/alerts", "127.0.0.1");
        AlertEvent event = warningEvent();

        alertAdapter.sendAlert(event);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("即使在白名单内，公网 IP webhook 地址也应被拒绝")
    void shouldRejectWebhookHostWhenPublicIpIsAllowlisted() {
        enableWebhook("https://8.8.8.8/alerts", "8.8.8.8");
        AlertEvent event = warningEvent();

        alertAdapter.sendAlert(event);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("即使在白名单内，IPv6 webhook 地址也应被拒绝")
    void shouldRejectWebhookHostWhenIpv6LiteralIsAllowlisted() {
        enableWebhook("https://[::1]/alerts", "::1");
        AlertEvent event = warningEvent();

        alertAdapter.sendAlert(event);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("未在白名单内的 webhook 域名应被拒绝")
    void shouldRejectWebhookHostOutsideAllowlist() {
        enableWebhook("https://example.com/alerts", "hooks.example.com");
        AlertEvent event = warningEvent();

        alertAdapter.sendAlert(event);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("格式错误的 webhook URL 应被跳过")
    void shouldRejectMalformedWebhookUrl() {
        enableWebhook("not a webhook url", "example.com");
        AlertEvent event = warningEvent();

        alertAdapter.sendAlert(event);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("带尾点的 localhost webhook 地址应被拒绝")
    void shouldRejectTrailingDotLocalhostWebhookHost() {
        enableWebhook("https://localhost./alerts", "hooks.example.com");
        AlertEvent event = warningEvent();

        alertAdapter.sendAlert(event);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("白名单为空时 webhook 应失败关闭")
    void shouldRejectWebhookWhenAllowlistIsBlank() {
        enableWebhook("https://example.com/alerts", " ");
        AlertEvent event = warningEvent();

        alertAdapter.sendAlert(event);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("带空格的不安全白名单条目应被忽略")
    void shouldIgnoreUnsafeAllowlistEntries() {
        enableWebhook("https://example.com/alerts", " localhost. , 127.0.0.1 , [::1] ");
        AlertEvent event = warningEvent();

        alertAdapter.sendAlert(event);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("有效告警应发送到 webhook")
    void shouldPostWarningAlertToWebhook() throws Exception {
        URI webhookUri = URI.create("https://example.com/alerts");
        enableWebhook(webhookUri.toString(), "example.com");
        AlertEvent event = warningEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");

        alertAdapter.sendAlert(event);

        verify(restTemplate).postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("白名单域名应统一大小写、空格和尾点")
    void shouldPostWarningAlertWhenAllowlistHostNeedsNormalization() throws Exception {
        URI webhookUri = URI.create("https://example.com/alerts");
        enableWebhook(webhookUri.toString(), " Example.COM. ");
        AlertEvent event = warningEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");

        alertAdapter.sendAlert(event);

        verify(restTemplate).postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("序列化失败不应阻断调用方")
    void shouldNotThrowWhenSerializationFails() throws Exception {
        enableWebhook("https://example.com/alerts", "example.com");
        AlertEvent event = warningEvent();
        when(objectMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("boom") {});

        assertDoesNotThrow(() -> alertAdapter.sendAlert(event));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("webhook 网络异常不应阻断调用方")
    void shouldNotThrowWhenWebhookRequestFails() throws Exception {
        URI webhookUri = URI.create("https://example.com/alerts");
        enableWebhook(webhookUri.toString(), "example.com");
        AlertEvent event = warningEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(restTemplate.postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        assertDoesNotThrow(() -> alertAdapter.sendAlert(event));
        verify(restTemplate).postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("webhook 返回 HTTP 错误不应阻断调用方")
    void shouldNotThrowWhenWebhookReturnsHttpError() throws Exception {
        URI webhookUri = URI.create("https://example.com/alerts");
        enableWebhook(webhookUri.toString(), "example.com");
        AlertEvent event = warningEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(restTemplate.postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "bad gateway"));

        assertDoesNotThrow(() -> alertAdapter.sendAlert(event));
        verify(restTemplate).postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("webhook 未预期运行时异常不应阻断调用方")
    void shouldNotThrowWhenUnexpectedRuntimeExceptionOccurs() throws Exception {
        URI webhookUri = URI.create("https://example.com/alerts");
        enableWebhook(webhookUri.toString(), "example.com");
        AlertEvent event = warningEvent();
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(restTemplate.postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(() -> alertAdapter.sendAlert(event));
        verify(restTemplate).postForEntity(eq(webhookUri), any(HttpEntity.class), eq(String.class));
    }

    private void enableWebhook(String webhookUrl) {
        enableWebhook(webhookUrl, "example.com");
    }

    private void enableWebhook(String webhookUrl, String allowedHosts) {
        ReflectionTestUtils.setField(alertAdapter, "enabled", true);
        ReflectionTestUtils.setField(alertAdapter, "webhookUrl", webhookUrl);
        ReflectionTestUtils.setField(alertAdapter, "allowedHosts", allowedHosts);
        alertAdapter.initializeAllowedHosts();
    }

    private AlertEvent warningEvent() {
        return AlertEvent.builder()
                .type(AlertType.POLICY_DENIED)
                .severity(Severity.WARNING)
                .actorId(1L)
                .actorType("user")
                .objectType("workspace")
                .objectId(100L)
                .message("策略拒绝")
                .reasonCode("INSUFFICIENT_ROLE")
                .build();
    }
}
