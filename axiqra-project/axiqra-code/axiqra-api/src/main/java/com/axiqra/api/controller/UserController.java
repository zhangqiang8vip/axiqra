package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.entity.UserEntity;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器（仅用于邀请成员等场景）
 *
 * @author Axiqra Team
 * @date 2026-06-26
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户", description = "用户查询（仅供内部使用）")
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    @Operation(summary = "搜索用户", description = "按用户名搜索用户（用于邀请成员）")
    public ApiResponse<List<UserSearchVO>> searchUsers(@RequestParam String username) {
        if (username == null || username.trim().isEmpty()) {
            return ApiResponse.ok(List.of());
        }
        UserEntity user = userService.getByUsername(username.trim());
        if (user == null) {
            return ApiResponse.ok(List.of());
        }
        return ApiResponse.ok(List.of(new UserSearchVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar()
        )));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ApiResponse<UserSearchVO> getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        UserEntity user = userService.getById(userId);
        if (user == null) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.ok(new UserSearchVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar()
        ));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取指定用户公开信息")
    public ApiResponse<UserSearchVO> getUserById(@PathVariable Long userId) {
        UserEntity user = userService.getById(userId);
        if (user == null) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.ok(new UserSearchVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar()
        ));
    }

    public record UserSearchVO(
            Long id,
            String username,
            String nickname,
            String avatar
    ) {}
}
