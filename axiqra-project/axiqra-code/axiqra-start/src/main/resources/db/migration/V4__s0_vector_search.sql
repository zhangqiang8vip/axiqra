-- ============================================================
-- Axiqra S0: pgvector 向量搜索支持
-- ============================================================
-- 向量字段用于语义搜索，支持 AI Agent 的智能方案检索
-- ============================================================

-- 1. 检查并启用向量扩展（PostgreSQL 需要 pgvector）
-- 注意：CockroachDB 从 v23.1 开始支持原生向量类型

-- 2. 添加向量字段到 solution 表
-- 使用可变长度的浮点数组存储嵌入向量
ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS embedding VECTOR(1536);

COMMENT ON COLUMN axiqra_solution.embedding IS 'Solution 的语义向量嵌入，用于向量相似度搜索';

-- 3. 创建向量索引以加速相似度搜索
-- 使用 IVFFlat 索引，适合百万级数据
-- 对于小数据集，可以考虑使用 HNSW 索引（更高召回率但索引更大）

-- CockroachDB 兼容的向量索引
CREATE INDEX IF NOT EXISTS idx_solution_embedding_ivfflat
ON axiqra_solution USING GIN (embedding)
WHERE embedding IS NOT NULL;

-- 备选：HNSW 索引（更高精度但索引构建更慢）
-- CREATE INDEX IF NOT EXISTS idx_solution_embedding_hnsw
-- ON axiqra_solution USING HNSW (embedding)
-- WHERE embedding IS NOT NULL;

-- 4. 向量搜索参考 SQL

-- 4.1 余弦相似度搜索（最常用）
-- SELECT id, solution_code, title,
--        1 - (embedding <=> '[0.1, 0.2, ...]::vector) AS similarity
-- FROM axiqra_solution
-- WHERE embedding IS NOT NULL
-- ORDER BY embedding <=> '[0.1, 0.2, ...]::vector
-- LIMIT 10;

-- 4.2 混合搜索（关键词 + 向量）
-- SELECT s.*,
--        (0.4 * keyword_score + 0.6 * vector_similarity) AS final_score
-- FROM axiqra_solution s,
--      (SELECT id AS match_id,
--              CASE WHEN title ILIKE '%keyword%' THEN 1.0 ELSE 0.5 END AS keyword_score
--       FROM axiqra_solution
--       WHERE title ILIKE '%keyword%' OR domain ILIKE '%keyword%') kw
-- WHERE s.id = kw.match_id
--   AND s.embedding IS NOT NULL
-- ORDER BY final_score DESC
-- LIMIT 20;

-- 5. 验证查询
-- SELECT id, solution_code, embedding IS NOT NULL AS has_embedding,
--       array_length(embedding, 1) AS dimensions
-- FROM axiqra_solution
-- LIMIT 10;
