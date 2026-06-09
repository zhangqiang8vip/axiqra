package com.axiqra.core.service.impl;

import com.axiqra.common.audit.AuditPort;
import com.axiqra.common.domain.dto.ConnectSessionCreateRequest;
import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.port.ConnectSessionPort;
import com.axiqra.core.service.QuotaService;
import com.axiqra.core.service.RateLimitService;
import com.axiqra.core.service.RbacService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectServiceImpl 单元测试")
class ConnectServiceImplTest {

    @Mock
    private QuotaService quotaService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private RbacService rbacService;
    @Mock
    private AuditPort auditPort;
    @Mock
    private ConnectSessionPort connectSessionPort;

    @InjectMocks
    private ConnectServiceImpl connectService;

    @Test
    void doctorShouldReturnEightChecks() {
        when(rbacService.hasScope(1L, "connect:read")).thenReturn(true);
        when(rbacService.hasScope(1L, "connect:write")).thenReturn(true);
        MembershipEntity membership = new MembershipEntity();
        membership.setWorkspaceId(100L);
        membership.setRole(MemberRole.ADMIN.getCode());
        when(rbacService.getMemberships(1L)).thenReturn(List.of(membership));

        var doctor = connectService.runDoctor(1L, "cli", "mcp", 100L);

        assertEquals(8, doctor.getTotalChecks());
        assertTrue(doctor.getPassedChecks() >= 6);
    }

    @Test
    void createSessionShouldConsumeQuotaAndReturnReadySession() {
        when(rbacService.hasScope(1L, "connect:read")).thenReturn(true);
        when(rbacService.hasScope(1L, "connect:write")).thenReturn(true);
        MembershipEntity membership = new MembershipEntity();
        membership.setWorkspaceId(100L);
        when(rbacService.getMemberships(1L)).thenReturn(List.of(membership));

        ConnectSessionCreateRequest request = new ConnectSessionCreateRequest();
        request.setChannel("cli");
        request.setToolType("mcp");
        request.setTargetType("solution");
        request.setTargetId(10L);
        request.setWorkspaceId(100L);

        var session = connectService.createSession(1L, request);

        assertNotNull(session.getSessionId());
        assertNotNull(session.getHistory());
        assertEquals("READY", session.getStatus());
        verify(connectSessionPort).save(any());
        verify(rateLimitService).checkOrThrow(1L, "connect:create");
        verify(quotaService).consumeOrThrow(1L, "connect_session_daily");
        verify(auditPort).logInvocation(any());
    }

    @Test
    void listAndGetSessionShouldUseStore() {
        var session = com.axiqra.common.domain.vo.ConnectSessionVO.builder().sessionId("s-1").userId(1L).build();
        when(connectSessionPort.listByUser(1L)).thenReturn(List.of(session));
        when(connectSessionPort.get("s-1")).thenReturn(Optional.of(session));

        assertEquals(1, connectService.listSessions(1L).size());
        assertEquals("s-1", connectService.getSession(1L, "s-1").getSessionId());
    }
}
