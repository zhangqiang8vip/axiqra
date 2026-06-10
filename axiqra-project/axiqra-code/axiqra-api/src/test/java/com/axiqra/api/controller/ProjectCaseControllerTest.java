package com.axiqra.api.controller;

import com.axiqra.common.domain.dto.ProjectCaseCreateRequest;
import com.axiqra.common.domain.vo.ProjectCaseDetailVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.ProjectCaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectCaseController 接口测试")
class ProjectCaseControllerTest {

    @Mock
    private ProjectCaseService projectCaseService;

    @InjectMocks
    private ProjectCaseController controller;

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
    @DisplayName("创建 Project Case 应委托 service")
    void createShouldDelegateToService() {
        ProjectCaseCreateRequest request = ProjectCaseCreateRequest.builder()
                .traceId(88L)
                .licenseScope("open_source")
                .redactionStatus("complete")
                .build();
        when(projectCaseService.create(1L, request)).thenReturn(detail("private"));

        var result = controller.create(request);

        assertNotNull(result.getData());
        assertEquals("private", result.getData().getStatus());
        verify(projectCaseService).create(1L, request);
    }

    @Test
    @DisplayName("详情读取应委托 service")
    void getDetailShouldDelegateToService() {
        when(projectCaseService.getDetail(1L, 101L)).thenReturn(detail("private"));

        var result = controller.getDetail(101L);

        assertEquals(101L, result.getData().getId());
        verify(projectCaseService).getDetail(1L, 101L);
    }

    @Test
    @DisplayName("发布申请时 service 异常应透传")
    void requestPublishShouldPropagateBizException() {
        BizException exception = new BizException(ErrorCode.CASE_REDACTION_INCOMPLETE);
        when(projectCaseService.requestPublish(1L, 101L, 201L)).thenThrow(exception);

        BizException thrown = assertThrows(BizException.class, () -> controller.requestPublish(101L, 201L));

        assertEquals(ErrorCode.CASE_REDACTION_INCOMPLETE.getCode(), thrown.getCode());
        verify(projectCaseService).requestPublish(1L, 101L, 201L);
    }

    private ProjectCaseDetailVO detail(String status) {
        return ProjectCaseDetailVO.builder()
                .id(101L)
                .traceId(88L)
                .workspaceId(100L)
                .authorId(1L)
                .licenseScope("open_source")
                .redactionStatus("complete")
                .status(status)
                .build();
    }
}
