-- ============================================================
-- Axiqra S0: Engineering Trace 路径字段扩展
-- ============================================================
-- 添加 Engineering Trace Package 的完整路径追踪能力
-- 对应 D05/D07 文档规定的 Trace 路径字段
-- ============================================================

-- 1. 添加正向路径字段
ALTER TABLE axiqra_invocation
ADD COLUMN IF NOT EXISTS forward_path JSONB;

COMMENT ON COLUMN axiqra_invocation.forward_path IS '正向路径 JSON: 成功解决问题的步骤路径';

-- 2. 添加决策路径字段
ALTER TABLE axiqra_invocation
ADD COLUMN IF NOT EXISTS decision_path JSONB;

COMMENT ON COLUMN axiqra_invocation.decision_path IS '决策路径 JSON: 关键决策点和选择理由';

-- 3. 添加回滚路径字段
ALTER TABLE axiqra_invocation
ADD COLUMN IF NOT EXISTS rollback_path JSONB;

COMMENT ON COLUMN axiqra_invocation.rollback_path IS '回滚路径 JSON: 失败时的回滚步骤';

-- 4. 添加演化提示字段
ALTER TABLE axiqra_invocation
ADD COLUMN IF NOT EXISTS evolution_hint TEXT;

COMMENT ON COLUMN axiqra_invocation.evolution_hint IS '演化提示: 方案的演进方向和优化建议';

-- 5. 添加反向路径字段
ALTER TABLE axiqra_invocation
ADD COLUMN IF NOT EXISTS reverse_path JSONB;

COMMENT ON COLUMN axiqra_invocation.reverse_path IS '反向路径 JSON: 从结果反向推导的过程';

-- 6. 添加向量嵌入字段（如果尚未添加）
-- 注意：向量字段在 V4__s0_vector_search.sql 中定义

-- 7. 创建索引以加速路径查询
CREATE INDEX IF NOT EXISTS idx_invocation_forward_path
ON axiqra_invocation USING GIN (forward_path)
WHERE forward_path IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invocation_decision_path
ON axiqra_invocation USING GIN (decision_path)
WHERE decision_path IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invocation_rollback_path
ON axiqra_invocation USING GIN (rollback_path)
WHERE rollback_path IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invocation_reverse_path
ON axiqra_invocation USING GIN (reverse_path)
WHERE reverse_path IS NOT NULL;

-- 8. 验证查询
-- SELECT id, invocation_code,
--        forward_path IS NOT NULL AS has_forward_path,
--        decision_path IS NOT NULL AS has_decision_path,
--        rollback_path IS NOT NULL AS has_rollback_path,
--        evolution_hint IS NOT NULL AS has_evolution_hint,
--        reverse_path IS NOT NULL AS has_reverse_path
-- FROM axiqra_invocation
-- LIMIT 10;
