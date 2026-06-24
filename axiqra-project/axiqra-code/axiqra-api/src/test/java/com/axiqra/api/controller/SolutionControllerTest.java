package com.axiqra.api.controller;

import com.axiqra.api.handler.GlobalExceptionHandler;
import com.axiqra.common.domain.dto.SolutionCreateFromProjectCaseRequest;
import com.axiqra.common.domain.vo.SearchResultItemVO;
import com.axiqra.common.domain.vo.SolutionDetailVO;
import com.axiqra.common.domain.vo.SolutionFeedbackStatsVO;
import com.axiqra.common.domain.vo.SolutionVersionVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
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
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    @DisplayName("service 抛出不存在异常时应透传")
    void getDetailShouldPropagateNotFoundBizException() {
        BizException exception = new BizException(ErrorCode.SOLUTION_NOT_FOUND);
        when(solutionService.getDetail(1L, 10L)).thenThrow(exception);

        BizException thrown = assertThrows(BizException.class, () -> controller.getDetail(10L));

        assertEquals(ErrorCode.SOLUTION_NOT_FOUND.getCode(), thrown.getCode());
        verify(solutionService).getDetail(1L, 10L);
    }

    @Test
    @DisplayName("FORBIDDEN 异常应由全局处理器映射")
    void forbiddenBizExceptionShouldBeMappedByGlobalExceptionHandler() {
        BizException exception = new BizException(ErrorCode.FORBIDDEN, "无权访问该 Solution");
        when(solutionService.getDetail(1L, 10L)).thenThrow(exception);

        BizException thrown = assertThrows(BizException.class, () -> controller.getDetail(10L));
        var response = new GlobalExceptionHandler().handleBizException(thrown);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.FORBIDDEN.getCode(), response.getBody().getCode());
        assertEquals("无权访问该 Solution", response.getBody().getMessage());
    }

    @Test
    @DisplayName("从 Project Case 生成 Solution 应委托 service")
    void createFromProjectCaseShouldDelegateToService() {
        SolutionCreateFromProjectCaseRequest request = SolutionCreateFromProjectCaseRequest.builder()
                .projectCaseId(77L)
                .title("AI Agent 接入失败排查方案")
                .visibilityScope("workspace")
                .build();
        SolutionDetailVO detail = SolutionDetailVO.builder()
                .id(10L)
                .solutionCode("SOL-PC-77")
                .title("AI Agent 接入失败排查方案")
                .visibilityScope("workspace")
                .build();
        when(solutionService.createFromProjectCase(1L, request)).thenReturn(detail);

        var result = controller.createFromProjectCase(request);

        assertNotNull(result.getData());
        assertEquals("SOL-PC-77", result.getData().getSolutionCode());
        verify(solutionService).createFromProjectCase(1L, request);
    }

    @Test
    @DisplayName("公开 Solution 详情应委托 public service")
    void getPublicDetailShouldDelegateToService() {
        SolutionDetailVO detail = SolutionDetailVO.builder()
                .id(10L)
                .solutionCode("SOL-001")
                .title("Search Fusion")
                .status("verified")
                .visibilityScope("public")
                .build();
        when(solutionService.getPublicDetail(10L)).thenReturn(detail);

        var result = controller.getPublicDetail(10L);

        assertNotNull(result.getData());
        assertEquals("public", result.getData().getVisibilityScope());
        verify(solutionService).getPublicDetail(10L);
    }

    @Test
    @DisplayName("公开 Solution 列表应委托 public service")
    void listPublicSolutionsShouldDelegateToService() {
        when(solutionService.listPublicSolutions("spring", "backend", "boot", 1, 5))
                .thenReturn(List.of(SearchResultItemVO.builder()
                        .solutionId(10L)
                        .solutionCode("SOL-001")
                        .title("Search Fusion")
                        .visibilityScope("public")
                        .build()));

        var result = controller.listPublicSolutions("spring", "backend", "boot", 1, 5);

        assertEquals(1, result.getData().size());
        assertEquals("SOL-001", result.getData().get(0).getSolutionCode());
        verify(solutionService).listPublicSolutions("spring", "backend", "boot", 1, 5);
    }
}
