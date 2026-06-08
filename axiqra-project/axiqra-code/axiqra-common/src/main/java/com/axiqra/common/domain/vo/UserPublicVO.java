package com.axiqra.common.domain.vo;

import com.axiqra.common.domain.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公开用户信息 VO（用于 /users/{userId} 等公开查询接口，不包含隐私信息）。
 *
 * @author Axiqra Team
 * @date 2026-06-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPublicVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;

    /**
     * 从实体构建公开 VO（不含 email 等隐私字段）。
     */
    public static UserPublicVO from(UserEntity user) {
        if (user == null) {
            return null;
        }
        return UserPublicVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatar())
                .build();
    }
}
