package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.port.QuotaInfo;
import com.axiqra.common.port.QuotaPort;
import com.axiqra.common.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuotaServiceImpl 单元测试")
class QuotaServiceImplTest {

    @Mock
    private QuotaPort quotaPort;
    @Mock
    private AuditPort auditPort;

    @InjectMocks
    private QuotaServiceImpl quotaService;

    @Test
    void getStatusShouldUseQuotaPort() {
        when(quotaPort.getQuotaInfo(1L)).thenReturn(QuotaInfo.of(2, 2000));

        var result = quotaService.getStatus(1L);

        assertEquals(2, result.getUsed());
        assertEquals(1998, result.getRemaining());
    }

    @Test
    void consumeShouldThrowWhenQuotaExceeded() {
        when(quotaPort.getQuotaInfo(1L))
                .thenReturn(QuotaInfo.of(2000, 2000))
                .thenReturn(QuotaInfo.of(2000, 2000));
        when(quotaPort.tryConsumeQuota(1L)).thenReturn(false);

        assertThrows(BizException.class, () -> quotaService.consumeOrThrow(1L, "connect_session_daily"));
        verify(auditPort).logQuotaEvent(any());
    }
}
