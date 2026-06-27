package com.axiqra.core.service.impl;

import com.axiqra.common.domain.entity.SolutionEntity;
import com.axiqra.common.domain.service.EmbeddingService;
import com.axiqra.core.mapper.SolutionMapper;
import com.axiqra.core.service.VectorSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 向量搜索服务实现
 *
 * 使用 CockroachDB 原生向量支持实现语义搜索
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Slf4j
@Service
public class VectorSearchServiceImpl implements VectorSearchService {

    private final EmbeddingService embeddingService;
    private final SolutionMapper solutionMapper;
    private final JdbcTemplate jdbcTemplate;

    public VectorSearchServiceImpl(EmbeddingService embeddingService,
                                   SolutionMapper solutionMapper,
                                   @Qualifier("dataSource") javax.sql.DataSource dataSource) {
        this.embeddingService = embeddingService;
        this.solutionMapper = solutionMapper;
        // 强制使用业务主库 (CockroachDB) 的 DataSource,
        // 避免被自动注入到 auditJdbcTemplate (audit-DB) 上。
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static final double VECTOR_WEIGHT = 0.6;
    private static final double KEYWORD_WEIGHT = 0.4;
    // 阈值极低 — 让向量结果至少返回,排序后再过滤。
    // 真实场景应基于业务验证 L2/cosine 距离-相似度的合理阈值。
    private static final double MIN_SIMILARITY_THRESHOLD = -1.0;

    @Override
    public List<VectorSearchResult> searchByVector(String queryText, int limit) {
        if (queryText == null || queryText.isBlank()) {
            return Collections.emptyList();
        }

        try {
            // 1. 生成查询文本的向量嵌入
            float[] queryEmbedding = embeddingService.embed(queryText);
            String embeddingJson = toPostgresVector(queryEmbedding);

            // 2. 执行向量相似度搜索
            // 注: CockroachDB 中 embedding 列为 float8[] 而非 pgvector 类型,
            // 必须显式 cast: s.embedding::vector <=> '...'::vector
            String sql = """
                SELECT s.id, s.solution_code,
                       1 - (s.embedding::vector <=> '%s'::vector) AS similarity
                FROM axiqra_solution s
                WHERE s.is_deleted = FALSE
                  AND s.visibility_scope = 'public'
                  AND s.status IN ('verified', 'stable', 'canonical')
                  AND s.embedding IS NOT NULL
                  AND (1 - (s.embedding::vector <=> '%s'::vector)) > %.2f
                ORDER BY s.embedding::vector <=> '%s'::vector
                LIMIT %d
                """.formatted(embeddingJson, embeddingJson, MIN_SIMILARITY_THRESHOLD, embeddingJson, limit);

            log.debug("Executing vector search: {}", sql);

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Long id = rs.getLong("id");
                String code = rs.getString("solution_code");
                double similarity = rs.getDouble("similarity");
                return new VectorSearchResult(id, code, similarity, similarity);
            });

        } catch (Exception e) {
            log.error("Vector search failed for query: {}, error: {}", queryText, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<VectorSearchResult> hybridSearch(String queryText, double keywordScore, int limit) {
        if (queryText == null || queryText.isBlank()) {
            return Collections.emptyList();
        }

        try {
            // 1. 生成查询文本的向量嵌入
            float[] queryEmbedding = embeddingService.embed(queryText);
            String embeddingJson = toPostgresVector(queryEmbedding);

            // 2. 执行混合搜索 SQL
            String sql = """
                SELECT s.id, s.solution_code,
                       1 - (s.embedding::vector <=> '%s'::vector) AS vector_similarity,
                       %.2f AS keyword_weight,
                       %.2f AS vector_weight,
                       (%.2f * %.2f + %.2f * (1 - (s.embedding::vector <=> '%s'::vector))) AS final_score
                FROM axiqra_solution s
                WHERE s.is_deleted = FALSE
                  AND s.visibility_scope = 'public'
                  AND s.status IN ('verified', 'stable', 'canonical')
                  AND s.embedding IS NOT NULL
                ORDER BY final_score DESC
                LIMIT %d
                """.formatted(
                    embeddingJson,
                    KEYWORD_WEIGHT, VECTOR_WEIGHT,
                    KEYWORD_WEIGHT, keywordScore,
                    VECTOR_WEIGHT,
                    embeddingJson,
                    limit
            );

            log.debug("Executing hybrid search: {}", sql);

            List<VectorSearchResult> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Long id = rs.getLong("id");
                String code = rs.getString("solution_code");
                double vectorSimilarity = rs.getDouble("vector_similarity");
                double finalScore = rs.getDouble("final_score");
                return new VectorSearchResult(id, code, vectorSimilarity, finalScore);
            });

            log.info("Hybrid search for '{}' returned {} results", queryText, results.size());
            return results;

        } catch (Exception e) {
            log.error("Hybrid search failed for query: {}, error: {}", queryText, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional
    public boolean generateEmbedding(Long solutionId) {
        if (solutionId == null) {
            return false;
        }

        try {
            // 1. 获取 Solution
            SolutionEntity solution = solutionMapper.selectActiveById(solutionId);
            if (solution == null) {
                log.warn("Solution not found for embedding generation: {}", solutionId);
                return false;
            }

            // 2. 构建要嵌入的文本（组合多个字段以获得更好的语义表示）
            String textToEmbed = buildEmbeddingText(solution);
            if (textToEmbed.isBlank()) {
                log.warn("Empty text for embedding generation: solutionId={}", solutionId);
                return false;
            }

            // 3. 生成向量嵌入
            float[] embedding = embeddingService.embed(textToEmbed);
            String embeddingJson = toPostgresVector(embedding);

            // 4. 更新 Solution 的 embedding 字段
            String updateSql = """
                UPDATE axiqra_solution
                SET embedding = '%s'::vector, gmt_modified = NOW()
                WHERE id = %d AND is_deleted = FALSE
                """.formatted(embeddingJson, solutionId);

            int updated = jdbcTemplate.update(updateSql);
            if (updated > 0) {
                log.info("Successfully generated embedding for solution: {}", solutionId);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Failed to generate embedding for solution: {}, error: {}", solutionId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public int generateEmbeddingBatch(List<Long> solutionIds) {
        if (solutionIds == null || solutionIds.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Long solutionId : solutionIds) {
            if (generateEmbedding(solutionId)) {
                successCount++;
            }
        }

        log.info("Batch embedding generation completed: {}/{} successful", successCount, solutionIds.size());
        return successCount;
    }

    /**
     * 构建用于向量嵌入的文本
     */
    private String buildEmbeddingText(SolutionEntity solution) {
        StringBuilder sb = new StringBuilder();

        // 标题
        if (solution.getTitle() != null) {
            sb.append(solution.getTitle()).append(". ");
        }

        // 领域
        if (solution.getDomain() != null) {
            sb.append(solution.getDomain()).append(". ");
        }

        // 技术栈
        if (solution.getTechStack() != null) {
            sb.append(solution.getTechStack()).append(". ");
        }

        // 适用条件
        if (solution.getApplicability() != null) {
            sb.append(solution.getApplicability()).append(". ");
        }

        // 不适用边界
        if (solution.getInapplicability() != null) {
            sb.append("Not applicable when: ").append(solution.getInapplicability()).append(". ");
        }

        // 失败路径
        if (solution.getFailurePaths() != null) {
            sb.append("Known failure paths: ").append(solution.getFailurePaths()).append(". ");
        }

        return sb.toString().trim();
    }

    /**
     * 将 float[] 转换为 PostgreSQL/CockroachDB 向量格式
     */
    private String toPostgresVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
