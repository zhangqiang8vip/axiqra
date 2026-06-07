package com.axiqra.common.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Email;

/**
 * 更新用户资料请求 DTO
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Getter
@Setter
@NoArgsConstructor
public class ProfileUpdateRequest {

    @Size(max = 100, message = "nickname 最多 100 字符")
    private String nickname;

    @Email(message = "email 格式不正确")
    @Size(max = 255, message = "email 最多 255 字符")
    private String email;
}
