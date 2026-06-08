package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 登录响应 VO
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Data
@NoArgsConstructor
@ToString(exclude = "email")
@AllArgsConstructor
@Builder
public class LoginResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String token;
}
