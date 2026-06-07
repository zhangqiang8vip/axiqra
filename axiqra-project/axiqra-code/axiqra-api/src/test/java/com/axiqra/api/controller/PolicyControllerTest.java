package com.axiqra.api.controller;

import com.axiqra.common.domain.dto.PolicyEvaluationRequest;
import com.axiqra.common.domain.enums.PolicyDecision;
import com.axiqra.common.domain.vo.PolicyEvaluationVO;
import com.axiqra.core.service.PolicyEngineService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyController 接口测试")
class PolicyControllerTest {

    @Mock
    private PolicyEngineService policyEngineService;

    @InjectMocks
    private PolicyController controller;

    @Captor
    private ArgumentCaptor<PolicyEvaluationRequest> requestCaptor;

    private MockedStatic<cn.dev33.satoken.stp.StpUtil> stpUtilMock;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        stpUtilMock = mockStatic(cn.dev33.satoken.stp.StpUtil.class);
        stpUtilMock.when(cn.dev33.satoken.stp.StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Nested
    @DisplayName("POST /api/policy/evaluate")
    class EvaluateTests {

        @Test
        @DisplayName("应注入当前用户 ID 并返回评估结果")
        void shouldInjectUserIdAndReturnResult() {
            PolicyEvaluationRequest request = new PolicyEvaluationRequest();
            request.setSubjectId(null);
            request.setAction("read");
            request.setObjectType("workspace");

            PolicyEvaluationVO expected = PolicyEvaluationVO.builder()
                    .decision(PolicyDecision.ALLOW)
                    .policyCode("ABAC-READ-MEMBER")
                    .reasonCode("MEMBER_ACCESS")
                    .message("成员可读取私有资源")
                    .build();
            when(policyEngineService.evaluate(any())).thenReturn(expected);

            var response = controller.evaluate(request);

            assertNotNull(response);
            assertNotNull(response.getData());
            assertEquals(PolicyDecision.ALLOW, response.getData().getDecision());
            assertEquals("ABAC-READ-MEMBER", response.getData().getPolicyCode());

            verify(policyEngineService).evaluate(requestCaptor.capture());
            assertEquals(USER_ID, requestCaptor.getValue().getSubjectId());
        }
    }

    @Nested
    @DisplayName("POST /api/policy/enforce")
    class EnforceTests {

        @Test
        @DisplayName("应注入当前用户 ID 并调用 service.enforce")
        void shouldInjectUserIdAndEnforce() {
            PolicyEvaluationRequest request = new PolicyEvaluationRequest();
            request.setSubjectId(null);
            request.setAction("write");
            request.setContext(Map.of("riskLevel", "R1"));

            doNothing().when(policyEngineService).enforce(any());

            var response = controller.enforce(request);

            assertNotNull(response);
            verify(policyEngineService).enforce(requestCaptor.capture());
            assertEquals(USER_ID, requestCaptor.getValue().getSubjectId());
        }
    }

    @Nested
    @DisplayName("GET /api/policy/check")
    class CheckScopeTests {

        @Test
        @DisplayName("应返回 scope 检查结果")
        void shouldReturnScopeCheckResult() {
            when(policyEngineService.hasScope(eq(USER_ID), eq("search:read"))).thenReturn(true);

            var response = controller.checkScope("search:read");

            assertNotNull(response);
            assertNotNull(response.getData());
            assertTrue(response.getData());
            verify(policyEngineService).hasScope(eq(USER_ID), eq("search:read"));
        }

        @Test
        @DisplayName("无 scope 应返回 false")
        void shouldReturnFalseWhenNoScope() {
            when(policyEngineService.hasScope(eq(USER_ID), eq("admin:all"))).thenReturn(false);

            var response = controller.checkScope("admin:all");

            assertNotNull(response);
            assertNotNull(response.getData());
            assertFalse(response.getData());
        }
    }
}
