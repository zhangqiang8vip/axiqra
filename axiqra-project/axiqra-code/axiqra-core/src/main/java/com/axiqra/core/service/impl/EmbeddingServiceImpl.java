package com.axiqra.core.service.impl;

import com.axiqra.common.domain.service.EmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 向量嵌入服务实现
 *
 * 支持多种嵌入提供商：
 * - Ollama 本地模型 (默认，免费)
 * - OpenAI text-embedding-3-small
 * - Cohere embed-v3
 * - 任何兼容 OpenAI API 格式的 LLM
 *
 * @author Axiqra Team
 * @date 2026-06-24
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final String provider;
    private final int dimension;

    public EmbeddingServiceImpl(
            @Value("${axiqra.embedding.base-url:http://localhost:11434}") String baseUrl,
            @Value("${axiqra.embedding.model:nomic-embed-text}") String model,
            @Value("${axiqra.embedding.api-key:${OPENAI_API_KEY:}}") String apiKey,
            @Value("${axiqra.embedding.dimension:768}") int dimension,
            @Value("${axiqra.embedding.provider:auto}") String provider) {
        
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
        this.dimension = dimension;
        
        // 自动检测提供商
        this.provider = detectProvider(baseUrl, provider);
        
        this.objectMapper = new ObjectMapper();
        
        // 根据提供商配置 WebClient
        if ("ollama".equals(this.provider)) {
            this.webClient = WebClient.builder()
                    .baseUrl(baseUrl + "/api")
                    .build();
            log.info("EmbeddingService initialized with Ollama: baseUrl={}, model={}", baseUrl, model);
        } else {
            this.webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
            log.info("EmbeddingService initialized: provider={}, baseUrl={}, model={}", 
                    this.provider, baseUrl, model);
        }
    }

    private String detectProvider(String baseUrl, String provider) {
        if ("auto".equalsIgnoreCase(provider)) {
            if (baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1") || baseUrl.contains("ollama")) {
                return "ollama";
            } else if (baseUrl.contains("openai")) {
                return "openai";
            } else if (baseUrl.contains("cohere")) {
                return "cohere";
            }
            return "openai";
        }
        return provider.toLowerCase();
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            log.warn("Attempt to embed empty text, returning zero vector");
            return new float[dimension];
        }

        try {
            return switch (provider) {
                case "ollama" -> embedWithOllama(text);
                case "cohere" -> embedWithCohere(text);
                default -> embedWithOpenAI(text);
            };
        } catch (Exception e) {
            log.error("Failed to embed text: {}", e.getMessage(), e);
            return new float[dimension];
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<float[]> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public double cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA == null || vecB == null || vecA.length != vecB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator < 1e-10) {
            return 0.0;
        }

        return dotProduct / denominator;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    /**
     * 使用 Ollama API 生成嵌入向量
     * Ollama 是免费的本地 LLM 服务，支持多种嵌入模型
     */
    private float[] embedWithOllama(String text) throws Exception {
        String requestBody = objectMapper.writeValueAsString(new OllamaEmbeddingRequest(
                model,
                text,
                false
        ));

        String response = webClient.post()
                .uri("/embeddings")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode root = objectMapper.readTree(response);
        JsonNode embeddingNode = root.path("embedding");

        List<Float> embeddingList = objectMapper.readValue(
                embeddingNode.traverse(),
                new TypeReference<List<Float>>() {}
        );

        float[] result = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            result[i] = embeddingList.get(i);
        }

        log.debug("Ollama embedded text to vector with {} dimensions", result.length);
        return result;
    }

    /**
     * 使用 OpenAI API 格式生成嵌入向量
     */
    private float[] embedWithOpenAI(String text) throws Exception {
        String requestBody = objectMapper.writeValueAsString(new OpenAIEmbeddingRequest(
                text,
                model
        ));

        String response = webClient.post()
                .uri("/v1/embeddings")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode root = objectMapper.readTree(response);
        JsonNode embeddingNode = root.path("data").path(0).path("embedding");

        List<Float> embeddingList = objectMapper.readValue(
                embeddingNode.traverse(),
                new TypeReference<List<Float>>() {}
        );

        float[] result = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            result[i] = embeddingList.get(i);
        }

        log.debug("OpenAI embedded text to vector with {} dimensions", result.length);
        return result;
    }

    /**
     * 使用 Cohere API 生成嵌入向量
     */
    private float[] embedWithCohere(String text) throws Exception {
        String requestBody = objectMapper.writeValueAsString(new CohereEmbeddingRequest(
                model,
                text,
                "float"
        ));

        String response = webClient.post()
                .uri("/v1/embed")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode root = objectMapper.readTree(response);
        JsonNode embeddingsNode = root.path("embeddings");

        List<Float> embeddingList = objectMapper.readValue(
                embeddingsNode.traverse(),
                new TypeReference<List<Float>>() {}
        );

        float[] result = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            result[i] = embeddingList.get(i);
        }

        log.debug("Cohere embedded text to vector with {} dimensions", result.length);
        return result;
    }

    /**
     * 将向量转换为 SQL 格式（用于 CockroachDB 向量检索）
     */
    public String toSqlVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    // ========== 请求体类 ==========

    private static class OllamaEmbeddingRequest {
        public String model;
        public String prompt;
        public boolean truncate;

        public OllamaEmbeddingRequest(String model, String prompt, boolean truncate) {
            this.model = model;
            this.prompt = prompt;
            this.truncate = truncate;
        }
    }

    private static class OpenAIEmbeddingRequest {
        public String input;
        public String model;

        public OpenAIEmbeddingRequest(String input, String model) {
            this.input = input;
            this.model = model;
        }
    }

    private static class CohereEmbeddingRequest {
        public String model;
        public String texts;
        public String inputType;
        public String embeddingTypes;

        public CohereEmbeddingRequest(String model, String text, String embeddingType) {
            this.model = model;
            this.texts = text;
            this.inputType = "search_document";
            this.embeddingTypes = embeddingType;
        }
    }
}
