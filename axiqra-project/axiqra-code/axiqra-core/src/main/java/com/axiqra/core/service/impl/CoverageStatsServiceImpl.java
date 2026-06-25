package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.CandidateSeedEntity;
import com.axiqra.common.domain.entity.SolutionEntity;
import com.axiqra.common.domain.enums.SolutionStatus;
import com.axiqra.common.domain.vo.CoverageStatsVO;
import com.axiqra.core.mapper.CandidateSeedMapper;
import com.axiqra.core.mapper.SolutionMapper;
import com.axiqra.core.service.CoverageStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 覆盖范围统计服务实现
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoverageStatsServiceImpl implements CoverageStatsService {

    private final SolutionMapper solutionMapper;
    private final CandidateSeedMapper candidateSeedMapper;

    @Override
    public CoverageStatsVO getOverallCoverage() {
        List<SolutionEntity> allSolutions = getAllSolutions();
        Set<String> coveredErrorTypes = new HashSet<>();
        Set<String> coveredTechStacks = new HashSet<>();
        long verifiedCount = 0;

        for (SolutionEntity solution : allSolutions) {
            if (solution.getErrorSignature() != null && !solution.getErrorSignature().isBlank()) {
                coveredErrorTypes.add(normalizeErrorType(solution.getErrorSignature()));
            }
            if (solution.getTechStack() != null && !solution.getTechStack().isBlank()) {
                coveredTechStacks.add(normalizeTechStack(solution.getTechStack()));
            }
            if (solution.getStatus() == SolutionStatus.VERIFIED 
                    || solution.getStatus() == SolutionStatus.STABLE 
                    || solution.getStatus() == SolutionStatus.CANONICAL) {
                verifiedCount++;
            }
        }

        // 估算总错误类型（基于已知常见错误）
        long estimatedTotalErrorTypes = 1000;
        long estimatedTotalTechStacks = 500;

        double errorCoverage = Math.min(100.0, (double) coveredErrorTypes.size() / estimatedTotalErrorTypes * 100);
        double techCoverage = Math.min(100.0, (double) coveredTechStacks.size() / estimatedTotalTechStacks * 100);

        return CoverageStatsVO.builder()
                .totalSolutions(allSolutions.size())
                .verifiedSolutions(verifiedCount)
                .totalErrorTypes(estimatedTotalErrorTypes)
                .coveredErrorTypes(coveredErrorTypes.size())
                .errorCoveragePercent(errorCoverage)
                .totalTechStacks(estimatedTotalTechStacks)
                .coveredTechStacks(coveredTechStacks.size())
                .techStackCoveragePercent(techCoverage)
                .candidateSeedCount(candidateSeedMapper.selectAll().size())
                .generatedAt(System.currentTimeMillis())
                .build();
    }

    @Override
    public Map<String, Long> getCoverageByTechStack() {
        List<SolutionEntity> solutions = getAllSolutions();
        return solutions.stream()
                .filter(s -> s.getTechStack() != null && !s.getTechStack().isBlank())
                .collect(Collectors.groupingBy(
                        s -> normalizeTechStack(s.getTechStack()),
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Long> getCoverageByErrorType() {
        List<SolutionEntity> solutions = getAllSolutions();
        return solutions.stream()
                .filter(s -> s.getErrorSignature() != null && !s.getErrorSignature().isBlank())
                .collect(Collectors.groupingBy(
                        s -> normalizeErrorType(s.getErrorSignature()),
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Long> getCoverageByFramework() {
        List<SolutionEntity> solutions = getAllSolutions();
        Map<String, Long> frameworkCount = new HashMap<>();

        for (SolutionEntity solution : solutions) {
            if (solution.getTechStack() != null) {
                // 从 tech_stack 字段提取框架信息
                String[] parts = solution.getTechStack().split("[,\\-_]");
                for (String part : parts) {
                    String framework = part.trim().toLowerCase();
                    if (isKnownFramework(framework)) {
                        frameworkCount.merge(framework, 1L, Long::sum);
                    }
                }
            }
        }

        return frameworkCount;
    }

    @Override
    public List<CoverageStatsVO.CoverageGap> getCoverageGaps() {
        List<CoverageStatsVO.CoverageGap> gaps = new ArrayList<>();
        
        // 获取所有候选缺口
        List<CandidateSeedEntity> seeds = candidateSeedMapper.selectAll();
        Map<String, Long> errorTypeCounts = getCoverageByErrorType();
        Map<String, Long> techStackCounts = getCoverageByTechStack();

        for (CandidateSeedEntity seed : seeds) {
            if (seed.getCoverageGap() != null && !seed.getCoverageGap().isBlank()) {
                gaps.add(CoverageStatsVO.CoverageGap.builder()
                        .gapType("error_type")
                        .gapName(seed.getCoverageGap())
                        .description("需要覆盖的错误类型: " + seed.getCoverageGap())
                        .relatedSolutionCount(errorTypeCounts.getOrDefault(seed.getCoverageGap(), 0L))
                        .priority(determinePriority(seed))
                        .build());
            }
        }

        return gaps;
    }

    @Override
    public List<CoverageStatsVO.TechStackCoverage> getTechStackRanking() {
        Map<String, Long> coverage = getCoverageByTechStack();
        long total = coverage.values().stream().mapToLong(Long::longValue).sum();

        List<CoverageStatsVO.TechStackCoverage> rankings = coverage.entrySet().stream()
                .map(entry -> CoverageStatsVO.TechStackCoverage.builder()
                        .techStack(entry.getKey())
                        .solutionCount(entry.getValue())
                        .coveragePercent(total > 0 ? (double) entry.getValue() / total * 100 : 0)
                        .build())
                .sorted((a, b) -> Long.compare(b.getSolutionCount(), a.getSolutionCount()))
                .collect(Collectors.toList());

        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }

        return rankings;
    }

    @Override
    public long getCandidateSeedCount() {
        return candidateSeedMapper.selectAll().size();
    }

    private List<SolutionEntity> getAllSolutions() {
        return solutionMapper.selectAll().stream()
                .filter(s -> !s.isDeleted())
                .filter(s -> s.getStatus() != null && s.getStatus() != SolutionStatus.DEPRECATED)
                .toList();
    }

    private String normalizeTechStack(String techStack) {
        if (techStack == null) return "unknown";
        return techStack.trim().toLowerCase().split("[,\\-_]")[0];
    }

    private String normalizeErrorType(String errorSignature) {
        if (errorSignature == null) return "unknown";
        // 提取错误类型（去除版本号等具体信息）
        String normalized = errorSignature.toLowerCase();
        if (normalized.contains("connection")) return "connection_error";
        if (normalized.contains("timeout")) return "timeout_error";
        if (normalized.contains("auth") || normalized.contains("permission")) return "auth_error";
        if (normalized.contains("validation")) return "validation_error";
        if (normalized.contains("null")) return "null_pointer_error";
        if (normalized.contains("memory")) return "memory_error";
        if (normalized.contains("io") || normalized.contains("file")) return "io_error";
        return "other_error";
    }

    private boolean isKnownFramework(String framework) {
        Set<String> knownFrameworks = Set.of(
                "spring", "springboot", "django", "flask", "rails", "express",
                "react", "vue", "angular", "nextjs", "nuxt",
                "fastapi", "fastify", "nestjs", "laravel",
                "kubernetes", "docker", "terraform"
        );
        return knownFrameworks.contains(framework.toLowerCase());
    }

    private String determinePriority(CandidateSeedEntity seed) {
        if (seed.getCoverageGap() == null) return "low";
        String gap = seed.getCoverageGap().toLowerCase();
        if (gap.contains("critical") || gap.contains("production") || gap.contains("security")) {
            return "high";
        }
        if (gap.contains("common") || gap.contains("frequent")) {
            return "medium";
        }
        return "low";
    }
}
