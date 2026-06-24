package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.advice.TraceIdResponseAdvice;
import com.axiqra.api.filter.TraceIdFilter;
import com.axiqra.api.handler.GlobalExceptionHandler;
import com.axiqra.common.domain.vo.PublicCaseDetailVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.PublicCaseService;
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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicCaseController MockMvc 闭环测试")
class PublicCaseControllerMockMvcTest {

    @Mock
    private PublicCaseService publicCaseService;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        PublicCaseController controller = new PublicCaseController(publicCaseService);
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
    @DisplayName("POST /public-cases/publish/{projectCaseId} 成功时应返回统一响应")
    void shouldPublishPublicCaseWithUnifiedResponse() throws Exception {
        when(publicCaseService.publish(USER_ID, 101L)).thenReturn(detail(301L, "verified"));

        mockMvc.perform(post("/public-cases/publish/{projectCaseId}", 101L)
                        .header("X-Trace-Id", "trace-public-case-publish-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.requestId").value("trace-public-case-publish-001"))
                .andExpect(jsonPath("$.traceId").value("trace-public-case-publish-001"))
                .andExpect(jsonPath("$.data.id").value(301L))
                .andExpect(jsonPath("$.data.status").value("verified"));
    }

    @Test
    @DisplayName("GET /public-cases/{publicCaseId} 不存在时应走统一异常响应")
    void shouldReturnUnifiedNotFoundForDetail() throws Exception {
        when(publicCaseService.getPublicDetail(301L))
                .thenThrow(new BizException(ErrorCode.PUBLIC_CASE_NOT_FOUND));

        mockMvc.perform(get("/public-cases/{publicCaseId}", 301L)
                        .header("X-Trace-Id", "trace-public-case-404-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PUBLIC_CASE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.PUBLIC_CASE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.requestId").value("trace-public-case-404-001"));
    }

    @Test
    @DisplayName("GET /public-cases 成功时应返回统一列表响应")
    void shouldListPublicCasesWithUnifiedResponse() throws Exception {
        when(publicCaseService.listPublicCases(5)).thenReturn(List.of(detail(301L, "verified")));

        mockMvc.perform(get("/public-cases")
                        .queryParam("limit", "5")
                        .header("X-Trace-Id", "trace-public-case-list-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.requestId").value("trace-public-case-list-001"))
                .andExpect(jsonPath("$.data[0].id").value(301L))
                .andExpect(jsonPath("$.data[0].status").value("verified"));
    }

    private PublicCaseDetailVO detail(Long id, String status) {
        return PublicCaseDetailVO.builder()
                .id(id)
                .sourceCaseId(101L)
                .workspaceId(100L)
                .authorId(USER_ID)
                .redactionStatus("complete")
                .status(status)
                .build();
    }
}
