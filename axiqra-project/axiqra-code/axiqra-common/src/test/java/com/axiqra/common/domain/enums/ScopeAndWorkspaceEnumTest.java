package com.axiqra.common.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Scope 和 Workspace 枚举解析测试")
class ScopeAndWorkspaceEnumTest {

    @Test
    @DisplayName("WorkspaceType 只解析标准值")
    void shouldParseOnlyWorkspaceTypeCanonicalCodes() {
        assertEquals(WorkspaceType.PERSONAL, WorkspaceType.of(" Personal "));
        assertEquals(WorkspaceType.TEAM, WorkspaceType.of("team"));
        assertEquals(WorkspaceType.ENTERPRISE, WorkspaceType.of("enterprise"));
        assertNull(WorkspaceType.of("organization"));
        assertNull(WorkspaceType.of("government"));
        assertNull(WorkspaceType.of("public"));
        assertNull(WorkspaceType.of("unknown"));
    }

    @Test
    @DisplayName("VisibilityScope 只解析标准值")
    void shouldParseOnlyVisibilityScopeCanonicalCodes() {
        assertEquals(VisibilityScope.PRIVATE, VisibilityScope.of(" Private "));
        assertEquals(VisibilityScope.WORKSPACE, VisibilityScope.of("workspace"));
        assertEquals(VisibilityScope.ENTERPRISE, VisibilityScope.of("enterprise"));
        assertEquals(VisibilityScope.PUBLIC, VisibilityScope.of("public"));
        assertNull(VisibilityScope.of("private_only"));
        assertNull(VisibilityScope.of("team_only"));
        assertNull(VisibilityScope.of("enterprise_only"));
    }

    @Test
    @DisplayName("ScopeEnum 只解析标准 resource:action")
    void shouldParseOnlyScopeCanonicalCodes() {
        assertNull(ScopeEnum.of(null));
        assertEquals(ScopeEnum.SEARCH_READ, ScopeEnum.of(" search:read "));
        assertEquals(ScopeEnum.SOLUTION_PUBLISH, ScopeEnum.of("solution:publish"));
        assertEquals(ScopeEnum.SOLUTION_MAINTAIN, ScopeEnum.of("solution:maintain"));
        assertEquals(ScopeEnum.CASE_PUBLISH, ScopeEnum.of("case:publish"));
        assertEquals(ScopeEnum.PUBLIC_READ, ScopeEnum.of("public:read"));
        assertNull(ScopeEnum.of("search:public"));
        assertNull(ScopeEnum.of("public_search:read"));
        assertNull(ScopeEnum.of("public_case:publish"));
        assertNull(ScopeEnum.of("public/read"));
        assertNull(ScopeEnum.of("unknown:scope"));
    }
}
