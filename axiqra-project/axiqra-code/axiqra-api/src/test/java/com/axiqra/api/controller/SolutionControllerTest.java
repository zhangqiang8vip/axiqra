package com.axiqra.api.controller;

import com.axiqra.common.domain.vo.SolutionDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.domain.vo.SolutionVersionVO;
import com.axiqra.core.service.SolutionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolutionController 接口测试")
class SolutionControllerTest {

    @Mock
    private SolutionService solutionService;

    @InjectMocks
    private SolutionController controller;

    private MockedStatic<cn.dev33.satoken.stp.StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        stpUtilMock = mockStatic(cn.dev33.satoken.stp.StpUtil.class);
        stpUtilMock.when(cn.dev33.satoken.stp.StpUtil::getLoginIdAsLong).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    @DisplayName("应委托 service 返回详情")
    void getDetailShouldDelegateToService() {
        SolutionDetailVO detail = SolutionDetailVO.builder()
                .id(10L)
                .solutionCode("SOL-001")
                .title("Search Fusion")
                .status("verified")
                .activeVersion(SolutionVersionVO.builder().id(1L).versionNumber(3).active(true).build())
                .versions(List.of(SolutionVersionVO.builder().id(1L).versionNumber(3).active(true).build()))
                .feedbackStats(SolutionFeedbackStatsVO.builder().workedCount(2).totalCount(2).build())
                .build();
        when(solutionService.getDetail(1L, 10L)).thenReturn(detail);

        var result = controller.getDetail(10L);

        assertNotNull(result.getData());
        assertEquals("SOL-001", result.getData().getSolutionCode());
        verify(solutionService).getDetail(1L, 10L);
    }
}
