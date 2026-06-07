package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberRole;
import com.axiqra.common.domain.enums.MemberStatus;
import com.axiqra.common.domain.vo.NavItemVO;
import com.axiqra.common.domain.vo.NavResponseVO;
import com.axiqra.core.service.NavService;
import com.axiqra.core.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 累加式导航服务实现
 *
 * @author Axiqra Team
 * @date 2026-06-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NavServiceImpl implements NavService {

    private final RbacService rbacService;

    @Override
    public NavResponseVO getNav(Long userId) {
        if (userId == null) {
            return emptyNav();
        }

        log.info("获取导航菜单: userId={}", userId);

        List<NavItemVO> baseUserNav = buildBaseUserNav();
        List<NavItemVO> spaceMembershipNav = buildSpaceMembershipNav(userId);
        List<NavItemVO> grantedScopeNav = buildGrantedScopeNav(userId);
        List<NavItemVO> governanceNav = buildGovernanceNav();
        List<NavItemVO> adminNav = buildAdminNav();

        return NavResponseVO.builder()
                .baseUserNav(baseUserNav)
                .spaceMembershipNav(spaceMembershipNav)
                .grantedScopeNav(grantedScopeNav)
                .governanceNav(governanceNav)
                .adminNav(adminNav)
                .build();
    }

    private List<NavItemVO> buildBaseUserNav() {
        List<NavItemVO> items = new ArrayList<>();
        items.add(NavItemVO.builder()
                .id("home")
                .label("首页")
                .icon("home")
                .path("/home")
                .category("base")
                .build());
        items.add(NavItemVO.builder()
                .id("profile")
                .label("个人资料")
                .icon("user")
                .path("/profile")
                .category("base")
                .build());
        items.add(NavItemVO.builder()
                .id("workspaces")
                .label("我的空间")
                .icon("folder")
                .path("/workspaces")
                .category("base")
                .build());
        return items;
    }

    private List<NavItemVO> buildSpaceMembershipNav(Long userId) {
        List<NavItemVO> items = new ArrayList<>();

        List<MembershipEntity> memberships = rbacService.getMemberships(userId);
        if (memberships == null || memberships.isEmpty()) {
            return items;
        }

        for (MembershipEntity m : memberships) {
            if (m == null) {
                continue;
            }
            if (!MemberStatus.ACTIVE.getCode().equals(m.getStatus())) {
                continue;
            }
            Long workspaceId = m.getWorkspaceId();
            String role = m.getRole();

            if (workspaceId == null) {
                continue;
            }

            items.add(NavItemVO.builder()
                    .id("workspace-" + workspaceId)
                    .label("空间 " + workspaceId)
                    .icon("folder-open")
                    .path("/workspaces/" + workspaceId)
                    .category("space")
                    .build());

            if (MemberRole.ADMIN.getCode().equals(role)
                    || MemberRole.OWNER.getCode().equals(role)) {
                items.add(NavItemVO.builder()
                        .id("workspace-" + workspaceId + "-members")
                        .label("成员管理")
                        .icon("team")
                        .path("/workspaces/" + workspaceId + "/members")
                        .category("space-admin")
                        .build());
            }
        }
        return items;
    }

    private List<NavItemVO> buildGrantedScopeNav(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<NavItemVO> items = new ArrayList<>();
        if (rbacService.hasScope(userId, "search")) {
            items.add(NavItemVO.builder()
                    .id("search")
                    .label("搜索")
                    .icon("search")
                    .path("/search")
                    .category("scope")
                    .build());
        }
        if (rbacService.hasScope(userId, "solution")) {
            items.add(NavItemVO.builder()
                    .id("solutions")
                    .label("解决方案")
                    .icon("solution")
                    .path("/solutions")
                    .category("scope")
                    .build());
        }
        if (rbacService.hasScope(userId, "trace")) {
            items.add(NavItemVO.builder()
                    .id("traces")
                    .label("工程轨迹")
                    .icon("branches")
                    .path("/traces")
                    .category("scope")
                    .build());
        }
        return items;
    }

    private List<NavItemVO> buildGovernanceNav() {
        return List.of();
    }

    private List<NavItemVO> buildAdminNav() {
        return List.of();
    }

    private NavResponseVO emptyNav() {
        return NavResponseVO.builder()
                .baseUserNav(List.of())
                .spaceMembershipNav(List.of())
                .grantedScopeNav(List.of())
                .governanceNav(List.of())
                .adminNav(List.of())
                .build();
    }
}
