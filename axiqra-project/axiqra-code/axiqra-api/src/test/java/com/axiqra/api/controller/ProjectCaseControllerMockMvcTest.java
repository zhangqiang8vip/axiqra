package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.advice.TraceIdResponseAdvice;
import com.axiqra.api.filter.TraceIdFilter;
import com.axiqra.api.handler.GlobalExceptionHandler;
import com.axiqra.common.domain.vo.ProjectCaseDetailVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.ProjectCaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectCaseController MockMvc 闭环测试")
class ProjectCaseControllerMockMvcTest {

    @Mock
    private ProjectCaseService projectCaseService;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        ProjectCaseController controller = new ProjectCaseController(projectCaseService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new TraceIdFilter())
                .setControllerAdvice(new GlobalExceptionHandler(), new TraceIdResponseAdvice())
                .build();

        stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    @DisplayName("POST /project-cases 成功时应返回统一响应")
    void shouldCreateProjectCaseWithUnifiedResponse() throws Exception {
        ProjectCaseDetailVO detail = detail("private", 201L);
        when(projectCaseService.create(eq(USER_ID), org.mockito.ArgumentMatchers.any())).thenReturn(detail);

        mockMvc.perform(post("/project-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", "trace-project-case-create-001")
                        .content("""
                                {
                                  "traceId": 88,
                                  "licenseScope": "open_source",
                                  "redactionStatus": "complete"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.requestId").value("trace-project-case-create-001"))
                .andExpect(jsonPath("$.traceId").value("trace-project-case-create-001"))
                .andExpect(jsonPath("$.data.id").value(201L))
                .andExpect(jsonPath("$.data.status").value("private"))
                .andExpect(jsonPath("$.data.licenseScope").value("open_source"));
    }

    @Test
    @DisplayName("POST /project-cases 参数无效时应返回统一 400")
    void shouldReturnBadRequestWhenCreatePayloadInvalid() throws Exception {
        mockMvc.perform(post("/project-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", "trace-project-case-badreq-001")
                        .content("""
                                {
                                  "traceId": 0,
                                  "licenseScope": "",
                                  "redactionStatus": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_VALIDATION_FAILED.getCode()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.requestId").value("trace-project-case-badreq-001"));
    }

    @Test
    @DisplayName("GET /project-cases/{caseId} 业务异常时应走统一异常响应")
    void shouldReturnUnifiedResponseWhenDetailThrowsBizException() throws Exception {
        when(projectCaseService.getDetail(USER_ID, 201L))
                .thenThrow(new BizException(ErrorCode.PROJECT_CASE_NOT_FOUND));

        mockMvc.perform(get("/project-cases/{caseId}", 201L)
                        .header("X-Trace-Id", "trace-project-case-404-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PROJECT_CASE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.PROJECT_CASE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.requestId").value("trace-project-case-404-001"));
    }

    @Test
    @DisplayName("POST /project-cases/{caseId}/publish-request 成功时应返回统一响应")
    void shouldRequestPublishWithUnifiedResponse() throws Exception {
        ProjectCaseDetailVO detail = detail("pending_review", 201L);
        detail.setAuthorizationId(301L);
        when(projectCaseService.requestPublish(USER_ID, 201L, 301L)).thenReturn(detail);

        mockMvc.perform(post("/project-cases/{caseId}/publish-request", 201L)
                        .queryParam("authorizationId", "301")
                        .header("X-Trace-Id", "trace-project-case-publish-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.requestId").value("trace-project-case-publish-001"))
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.authorizationId").value(301L));
    }

    private ProjectCaseDetailVO detail(String status, Long id) {
        return ProjectCaseDetailVO.builder()
                .id(id)
                .traceId(88L)
                .workspaceId(100L)
                .projectId(10L)
                .authorId(USER_ID)
                .visibilityScope("workspace")
                .licenseScope("open_source")
                .redactionStatus("complete")
                .status(status)
                .build();
    }
}
