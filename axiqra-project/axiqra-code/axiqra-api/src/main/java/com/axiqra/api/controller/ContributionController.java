package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.common.domain.vo.ContributionRankingVO;
import com.axiqra.common.domain.vo.UserContributionVO;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.service.ContributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contribution 控制器
 *
 * @author Axiqra Team
 */
@Slf4j
@RestController
@RequestMapping("/v1/contributions")
@RequiredArgsConstructor
@Tag(name = "Contribution", description = "贡献积分查询")
public class ContributionController {

    private final ContributionService contributionService;

    @GetMapping("/me")
    @Operation(summary = "获取我的贡献统计")
    public ApiResponse<UserContributionVO> getMyContribution(
            @RequestParam(required = false) Long workspaceId) {
        long userId = StpUtil.getLoginIdAsLong();
        UserContributionVO result = contributionService.getUserContribution(userId, workspaceId);
        log.info("查询贡献统计: userId={}, workspaceId={}", userId, workspaceId);
        return ApiResponse.ok(result);
    }

    @GetMapping("/rankings")
    @Operation(summary = "获取贡献排行榜")
    public ApiResponse<List<ContributionRankingVO>> getRankings(
            @RequestParam(required = false) Long workspaceId,
            @RequestParam(defaultValue = "20") int limit) {
        List<ContributionRankingVO> result = contributionService.getContributionRankings(workspaceId, limit);
        log.info("查询贡献排行榜: workspaceId={}, limit={}", workspaceId, limit);
        return ApiResponse.ok(result);
    }
}
