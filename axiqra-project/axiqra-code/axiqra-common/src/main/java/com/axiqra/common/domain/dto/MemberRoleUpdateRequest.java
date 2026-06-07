package com.axiqra.common.domain.dto;

import com.axiqra.common.domain.enums.MemberRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 更新成员角色请求 DTO
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Getter
@Setter
@NoArgsConstructor
public class MemberRoleUpdateRequest {

    @NotNull(message = "memberId 不能为空")
    @Positive(message = "memberId 必须为正数")
    private Long memberId;

    @NotNull(message = "role 不能为空")
    private MemberRole role;
}
