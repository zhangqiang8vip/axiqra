-- ============================================================
-- Axiqra S1.1: V9 兜底迁移
-- ============================================================
-- 背景：V9 在 CockroachDB v25.4 上存在索引依赖问题：
--   DROP COLUMN 必须在 DROP INDEX 之前，否则报
--   "cannot drop column X because it is referenced by partial index"。
--   部分环境下 V9 中途失败，会留下：
--     - 已 drop 的部分列
--     - 仍残留的部分列（forward_path / decision_path / rollback_path / evolution_hint / reverse_path）
--     - 仍残留的 4 个 GIN 索引（idx_invocation_*_path）
--
-- 本迁移以 IF EXISTS 形式兜底清理这些残留，幂等可重复执行。
-- ============================================================

-- ===================== 先删除依赖的 GIN 索引（幂等） =====================
DROP INDEX IF EXISTS axiqra."idx_invocation_forward_path";
DROP INDEX IF EXISTS axiqra."idx_invocation_decision_path";
DROP INDEX IF EXISTS axiqra."idx_invocation_rollback_path";
DROP INDEX IF EXISTS axiqra."idx_invocation_reverse_path";
DROP INDEX IF EXISTS "idx_invocation_forward_path";
DROP INDEX IF EXISTS "idx_invocation_decision_path";
DROP INDEX IF EXISTS "idx_invocation_rollback_path";
DROP INDEX IF EXISTS "idx_invocation_reverse_path";

-- ===================== 再删除 invocation 上的 5 个路径字段（幂等） =====================
ALTER TABLE axiqra.axiqra_invocation DROP COLUMN IF EXISTS forward_path;
ALTER TABLE axiqra.axiqra_invocation DROP COLUMN IF EXISTS decision_path;
ALTER TABLE axiqra.axiqra_invocation DROP COLUMN IF EXISTS rollback_path;
ALTER TABLE axiqra.axiqra_invocation DROP COLUMN IF EXISTS evolution_hint;
ALTER TABLE axiqra.axiqra_invocation DROP COLUMN IF EXISTS reverse_path;
ALTER TABLE axiqra_invocation DROP COLUMN IF EXISTS forward_path;
ALTER TABLE axiqra_invocation DROP COLUMN IF EXISTS decision_path;
ALTER TABLE axiqra_invocation DROP COLUMN IF EXISTS rollback_path;
ALTER TABLE axiqra_invocation DROP COLUMN IF EXISTS evolution_hint;
ALTER TABLE axiqra_invocation DROP COLUMN IF EXISTS reverse_path;

-- ===================== 验证：理论上应返回 0 行 =====================
-- 残留索引检查：
-- SELECT indexname FROM pg_indexes
-- WHERE tablename = 'axiqra_invocation'
--   AND indexname LIKE 'idx_invocation_%_path';
--
-- 残留列检查：
-- SELECT column_name FROM information_schema.columns
-- WHERE table_name = 'axiqra_invocation'
--   AND column_name IN ('forward_path','decision_path','rollback_path','evolution_hint','reverse_path');