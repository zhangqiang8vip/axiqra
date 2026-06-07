package com.axiqra.common.domain.vo;

import com.axiqra.common.domain.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息 VO（/me 接口返回，去掉敏感字段）
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoVO {

    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;

    public static UserInfoVO from(UserEntity user) {
        if (user == null) {
            return null;
        }
        return UserInfoVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build();
    }
}
