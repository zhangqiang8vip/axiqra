package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.advice.TraceIdResponseAdvice;
import com.axiqra.api.filter.TraceIdFilter;
import com.axiqra.api.handler.GlobalExceptionHandler;
import com.axiqra.common.domain.enums.WorkspaceType;
import com.axiqra.common.domain.vo.WorkspaceVO;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.core.service.WorkspaceService;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceController MockMvc 闭环测试")
class WorkspaceControllerMockMvcTest {

    @Mock
    private WorkspaceService workspaceService;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        WorkspaceController controller = new WorkspaceController(workspaceService);
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
    @DisplayName("POST /workspaces 成功时应返回统一响应和 requestId")
    void shouldCreateWorkspaceWithUnifiedResponse() throws Exception {
        WorkspaceVO workspace = WorkspaceVO.builder()
                .id(100L)
                .workspaceName("My Space")
                .workspaceType(WorkspaceType.PERSONAL)
                .ownerId(USER_ID)
                .memberCount(1L)
                .myRole("owner")
                .gmtCreate(Instant.parse("2026-06-09T12:00:00Z"))
                .build();
        when(workspaceService.create(eq(USER_ID), eq("personal"), eq("My Space"))).thenReturn(workspace);

        mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", "trace-workspace-create-001")
                        .content("""
                                {
                                  "workspaceType": "personal",
                                  "workspaceName": "My Space"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost:80/workspaces/100"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.requestId").value("trace-workspace-create-001"))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.workspaceName").value("My Space"))
                .andExpect(jsonPath("$.data.workspaceType").value("PERSONAL"));
    }

    @Test
    @DisplayName("POST /workspaces 参数无效时应返回统一 400 响应")
    void shouldReturnUnifiedBadRequestWhenCreatePayloadInvalid() throws Exception {
        mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", "trace-workspace-badreq-001")
                        .content("""
                                {
                                  "workspaceType": "",
                                  "workspaceName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_VALIDATION_FAILED.getCode()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.requestId").value("trace-workspace-badreq-001"));
    }
}
