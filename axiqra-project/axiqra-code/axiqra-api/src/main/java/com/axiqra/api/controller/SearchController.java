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
 * 搜索控制器
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "搜索", description = "权限预过滤 + 多路召回 + 空结果处理")
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/before-act")
    @Operation(summary = "搜索前权限预过滤", description = "执行搜索前先根据用户权限预过滤，返回可访问的召回结果")
    public ApiResponse<SearchResponseVO> searchBeforeAct(@Valid @RequestBody SearchRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        SearchResponseVO result = searchService.searchBeforeAct(userId, request);
        log.info("执行 Search before act: userId={}, query={}", userId, request.getQuery());
        return ApiResponse.ok(result);
    }

    @PostMapping("/public")
    @Operation(summary = "公开搜索", description = "无需登录，搜索所有公开可展示的 Solution")
    public ApiResponse<SearchResponseVO> searchPublic(@Valid @RequestBody SearchRequest request) {
        SearchResponseVO result = searchService.searchPublic(request);
        log.info("执行 Public Search: query={}, hits={}", request.getQuery(), result.getReturnedHits());
        return ApiResponse.ok(result);
    }
}
