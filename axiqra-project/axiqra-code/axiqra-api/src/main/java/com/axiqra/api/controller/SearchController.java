package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.dto.SearchRequest;
import com.axiqra.common.domain.vo.SearchResponseVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Search 控制器
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "权限预过滤 + 多路召回 + 空结果处理")
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/before-act")
    @Operation(summary = "搜索前执行权限预过滤与召回")
    public ApiResponse<SearchResponseVO> searchBeforeAct(@Valid @RequestBody SearchRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        SearchResponseVO result = searchService.searchBeforeAct(userId, request);
        log.info("执行 Search before act: userId={}, query={}", userId, request.getQuery());
        return ApiResponse.ok(result);
    }

    @PostMapping("/public")
    @Operation(summary = "匿名搜索公开可展示的 Solution")
    public ApiResponse<SearchResponseVO> searchPublic(@Valid @RequestBody SearchRequest request) {
        SearchResponseVO result = searchService.searchPublic(request);
        log.info("执行 Public Search: query={}, hits={}", request.getQuery(), result.getReturnedHits());
        return ApiResponse.ok(result);
    }
}
