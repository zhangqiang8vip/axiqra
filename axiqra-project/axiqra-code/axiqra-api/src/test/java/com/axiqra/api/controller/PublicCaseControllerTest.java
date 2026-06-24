package com.axiqra.api.controller;

import com.axiqra.common.domain.vo.PublicCaseDetailVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.PublicCaseService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicCaseController 接口测试")
class PublicCaseControllerTest {

    @Mock
    private PublicCaseService publicCaseService;

    @InjectMocks
    private PublicCaseController controller;

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
    @DisplayName("发布 Public Case 应委托 service")
    void publishShouldDelegateToService() {
        when(publicCaseService.publish(1L, 101L)).thenReturn(detail(301L));

        var result = controller.publish(101L);

        assertNotNull(result.getData());
        assertEquals(301L, result.getData().getId());
        verify(publicCaseService).publish(1L, 101L);
    }

    @Test
    @DisplayName("读取 Public Case 详情应委托 service")
    void getDetailShouldDelegateToService() {
        when(publicCaseService.getPublicDetail(301L)).thenReturn(detail(301L));

        var result = controller.getDetail(301L);

        assertEquals("verified", result.getData().getStatus());
        verify(publicCaseService).getPublicDetail(301L);
    }

    @Test
    @DisplayName("公开列表应委托 service")
    void listShouldDelegateToService() {
        when(publicCaseService.listPublicCases(5)).thenReturn(List.of(detail(301L)));

        var result = controller.list(5);

        assertEquals(1, result.getData().size());
        verify(publicCaseService).listPublicCases(5);
    }

    @Test
    @DisplayName("service 异常应透传")
    void publishShouldPropagateBizException() {
        BizException exception = new BizException(ErrorCode.CASE_DUPLICATE_SOURCE);
        when(publicCaseService.publish(1L, 101L)).thenThrow(exception);

        BizException thrown = assertThrows(BizException.class, () -> controller.publish(101L));

        assertEquals(ErrorCode.CASE_DUPLICATE_SOURCE.getCode(), thrown.getCode());
    }

    private PublicCaseDetailVO detail(Long id) {
        return PublicCaseDetailVO.builder()
                .id(id)
                .sourceCaseId(101L)
                .workspaceId(100L)
                .authorId(1L)
                .redactionStatus("complete")
                .status("verified")
                .build();
    }
}
