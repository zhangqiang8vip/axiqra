package com.axiqra.common.domain.vo;

import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.domain.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 空间成员信息 VO
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberVO {

    private Long memberId;
    private Long userId;
    private Long workspaceId;
    private MemberRole role;
    private MemberStatus status;
    private String username;
    private String nickname;
    private String email;
    private Instant joinedAt;

    public static MemberVO from(MembershipEntity membership) {
        if (membership == null) {
            return null;
        }
        MemberVO vo = MemberVO.builder()
                .memberId(membership.getId())
                .userId(membership.getUserId())
                .workspaceId(membership.getWorkspaceId())
                .role(MemberRole.of(membership.getRole()))
                .status(MemberStatus.of(membership.getStatus()))
                .joinedAt(membership.getGmtCreate())
                .build();
        return vo;
    }

    public MemberVO withUsername(String username) {
        this.username = username;
        return this;
    }

    public MemberVO withNickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public MemberVO withEmail(String email) {
        this.email = email;
        return this;
    }
}
