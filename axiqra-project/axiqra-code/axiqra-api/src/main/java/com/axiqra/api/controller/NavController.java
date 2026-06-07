package com.axiqra.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.vo.NavResponseVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.NavService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 导航控制器
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "导航", description = "累加式导航菜单")
public class NavController {

    private final NavService navService;

    @SaCheckLogin
    @GetMapping("/nav")
    @Operation(summary = "获取导航菜单", description = "累加式返回当前用户可见的全部菜单")
    public ApiResponse<NavResponseVO> getNav() {
        long userId = StpUtil.getLoginIdAsLong();
        NavResponseVO nav = navService.getNav(userId);
        return ApiResponse.ok(nav);
    }
}
