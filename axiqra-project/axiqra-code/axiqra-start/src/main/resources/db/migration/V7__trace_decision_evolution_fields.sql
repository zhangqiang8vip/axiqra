-- ============================================================
-- Axiqra S1: Engineering Trace 扩展字段
-- ============================================================
-- 添加 decision_path 和 evolution_hint 字段
-- 与 InvocationEntity 保持一致
-- 对应 D06 §3 Engineering Trace Package 规范
-- ============================================================

-- 1. 添加决策路径字段 (JSONB)
ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS decision_path JSONB;

COMMENT ON COLUMN axiqra_engineering_trace.decision_path IS '决策路径 JSON: 关键决策点和选择理由';

-- 2. 添加演进提示字段 (TEXT)
ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS evolution_hint TEXT;

COMMENT ON COLUMN axiqra_engineering_trace.evolution_hint IS '演进提示: 方案的演进方向和优化建议';

-- 3. 创建索引以加速路径查询
CREATE INDEX IF NOT EXISTS idx_trace_decision_path
ON axiqra_engineering_trace USING GIN (decision_path)
WHERE decision_path IS NOT NULL;

-- evolution_hint 不需要 GIN 索引，因为它是纯文本
CREATE INDEX IF NOT EXISTS idx_trace_evolution_hint
ON axiqra_engineering_trace (evolution_hint)
WHERE evolution_hint IS NOT NULL;

-- 4. 验证查询
-- SELECT id, task_goal,
--        decision_path IS NOT NULL AS has_decision_path,
--        evolution_hint IS NOT NULL AS has_evolution_hint
-- FROM axiqra_engineering_trace
-- LIMIT 10;
