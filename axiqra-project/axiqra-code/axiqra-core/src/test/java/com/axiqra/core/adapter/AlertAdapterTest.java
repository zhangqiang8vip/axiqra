package com.axiqra.core.adapter;

import com.axiqra.common.port.AlertPort.AlertEvent;
import com.axiqra.common.port.AlertPort.AlertType;
import com.axiqra.common.port.AlertPort.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertAdapter 单元测试")
class AlertAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private AlertAdapter alertAdapter;

    @BeforeEach
    void setUp() {
        alertAdapter = new AlertAdapter(restTemplate, new ObjectMapper());
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
        ReflectionTestUtils.setField(alertAdapter, "enabled", true);
        ReflectionTestUtils.setField(alertAdapter, "webhookUrl", "https://127.0.0.1/alerts");
        AlertEvent event = AlertEvent.builder()
                .type(AlertType.POLICY_DENIED)
                .severity(Severity.WARNING)
                .build();

        alertAdapter.sendAlert(event);

        verifyNoInteractions(restTemplate);
    }
}
