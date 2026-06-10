package com.axiqra.core.adapter;

import com.axiqra.common.audit.AuditPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AuditAdapterTest {

    @Mock
    private JdbcTemplate auditJdbcTemplate;

    @Test
    @DisplayName("审计写入异常 message 为 null 时不应再次抛错")
    void shouldHandleNullExceptionMessage() {
        AuditAdapter adapter = new AuditAdapter(auditJdbcTemplate, new ObjectMapper());
        RuntimeException writeFailure = new RuntimeException();
        doThrow(writeFailure).when(auditJdbcTemplate).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        AuditPort.AuditEvent event = AuditPort.AuditEvent.builder()
                .requestId("req-1")
                .actorId(1L)
                .actorType("user")
                .action("create")
                .objectType("workspace")
                .objectId(1L)
                .result("success")
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .payload(Map.of("k", "v"))
                .tenantId(1L)
                .build();

        assertDoesNotThrow(() -> adapter.log(event));
    }
}
