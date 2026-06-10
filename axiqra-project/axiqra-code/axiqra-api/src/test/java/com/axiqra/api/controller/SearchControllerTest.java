package com.axiqra.api.controller;

import com.axiqra.common.domain.dto.SearchRequest;
import com.axiqra.common.domain.vo.SearchResponseVO;
import com.axiqra.common.domain.vo.SearchResultItemVO;
import com.axiqra.core.service.SearchService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchController 接口测试")
class SearchControllerTest {

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController controller;

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
    @DisplayName("合法 workspaceId 时应成功委托 searchBeforeAct")
    void searchBeforeActShouldDelegateWithValidWorkspaceId() {
        SearchRequest request = SearchRequest.builder()
                .query("spring boot")
                .workspaceId(100L)
                .limit(10)
                .build();
        SearchResponseVO response = SearchResponseVO.builder()
                .query("spring boot")
                .totalHits(1)
                .returnedHits(1)
                .empty(false)
                .candidateSeedCreated(false)
                .items(List.of(SearchResultItemVO.builder().solutionId(100L).title("Spring Boot Search").build()))
                .build();
        when(searchService.searchBeforeAct(1L, request)).thenReturn(response);

        var result = controller.searchBeforeAct(request);

        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotalHits());
        assertFalse(result.getData().isEmpty());
        verify(searchService).searchBeforeAct(1L, request);
    }
}
