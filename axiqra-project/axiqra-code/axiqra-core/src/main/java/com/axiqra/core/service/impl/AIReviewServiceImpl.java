package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.PublicCaseEntity;
import com.axiqra.common.domain.entity.SolutionEntity;
import com.axiqra.core.mapper.PublicCaseMapper;
import com.axiqra.core.mapper.SolutionMapper;
import com.axiqra.core.service.AIReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * AI 审核服务实现
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIReviewServiceImpl implements AIReviewService {

    private final SolutionMapper solutionMapper;
    private final PublicCaseMapper publicCaseMapper;

    // 敏感信息检测模式
    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
            Pattern.compile("password\\s*[:=]\\s*\\S+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("api[_-]?key\\s*[:=]\\s*\\S+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("secret\\s*[:=]\\s*\\S+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("token\\s*[:=]\\s*\\S+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("bearer\\s+\\S+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\d{15,16}[\\s/]\\d{2}[\\s/]\\d{2,4}[\\s/]\\d{3,4}"), // 信用卡号
            Pattern.compile("\\[REDACTED\\]|\\[REMOVED\\]|\\*+\\d+\\*+"), // 已脱敏标记
            Pattern.compile("private[_-]?key", Pattern.CASE_INSENSITIVE),
            Pattern.compile("aws[_-]?access[_-]?key", Pattern.CASE_INSENSITIVE)
    );

    @Override
    public AIReviewResult checkStructureIntegrity(Long contentId, String contentType) {
        try {
            switch (contentType.toLowerCase()) {
                case "solution" -> {
                    SolutionEntity solution = solutionMapper.selectActiveById(contentId);
                    if (solution == null) {
                        return new AIReviewResult("structure", false, "NOT_FOUND", "Content not found", 1.0);
                    }
                    return checkSolutionStructure(solution);
                }
                case "public_case" -> {
                    PublicCaseEntity entity = publicCaseMapper.selectActiveById(contentId);
                    if (entity == null) {
                        return new AIReviewResult("structure", false, "NOT_FOUND", "Content not found", 1.0);
                    }
                    return checkPublicCaseStructure(entity);
                }
                default -> {
                    return new AIReviewResult("structure", false, "UNKNOWN_TYPE", "Unknown content type", 1.0);
                }
            }
        } catch (Exception e) {
            log.error("Structure check failed: {}", e.getMessage(), e);
            return new AIReviewResult("structure", false, "ERROR", e.getMessage(), 0.5);
        }
    }

    @Override
    public AIReviewResult checkSensitiveInfo(Long contentId, String contentType) {
        try {
            String content = getContentText(contentId, contentType);
            if (content == null) {
                return new AIReviewResult("sensitive_info", false, "NOT_FOUND", "Content not found", 1.0);
            }

            List<String> detectedPatterns = new ArrayList<>();
            for (Pattern pattern : SENSITIVE_PATTERNS) {
                if (pattern.matcher(content).find()) {
                    detectedPatterns.add(pattern.pattern());
                }
            }

            if (detectedPatterns.isEmpty()) {
                return new AIReviewResult("sensitive_info", true, "PASS", "No sensitive information detected", 0.95);
            } else {
                return new AIReviewResult("sensitive_info", false, "FAIL", 
                        "Sensitive patterns detected: " + String.join(", ", detectedPatterns), 0.9);
            }
        } catch (Exception e) {
            log.error("Sensitive info check failed: {}", e.getMessage(), e);
            return new AIReviewResult("sensitive_info", false, "ERROR", e.getMessage(), 0.5);
        }
    }

    @Override
    public AIReviewResult checkDuplicate(Long contentId, String contentType) {
        try {
            if (!"solution".equalsIgnoreCase(contentType)) {
                return new AIReviewResult("duplicate", true, "SKIP", "Duplicate check not implemented for " + contentType, 0.8);
            }

            SolutionEntity solution = solutionMapper.selectActiveById(contentId);
            if (solution == null) {
                return new AIReviewResult("duplicate", false, "NOT_FOUND", "Content not found", 1.0);
            }

            // 简单的标题相似度检测
            String title = solution.getTitle() != null ? solution.getTitle().toLowerCase() : "";
            
            // 检查是否存在标题完全相同的 Solution
            List<SolutionEntity> allSolutions = solutionMapper.selectAll();
            long similarCount = allSolutions.stream()
                    .filter(s -> !s.getId().equals(contentId))
                    .filter(s -> s.getTitle() != null)
                    .filter(s -> s.getTitle().toLowerCase().equals(title))
                    .count();

            if (similarCount > 0) {
                return new AIReviewResult("duplicate", false, "DUPLICATE", 
                        "Found " + similarCount + " solutions with similar title", 0.85);
            }

            return new AIReviewResult("duplicate", true, "PASS", "No duplicates detected", 0.9);

        } catch (Exception e) {
            log.error("Duplicate check failed: {}", e.getMessage(), e);
            return new AIReviewResult("duplicate", false, "ERROR", e.getMessage(), 0.5);
        }
    }

    @Override
    public QualityScore getQualityScore(Long contentId, String contentType) {
        try {
            switch (contentType.toLowerCase()) {
                case "solution" -> {
                    SolutionEntity solution = solutionMapper.selectActiveById(contentId);
                    if (solution == null) {
                        return new QualityScore(0, 0, 0, 0, "Content not found");
                    }
                    return calculateSolutionQualityScore(solution);
                }
                default -> {
                    return new QualityScore(0.5, 0.5, 0.5, 0.5, "Quality score not implemented for " + contentType);
                }
            }
        } catch (Exception e) {
            log.error("Quality score calculation failed: {}", e.getMessage(), e);
            return new QualityScore(0, 0, 0, 0, "Error: " + e.getMessage());
        }
    }

    @Override
    public AIReviewReport fullReview(Long contentId, String contentType) {
        List<AIReviewResult> results = new ArrayList<>();
        List<AIReviewResult> additionalChecks = new ArrayList<>();

        // 执行各项检查
        AIReviewResult structureResult = checkStructureIntegrity(contentId, contentType);
        results.add(structureResult);

        AIReviewResult sensitiveResult = checkSensitiveInfo(contentId, contentType);
        results.add(sensitiveResult);

        AIReviewResult duplicateResult = checkDuplicate(contentId, contentType);
        results.add(duplicateResult);

        // 计算质量分数
        QualityScore qualityScore = getQualityScore(contentId, contentType);

        // 额外检查：风险等级检查
        if ("solution".equalsIgnoreCase(contentType)) {
            SolutionEntity solution = solutionMapper.selectActiveById(contentId);
            if (solution != null && solution.getRiskLevel() != null) {
                additionalChecks.add(new AIReviewResult(
                        "risk_level",
                        solution.getRiskLevel() <= 2,
                        solution.getRiskLevel() <= 2 ? "PASS" : "HIGH_RISK",
                        "Risk level: " + solution.getRiskLevel(),
                        0.95
                ));
            }
        }

        // 综合判断
        boolean overallPass = results.stream()
                .filter(r -> !"SKIP".equals(r.verdict()))
                .allMatch(AIReviewResult::passed);

        String recommendation = overallPass ? "APPROVE" : "NEEDS_REVIEW";

        return new AIReviewReport(
                contentId,
                contentType,
                overallPass,
                structureResult.passed(),
                sensitiveResult.passed(),
                duplicateResult.passed(),
                qualityScore,
                additionalChecks,
                recommendation,
                System.currentTimeMillis()
        );
    }

    // ========== 辅助方法 ==========

    private AIReviewResult checkSolutionStructure(SolutionEntity solution) {
        List<String> missingFields = new ArrayList<>();

        if (solution.getTitle() == null || solution.getTitle().isBlank()) {
            missingFields.add("title");
        }
        if (solution.getTechStack() == null || solution.getTechStack().isBlank()) {
            missingFields.add("tech_stack");
        }
        if (solution.getStatus() == null) {
            missingFields.add("status");
        }

        if (missingFields.isEmpty()) {
            return new AIReviewResult("structure", true, "PASS", "All required fields present", 0.95);
        }

        return new AIReviewResult("structure", false, "INCOMPLETE", 
                "Missing fields: " + String.join(", ", missingFields), 0.9);
    }

    private AIReviewResult checkPublicCaseStructure(PublicCaseEntity entity) {
        List<String> missingFields = new ArrayList<>();

        if (entity.getSourceCaseId() == null) {
            missingFields.add("source_case_id");
        }
        if (entity.getRedactionStatus() == null || !"complete".equalsIgnoreCase(entity.getRedactionStatus())) {
            missingFields.add("redaction_status");
        }

        if (missingFields.isEmpty()) {
            return new AIReviewResult("structure", true, "PASS", "All required fields present", 0.95);
        }

        return new AIReviewResult("structure", false, "INCOMPLETE", 
                "Missing fields: " + String.join(", ", missingFields), 0.9);
    }

    private String getContentText(Long contentId, String contentType) {
        switch (contentType.toLowerCase()) {
            case "solution" -> {
                SolutionEntity solution = solutionMapper.selectActiveById(contentId);
                return solution != null ? solution.getTitle() + " " + solution.getTechStack() : null;
            }
            case "public_case" -> {
                PublicCaseEntity entity = publicCaseMapper.selectActiveById(contentId);
                return entity != null ? "public_case_" + entity.getId() : null;
            }
            default -> {
                return null;
            }
        }
    }

    private QualityScore calculateSolutionQualityScore(SolutionEntity solution) {
        double completenessScore = 0.5;
        double clarityScore = 0.5;
        double accuracyScore = 0.5;

        // 完整性评分
        int fieldsPresent = 0;
        int totalFields = 8;
        if (solution.getTitle() != null && !solution.getTitle().isBlank()) fieldsPresent++;
        if (solution.getTechStack() != null && !solution.getTechStack().isBlank()) fieldsPresent++;
        if (solution.getDomain() != null && !solution.getDomain().isBlank()) fieldsPresent++;
        if (solution.getErrorSignature() != null && !solution.getErrorSignature().isBlank()) fieldsPresent++;
        if (solution.getApplicability() != null && !solution.getApplicability().isBlank()) fieldsPresent++;
        if (solution.getInapplicability() != null && !solution.getInapplicability().isBlank()) fieldsPresent++;
        if (solution.getFailurePaths() != null && !solution.getFailurePaths().isBlank()) fieldsPresent++;
        if (solution.getEnvironment() != null && !solution.getEnvironment().isBlank()) fieldsPresent++;
        completenessScore = (double) fieldsPresent / totalFields;

        // 清晰度评分（基于标题长度）
        String title = solution.getTitle() != null ? solution.getTitle() : "";
        if (title.length() >= 10 && title.length() <= 100) {
            clarityScore = 0.9;
        } else if (title.length() > 0) {
            clarityScore = 0.6;
        }

        // 准确性评分（基于验证等级）
        if (solution.getVerificationLevel() != null) {
            accuracyScore = Math.min(1.0, solution.getVerificationLevel().getLevel() / 5.0 * 0.8 + 0.2);
        }

        double overallScore = completenessScore * 0.4 + clarityScore * 0.3 + accuracyScore * 0.3;
        String recommendations = generateQualityRecommendations(completenessScore, clarityScore, accuracyScore);

        return new QualityScore(overallScore, completenessScore, clarityScore, accuracyScore, recommendations);
    }

    private String generateQualityRecommendations(double completeness, double clarity, double accuracy) {
        List<String> suggestions = new ArrayList<>();

        if (completeness < 0.7) {
            suggestions.add("建议补充更多字段信息以提高完整性");
        }
        if (clarity < 0.7) {
            suggestions.add("建议优化标题使其更清晰明确");
        }
        if (accuracy < 0.7) {
            suggestions.add("建议提供更多验证证据以提高准确性");
        }

        if (suggestions.isEmpty()) {
            return "内容质量良好";
        }
        return String.join("; ", suggestions);
    }
}
