package com.axiqra.common.domain.service;

import java.util.List;

/**
 * 向量嵌入服务接口
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
public interface EmbeddingService {

    /**
     * 将文本转换为向量嵌入
     *
     * @param text 输入文本
     * @return 归一化后的向量数组
     */
    float[] embed(String text);

    /**
     * 批量将文本转换为向量嵌入
     *
     * @param texts 输入文本列表
     * @return 归一化后的向量数组列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vecA 向量A
     * @param vecB 向量B
     * @return 余弦相似度 (0-1)
     */
    double cosineSimilarity(float[] vecA, float[] vecB);

    /**
     * 获取当前嵌入模型的维度
     *
     * @return 嵌入向量维度
     */
    int getDimension();

    /**
     * 获取嵌入服务提供商名称
     *
     * @return provider name (e.g., "openai", "cohere", "local")
     */
    String getProvider();
}
