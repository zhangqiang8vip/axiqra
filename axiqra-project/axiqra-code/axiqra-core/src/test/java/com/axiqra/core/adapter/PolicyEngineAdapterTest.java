package com.axiqra.core.adapter;

import com.axiqra.common.domain.enums.ScopeEnum;
import com.axiqra.common.port.RbacPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyEngineAdapter 单元测试")
class PolicyEngineAdapterTest {

    @Mock
    private RbacPort rbacPort;

    private PolicyEngineAdapter policyEngineAdapter;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        policyEngineAdapter = new PolicyEngineAdapter(rbacPort);
    }

    @Test
    @DisplayName("默认 scope 应支持标准 code")
    void shouldAllowDefaultCanonicalScopes() {
        assertTrue(policyEngineAdapter.hasScope(USER_ID, ScopeEnum.SEARCH_READ.getCode()));
        assertTrue(policyEngineAdapter.hasScope(USER_ID, ScopeEnum.CONNECT_READ.getCode()));
        assertTrue(policyEngineAdapter.hasScope(USER_ID, ScopeEnum.CONNECT_WRITE.getCode()));
        assertTrue(policyEngineAdapter.hasScope(USER_ID, ScopeEnum.SOLUTION_READ.getCode()));
        assertTrue(policyEngineAdapter.hasScope(USER_ID, ScopeEnum.SOLUTION_WRITE.getCode()));
        assertTrue(policyEngineAdapter.hasScope(USER_ID, ScopeEnum.TRACE_WRITE.getCode()));
        assertTrue(policyEngineAdapter.hasScope(USER_ID, ScopeEnum.TRACE_CONFIRM.getCode()));
        assertTrue(policyEngineAdapter.hasScope(USER_ID, ScopeEnum.CASE_WRITE.getCode()));
        assertTrue(policyEngineAdapter.hasScope(USER_ID, ScopeEnum.FEEDBACK_READ.getCode()));
        assertTrue(policyEngineAdapter.hasScope(USER_ID, ScopeEnum.FEEDBACK_WRITE.getCode()));
    }

    @Test
    @DisplayName("旧 scope 不应被兼容")
    void shouldDenyLegacyScopeAliases() {
        assertFalse(policyEngineAdapter.hasScope(USER_ID, "search:public"));
        assertFalse(policyEngineAdapter.hasScope(USER_ID, "public/read"));
        assertFalse(policyEngineAdapter.hasScope(USER_ID, "public_case:publish"));
    }

    @Test
    @DisplayName("未知 scope 应拒绝")
    void shouldDenyUnknownScope() {
        assertFalse(policyEngineAdapter.hasScope(USER_ID, "unknown:scope"));
    }
}
