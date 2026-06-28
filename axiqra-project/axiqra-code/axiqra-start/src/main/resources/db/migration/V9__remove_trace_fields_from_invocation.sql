-- ============================================================
-- Axiqra S1: 修正错放的 Engineering Trace 路径字段
-- ============================================================
-- P0-1 问题：V5 将 Trace 的 5 个路径字段错误地加到了 axiqra_invocation 表
-- 这些字段属于 axiqra_engineering_trace，不属于 axiqra_invocation
-- 修复：删除 invocation 上的 5 个路径字段，改为在 trace 上补充缺失字段（V10）
--
-- 变更内容：
-- 1. 删除 axiqra_invocation.forward_path（正向路径）
-- 2. 删除 axiqra_invocation.decision_path（决策路径）
-- 3. 删除 axiqra_invocation.rollback_path（回滚路径）
-- 4. 删除 axiqra_invocation.evolution_hint（演化提示）
-- 5. 删除 axiqra_invocation.reverse_path（反向路径）
-- 6. 删除对应的 4 个 GIN 索引
--
-- 对应文档：D06 §9（Invocation 和 Feedback）/ D06 §15（字段矩阵）
-- Engineering Trace 路径字段补充：V10
--
-- S1.1 修正：CockroachDB v25.4 要求必须先 DROP 依赖索引，再 DROP 列。
-- 顺序倒置会导致 `ERROR: cannot drop column "X" because it is referenced
-- by partial/index`，迁移失败。详见 https://go.crdb.dev/issue-v/97372
-- ============================================================

-- ===================== 先删除依赖的 GIN 索引（必须在 DROP COLUMN 之前） =====================

DROP INDEX IF EXISTS idx_invocation_forward_path;
DROP INDEX IF EXISTS idx_invocation_decision_path;
DROP INDEX IF EXISTS idx_invocation_rollback_path;
DROP INDEX IF EXISTS idx_invocation_reverse_path;

-- ===================== 再删除 invocation 上的 5 个路径字段 =====================

ALTER TABLE axiqra_invocation DROP COLUMN IF EXISTS forward_path;
ALTER TABLE axiqra_invocation DROP COLUMN IF EXISTS decision_path;
ALTER TABLE axiqra_invocation DROP COLUMN IF EXISTS rollback_path;
ALTER TABLE axiqra_invocation DROP COLUMN IF EXISTS evolution_hint;
ALTER TABLE axiqra_invocation DROP COLUMN IF EXISTS reverse_path;

-- ===================== 验证 =====================
-- SELECT COUNT(*) AS invocation_count FROM axiqra_invocation;
-- SELECT COUNT(*) AS trace_count FROM axiqra_engineering_trace WHERE forward_path IS NOT NULL;
