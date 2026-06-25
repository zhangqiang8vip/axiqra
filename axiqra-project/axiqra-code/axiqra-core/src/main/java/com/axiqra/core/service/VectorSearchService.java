package com.axiqra.core.service;

import com.axiqra.common.domain.entity.SolutionEntity;
import com.axiqra.common.domain.vo.SearchResultItemVO;

import java.util.List;

/**
 * 向量搜索服务接口
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
public interface VectorSearchService {

    /**
     * 通过向量相似度搜索 Solution
     *
     * @param queryText 搜索文本
     * @param limit 返回数量限制
     * @return 向量搜索结果列表
     */
    List<VectorSearchResult> searchByVector(String queryText, int limit);

    /**
     * 混合搜索：结合关键词和向量相似度
     *
     * @param queryText 搜索文本
     * @param keywordScore 关键词搜索分数
     * @param limit 返回数量限制
     * @return 混合排序后的搜索结果
     */
    List<VectorSearchResult> hybridSearch(String queryText, double keywordScore, int limit);

    /**
     * 为 Solution 生成并更新向量嵌入
     *
     * @param solutionId Solution ID
     * @return 是否成功
     */
    boolean generateEmbedding(Long solutionId);

    /**
     * 批量为 Solution 生成向量嵌入
     *
     * @param solutionIds Solution ID 列表
     * @return 成功的数量
     */
    int generateEmbeddingBatch(List<Long> solutionIds);

    /**
     * 向量搜索结果
     */
    record VectorSearchResult(
            Long solutionId,
            String solutionCode,
            double vectorSimilarity,
            double finalScore
    ) {}
}
