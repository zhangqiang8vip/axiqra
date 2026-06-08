package com.axiqra.core.service.impl;

import com.axiqra.common.domain.dto.PolicyEvaluationRequest;
import com.axiqra.common.domain.enums.PolicyDecision;
import com.axiqra.common.domain.vo.PolicyEvaluationVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.port.AlertPort;
import com.axiqra.common.port.PolicyEnginePort;
import com.axiqra.core.service.PolicyEngineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyEngineServiceImpl 单元测试")
class PolicyEngineServiceImplTest {

    @Mock
    private PolicyEnginePort policyEnginePort;

    @Mock
    private AlertPort alertPort;

    @InjectMocks
    private PolicyEngineServiceImpl policyEngineService;

    private static final Long USER_ID = 1L;

    private void verifyPolicyDeniedWarningAlert() {
        verifyPolicyDeniedAlert(AlertPort.Severity.WARNING, null);
    }

    private void verifyPolicyDeniedCriticalAlert(Long workspaceId) {
        verifyPolicyDeniedAlert(AlertPort.Severity.CRITICAL, workspaceId);
    }

    private void verifyPolicyDeniedAlert(AlertPort.Severity severity, Long workspaceId) {
        verify(alertPort).sendAlert(argThat(event -> event != null
                && event.type() == AlertPort.AlertType.POLICY_DENIED
                && event.severity() == severity
                && USER_ID.equals(event.actorId())
                && (workspaceId == null ? event.workspaceId() == null : workspaceId.equals(event.workspaceId()))));
    }

    @Nested
    @DisplayName("evaluate")
    class EvaluateTests {

        @Test
        @DisplayName("应返回 policyEnginePort.evaluate 的结果")
        void shouldReturnFromPort() {
            PolicyEvaluationRequest request = new PolicyEvaluationRequest();
            request.setSubjectId(USER_ID);
            request.setAction("read");
            PolicyEvaluationVO expected = PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.ALLOW)
                    .policyCode("ABAC-READ-MEMBER")
                    .reasonCode("MEMBER_ACCESS")
                    .message("成员可读取私有资源")
                    .build();
            when(policyEnginePort.evaluate(request)).thenReturn(expected);

            PolicyEvaluationVO result = policyEngineService.evaluate(request);

            assertEquals(PolicyDecision.ALLOW, result.getDecision());
            assertEquals("ABAC-READ-MEMBER", result.getPolicyCode());
            verify(policyEnginePort).evaluate(request);
        }
    }

    @Nested
    @DisplayName("hasScope")
    class HasScopeTests {

        @Test
        @DisplayName("应代理到 policyEnginePort.hasScope")
        void shouldDelegateToPort() {
            when(policyEnginePort.hasScope(USER_ID, "search:read")).thenReturn(true);

            boolean result = policyEngineService.hasScope(USER_ID, "search:read");

            assertTrue(result);
            verify(policyEnginePort).hasScope(USER_ID, "search:read");
        }

        @Test
        @DisplayName("应返回 policyEnginePort 的结果")
        void shouldReturnFalseWhenPortReturnsFalse() {
            when(policyEnginePort.hasScope(USER_ID, "admin:all")).thenReturn(false);

            boolean result = policyEngineService.hasScope(USER_ID, "admin:all");

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("enforce")
    class EnforceTests {

        @Test
        @DisplayName("策略允许时应正常返回不抛异常")
        void shouldNotThrowWhenAllowed() {
            PolicyEvaluationRequest request = new PolicyEvaluationRequest();
            request.setSubjectId(USER_ID);
            request.setAction("read");
            PolicyEvaluationVO allowed = PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.ALLOW)
                    .policyCode("ABAC-READ-MEMBER")
                    .reasonCode("MEMBER_ACCESS")
                    .message("成员可读取私有资源")
                    .build();
            when(policyEnginePort.evaluate(request)).thenReturn(allowed);

            assertDoesNotThrow(() -> policyEngineService.enforce(request));
        }

        @Test
        @DisplayName("策略拒绝时应抛 BizException")
        void shouldThrowWhenDenied() {
            PolicyEvaluationRequest request = new PolicyEvaluationRequest();
            request.setSubjectId(USER_ID);
            request.setAction("delete");
            PolicyEvaluationVO denied = PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.DENY)
                    .policyCode("ABAC-DELETE-DENY")
                    .reasonCode("NOT_ADMIN")
                    .message("只有管理员或所有者可以删除资源")
                    .build();
            when(policyEnginePort.evaluate(request)).thenReturn(denied);

            BizException ex = assertThrows(BizException.class,
                    () -> policyEngineService.enforce(request));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("策略拒绝"));
            verifyPolicyDeniedWarningAlert();
        }

        @Test
        @DisplayName("DENY_SCOPE_MISSING 应抛 FORBIDDEN")
        void shouldThrowWhenScopeMissing() {
            PolicyEvaluationRequest request = new PolicyEvaluationRequest();
            request.setSubjectId(USER_ID);
            request.setAction("write");
            PolicyEvaluationVO denied = PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.DENY_SCOPE_MISSING)
                    .policyCode("ABAC-WRITE-DENY")
                    .reasonCode("INSUFFICIENT_ROLE")
                    .message("写入资源至少需要 member 角色")
                    .build();
            when(policyEnginePort.evaluate(request)).thenReturn(denied);

            BizException ex = assertThrows(BizException.class,
                    () -> policyEngineService.enforce(request));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
            verifyPolicyDeniedWarningAlert();
        }

        @Test
        @DisplayName("告警发送失败时仍应抛原始 FORBIDDEN")
        void shouldStillThrowForbiddenWhenAlertFails() {
            PolicyEvaluationRequest request = new PolicyEvaluationRequest();
            request.setSubjectId(USER_ID);
            request.setAction("write");
            PolicyEvaluationVO denied = PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.DENY)
                    .policyCode("ABAC-WRITE-DENY")
                    .reasonCode("INSUFFICIENT_ROLE")
                    .message("写入资源至少需要 member 角色")
                    .build();
            when(policyEnginePort.evaluate(request)).thenReturn(denied);
            doThrow(new IllegalStateException()).when(alertPort).sendAlert(any());

            BizException ex = assertThrows(BizException.class,
                    () -> policyEngineService.enforce(request));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
            verify(alertPort).sendAlert(any());
        }

        @Test
        @DisplayName("DENY_RISK_LEVEL_TOO_HIGH 应抛 FORBIDDEN")
        void shouldThrowWhenRiskLevelTooHigh() {
            PolicyEvaluationRequest request = new PolicyEvaluationRequest();
            request.setSubjectId(USER_ID);
            request.setAction("write");
            request.setContext(new java.util.HashMap<>() {{
                put("riskLevel", "R4");
                put("executionMode", "AUTO");
                put("workspaceId", "100");
            }});
            PolicyEvaluationVO denied = PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.DENY_RISK_LEVEL_TOO_HIGH)
                    .policyCode("ABAC-RISK-DENY")
                    .reasonCode("RISK_LEVEL_TOO_HIGH")
                    .message("风险等级 R4/R5 仅允许手动模式执行")
                    .build();
            when(policyEnginePort.evaluate(request)).thenReturn(denied);

            BizException ex = assertThrows(BizException.class,
                    () -> policyEngineService.enforce(request));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
            verifyPolicyDeniedCriticalAlert(100L);
        }
    }
}
