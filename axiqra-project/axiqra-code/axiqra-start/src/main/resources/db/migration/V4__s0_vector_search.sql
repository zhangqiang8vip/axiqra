-- ============================================================
-- Axiqra S0: 向量搜索支持 (CockroachDB 兼容)
-- ============================================================
-- 向量字段用于语义搜索，支持 AI Agent 的智能方案检索
-- ============================================================

-- 1. 添加向量字段到 solution 表
-- 使用 FLOAT[] 数组类型存储嵌入向量（1536 维）
ALTER TABLE axiqra_solution ADD COLUMN IF NOT EXISTS embedding FLOAT[];

-- 2. 添加注释
COMMENT ON COLUMN axiqra_solution.embedding IS 'Solution 的语义向量嵌入，用于向量相似度搜索';

-- 3. 尝试创建向量索引（可选，CockroachDB v24.1+）
-- 使用 HNSW 索引加速相似度搜索
-- 注意：此语句在旧版本 CockroachDB 会失败，但不影响迁移继续
