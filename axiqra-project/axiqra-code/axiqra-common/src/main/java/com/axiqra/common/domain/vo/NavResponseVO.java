package com.axiqra.common.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 累加式导航响应 VO
 *
 * <p>累加式返回当前用户可见的全部菜单：
 * <ol>
 *   <li>base_user_nav   - 所有登录用户的基础菜单</li>
 *   <li>space_membership_nav - 基于各空间成员身份的菜单（viewer 无管理入口）</li>
 *   <li>granted_scope_nav  - 基于已授权 scope 的菜单（S1 预留）</li>
 *   <li>governance_nav  - 治理相关菜单（S1 预留）</li>
 *   <li>admin_nav      - 管理员/平台管理员专属菜单（S1 预留）</li>
 * </ol>
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavResponseVO {

    private List<NavItemVO> baseUserNav;

    private List<NavItemVO> spaceMembershipNav;

    private List<NavItemVO> grantedScopeNav;

    private List<NavItemVO> governanceNav;

    private List<NavItemVO> adminNav;
}
