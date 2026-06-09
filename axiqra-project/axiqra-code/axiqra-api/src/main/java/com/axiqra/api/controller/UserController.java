package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.domain.vo.UserInfoVO;
import com.axiqra.common.domain.vo.UserPublicVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户信息控制器
 *
 * <p>提供用户个人信息和权限查询接口（登录后访问）。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户", description = "用户信息、权限查询")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "返回当前登录用户的完整信息")
    public ApiResponse<UserInfoVO> getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        UserEntity user = userService.getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return ApiResponse.ok(UserInfoVO.from(user));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取指定用户信息", description = "根据 ID 获取用户公开信息（不含 email 等隐私字段）")
    public ApiResponse<UserPublicVO> getUserById(@PathVariable Long userId) {
        UserEntity user = userService.getById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return ApiResponse.ok(UserPublicVO.from(user));
    }
}
