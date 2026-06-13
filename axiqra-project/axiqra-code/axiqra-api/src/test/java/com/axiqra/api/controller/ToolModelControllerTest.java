package com.axiqra.api.controller;

import com.axiqra.common.domain.vo.ToolModelLeaderboardVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.ToolModelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolModelController 接口测试")
class ToolModelControllerTest {

    @Mock
    private ToolModelService toolModelService;

    @InjectMocks
    private ToolModelController controller;

    @Test
    @DisplayName("GET /api/v1/tool-models/leaderboard 应返回排行榜")
    void getLeaderboardShouldReturn() {
        ToolModelLeaderboardVO vo = new ToolModelLeaderboardVO();
        vo.setToolName("cursor");
        vo.setReportedModelName("gpt-4o");
        vo.setSuccessRate7d(new BigDecimal("95.5"));
        vo.setSampleSize(1000);
        vo.setRank(1);

        when(toolModelService.getLeaderboard("global", null, null, 50))
                .thenReturn(List.of(vo));

        var result = controller.getLeaderboard("global", null, null, 50);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(1, result.getBody().getData().size());
        assertEquals("cursor", result.getBody().getData().get(0).getToolName());
        assertEquals(new BigDecimal("95.5"), result.getBody().getData().get(0).getSuccessRate7d());
        verify(toolModelService).getLeaderboard("global", null, null, 50);
    }

    @Test
    @DisplayName("GET /api/v1/tool-models/leaderboard 带 toolName 应过滤")
    void getLeaderboardWithToolNameShouldFilter() {
        ToolModelLeaderboardVO vo = new ToolModelLeaderboardVO();
        vo.setToolName("cursor");
        vo.setReportedModelName("gpt-4o");

        when(toolModelService.getLeaderboard("public", null, "cursor", 50))
                .thenReturn(List.of(vo));

        var result = controller.getLeaderboard("public", null, "cursor", 50);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(1, result.getBody().getData().size());
        verify(toolModelService).getLeaderboard("public", null, "cursor", 50);
    }

    @Test
    @DisplayName("GET /api/v1/tool-models/leaderboard 返回空排行榜")
    void getLeaderboardShouldReturnEmptyList() {
        when(toolModelService.getLeaderboard("global", null, null, 50))
                .thenReturn(List.of());

        var result = controller.getLeaderboard("global", null, null, 50);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCode());
        assertEquals(0, result.getBody().getData().size());
    }

    @Test
    @DisplayName("getLeaderboard limit=1 应成功调用 service")
    void getLeaderboardShouldSucceedWithLimitOne() {
        when(toolModelService.getLeaderboard("scope", null, null, 1))
                .thenReturn(List.of());

        var result = controller.getLeaderboard("scope", null, null, 1);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        verify(toolModelService).getLeaderboard("scope", null, null, 1);
    }

    @Test
    @DisplayName("getLeaderboard limit=500 应成功调用 service")
    void getLeaderboardShouldSucceedWithLimitFiveHundred() {
        when(toolModelService.getLeaderboard("scope", null, null, 500))
                .thenReturn(List.of());

        var result = controller.getLeaderboard("scope", null, null, 500);

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        verify(toolModelService).getLeaderboard("scope", null, null, 500);
    }

}
