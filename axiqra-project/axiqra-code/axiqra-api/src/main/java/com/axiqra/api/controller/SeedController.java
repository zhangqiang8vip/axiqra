package com.axiqra.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.axiqra.api.annotation.RequireScope;
import com.axiqra.common.domain.dto.SearchRequest;
import com.axiqra.common.domain.entity.CandidateSeedEntity;
import com.axiqra.common.domain.entity.MembershipEntity;
import com.axiqra.common.domain.enums.MemberStatus;
import com.axiqra.common.domain.vo.CandidateSeedVO;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.response.ApiResponse;
import com.axiqra.core.mapper.CandidateSeedMapper;
import com.axiqra.core.service.CandidateSeedService;
import com.axiqra.core.service.RbacService;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * Candidate Seed Controller
 *
 * @author Axiqra Team
 * @date 2026-06-25
 */
@Tag(name = "Seed", description = "候选种子管理")
@RestController
@RequestMapping("/seeds")
@RequiredArgsConstructor
public class SeedController {

    private final CandidateSeedService candidateSeedService;
    private final CandidateSeedMapper candidateSeedMapper;
    private final RbacService rbacService;

    @PostMapping
    @Operation(summary = "创建候选 Seed", description = "搜索无结果时创建候选 Seed")
    @RequireScope("seed:write")
    public ApiResponse<CandidateSeedVO> createSeed(@Valid @RequestBody CreateSeedRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        Long workspaceId = request.workspaceId() != null ? request.workspaceId() : resolveDefaultWorkspaceId(userId);
        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.normalizedQuery())
                .workspaceId(workspaceId)
                .techStack(request.techStack())
                .domain(request.domain())
                .build();
        CandidateSeedService.CandidateSeedCreationResult result =
                candidateSeedService.createOrReuseCandidateSeed(userId, searchRequest);
        return ApiResponse.ok(toVO(result.seed()));
    }

    @GetMapping
    @Operation(summary = "获取候选 Seed 列表")
    @RequireScope("seed:read")
    public ApiResponse<List<CandidateSeedVO>> listSeeds(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long workspaceId) {
        long userId = StpUtil.getLoginIdAsLong();
        List<CandidateSeedEntity> seeds;
        if (workspaceId != null) {
            seeds = candidateSeedMapper.selectByWorkspaceId(workspaceId);
        } else {
            seeds = candidateSeedMapper.selectByAuthorId(userId);
        }
        return ApiResponse.ok(seeds.stream().map(this::toVO).toList());
    }

    @GetMapping("/{seedId}")
    @Operation(summary = "获取候选 Seed 详情")
    @RequireScope("seed:read")
    public ApiResponse<CandidateSeedVO> getSeed(@PathVariable Long seedId) {
        CandidateSeedEntity seed = candidateSeedMapper.selectById(seedId);
        if (seed == null || seed.isDeleted()) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.ok(toVO(seed));
    }

    private CandidateSeedVO toVO(CandidateSeedEntity entity) {
        if (entity == null) return null;
        return CandidateSeedVO.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspaceId())
                .authorId(entity.getAuthorId())
                .queryHash(entity.getQueryHash())
                .taskGoal(entity.getTaskGoal())
                .techStack(entity.getTechStack())
                .coverageGap(entity.getCoverageGap())
                .status(entity.getStatus())
                .assigneeId(entity.getAssigneeId())
                .solutionId(entity.getSolutionId())
                .build();
    }

    public record CreateSeedRequest(
            @JsonAlias({"taskGoal", "task_goal", "goal"})
            String query,
            @JsonAlias("workspace_id")
            Long workspaceId,
            @JsonAlias("tech_stack")
            String techStack,
            String domain
    ) {
        public String normalizedQuery() {
            return query;
        }
    }

    private Long resolveDefaultWorkspaceId(long userId) {
        List<MembershipEntity> memberships = rbacService.getMemberships(userId);
        return (memberships == null ? Collections.<MembershipEntity>emptyList() : memberships).stream()
                .filter(membership -> membership != null && !membership.isDeleted())
                .filter(membership -> MemberStatus.ACTIVE.getCode().equals(membership.getStatus()))
                .map(MembershipEntity::getWorkspaceId)
                .filter(id -> id != null && id > 0)
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.PARAM_INVALID, "workspaceId 不能为空，请先创建或加入 Workspace"));
    }
}
