package com.axiqra.core.service;

/**
 * AI 审核服务接口
 *
 * 提供 AI 辅助的内容审核能力
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
public interface AIReviewService {

    /**
     * 执行结构完整性检查
     *
     * @param contentId 内容 ID
     * @param contentType 内容类型 (solution, public_case, project_case)
     * @return 检查结果
     */
    AIReviewResult checkStructureIntegrity(Long contentId, String contentType);

    /**
     * 执行敏感信息检测
     *
     * @param contentId 内容 ID
     * @param contentType 内容类型
     * @return 检查结果
     */
    AIReviewResult checkSensitiveInfo(Long contentId, String contentType);

    /**
     * 执行重复内容检测
     *
     * @param contentId 内容 ID
     * @param contentType 内容类型
     * @return 检查结果
     */
    AIReviewResult checkDuplicate(Long contentId, String contentType);

    /**
     * 执行质量评分
     *
     * @param contentId 内容 ID
     * @param contentType 内容类型
     * @return 质量评分结果
     */
    QualityScore getQualityScore(Long contentId, String contentType);

    /**
     * 执行完整 AI 审核
     *
     * @param contentId 内容 ID
     * @param contentType 内容类型
     * @return 完整审核结果
     */
    AIReviewReport fullReview(Long contentId, String contentType);

    /**
     * AI 审核结果
     */
    record AIReviewResult(
            String checkType,
            boolean passed,
            String verdict,
            String details,
            double confidence
    ) {}

    /**
     * 质量评分
     */
    record QualityScore(
            double overallScore,
            double completenessScore,
            double clarityScore,
            double accuracyScore,
            String recommendations
    ) {}

    /**
     * 完整审核报告
     */
    record AIReviewReport(
            Long contentId,
            String contentType,
            boolean overallPass,
            boolean structurePassed,
            boolean sensitiveInfoPassed,
            boolean duplicatePassed,
            QualityScore qualityScore,
            java.util.List<AIReviewResult> additionalChecks,
            String recommendation,
            Long generatedAt
    ) {}
}
