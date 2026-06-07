package com.axiqra.core.service;

import com.axiqra.common.domain.vo.NavResponseVO;

/**
 * 累加式导航服务接口
 *
 * <p>根据用户角色和空间成员关系，累加式返回可见菜单。
 * 菜单顺序固定：base_user_nav → space_membership_nav → granted_scope_nav
 * → governance_nav → admin_nav。
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
public interface NavService {

    /**
     * 获取当前用户的累加式导航菜单
     */
    NavResponseVO getNav(Long userId);
}
