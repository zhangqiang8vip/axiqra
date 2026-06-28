-- ============================================================
-- Axiqra S1: Solution 演化关系字段
-- ============================================================
-- P0-3: Solution 表缺少 D08 §15 规定的演化关系字段
--
-- 变更内容：
-- 1. parent_solution_id (BIGINT) - 父 Solution ID（merge / fork / split 来源）
-- 2. successor_solution_id (BIGINT) - 继承 Solution ID（merge 到 / superseded 到）
-- 3. fork_of_solution_id (BIGINT) - fork 来源 Solution ID
-- 4. evolution_kind (VARCHAR) - 演化类型：merge / fork / supersede / split / rollback
--
-- 对应文档：D08 §15 合并、分叉与废弃规则
-- D15 贡献者激励需要追溯贡献的 Case 最终融合到哪个 Solution
-- ============================================================

-- ===================== 1. parent_solution_id =====================
ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS parent_solution_id BIGINT;

COMMENT ON COLUMN axiqra_solution.parent_solution_id
IS '父 Solution ID：merge / fork / split 操作的来源 Solution，用于追溯血缘关系';

CREATE INDEX IF NOT EXISTS idx_solution_parent
ON axiqra_solution (parent_solution_id)
WHERE parent_solution_id IS NOT NULL;

-- ===================== 2. successor_solution_id =====================
ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS successor_solution_id BIGINT;

COMMENT ON COLUMN axiqra_solution.successor_solution_id
IS '继承 Solution ID：本 Solution 被合并到 / 被替代为哪个新 Solution';

CREATE INDEX IF NOT EXISTS idx_solution_successor
ON axiqra_solution (successor_solution_id)
WHERE successor_solution_id IS NOT NULL;

-- ===================== 3. fork_of_solution_id =====================
ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS fork_of_solution_id BIGINT;

COMMENT ON COLUMN axiqra_solution.fork_of_solution_id
IS 'Fork 来源 Solution ID：本 Solution 是从哪个 Solution fork 出来的';

CREATE INDEX IF NOT EXISTS idx_solution_fork_source
ON axiqra_solution (fork_of_solution_id)
WHERE fork_of_solution_id IS NOT NULL;

-- ===================== 4. evolution_kind =====================
ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS evolution_kind VARCHAR(20);

COMMENT ON COLUMN axiqra_solution.evolution_kind
IS '演化类型：merge（融合）/ fork（分叉）/ supersede（替代）/ split（拆分）/ rollback（回滚）/ null（原始）';

CREATE INDEX IF NOT EXISTS idx_solution_evolution_kind
ON axiqra_solution (evolution_kind)
WHERE evolution_kind IS NOT NULL;

-- ===================== 验证 =====================
-- SELECT id, title, evolution_kind, parent_solution_id, successor_solution_id, fork_of_solution_id
-- FROM axiqra_solution
-- WHERE evolution_kind IS NOT NULL
-- LIMIT 20;
