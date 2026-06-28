-- ============================================================
-- Axiqra S1: 补全审计字段 created_by / updated_by
-- ============================================================
-- P0-4: 4 张主表缺少 created_by / updated_by 审计字段
--
-- 变更内容：
-- 1. axiqra_user: 添加 created_by / updated_by
-- 2. axiqra_workspace: 添加 created_by / updated_by
-- 3. axiqra_engineering_trace: 添加 created_by / updated_by
-- 4. axiqra_solution: 添加 created_by / updated_by
--
-- 对应文档：
-- D06 §15 核心对象字段矩阵（审计字段要求 created_by / updated_by）
-- D13 §8 审计要求（所有关键动作必须有 actor_id）
-- D14 §6 反作弊 / 污染隔离（追溯贡献来源）
-- ============================================================

-- ===================== 1. axiqra_user =====================
ALTER TABLE axiqra_user
ADD COLUMN IF NOT EXISTS created_by BIGINT;

ALTER TABLE axiqra_user
ADD COLUMN IF NOT EXISTS updated_by BIGINT;

COMMENT ON COLUMN axiqra_user.created_by IS '创建人 user_id（注册用户时 created_by = 自身 id）';
COMMENT ON COLUMN axiqra_user.updated_by IS '最后修改人 user_id';

CREATE INDEX IF NOT EXISTS idx_user_created_by ON axiqra_user (created_by) WHERE created_by IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_updated_by ON axiqra_user (updated_by) WHERE updated_by IS NOT NULL;

-- ===================== 2. axiqra_workspace =====================
ALTER TABLE axiqra_workspace
ADD COLUMN IF NOT EXISTS created_by BIGINT;

ALTER TABLE axiqra_workspace
ADD COLUMN IF NOT EXISTS updated_by BIGINT;

COMMENT ON COLUMN axiqra_workspace.created_by IS '创建人 user_id（通常为 owner）';
COMMENT ON COLUMN axiqra_workspace.updated_by IS '最后修改人 user_id';

CREATE INDEX IF NOT EXISTS idx_workspace_created_by ON axiqra_workspace (created_by) WHERE created_by IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_workspace_updated_by ON axiqra_workspace (updated_by) WHERE updated_by IS NOT NULL;

-- ===================== 3. axiqra_engineering_trace =====================
ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS created_by BIGINT;

ALTER TABLE axiqra_engineering_trace
ADD COLUMN IF NOT EXISTS updated_by BIGINT;

COMMENT ON COLUMN axiqra_engineering_trace.created_by IS '创建人 user_id（提交轨迹的 AI 工具关联用户或直接用户）';
COMMENT ON COLUMN axiqra_engineering_trace.updated_by IS '最后修改人 user_id';

CREATE INDEX IF NOT EXISTS idx_trace_created_by ON axiqra_engineering_trace (created_by) WHERE created_by IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_trace_updated_by ON axiqra_engineering_trace (updated_by) WHERE updated_by IS NOT NULL;

-- ===================== 4. axiqra_solution =====================
ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS created_by BIGINT;

ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS updated_by BIGINT;

COMMENT ON COLUMN axiqra_solution.created_by IS '创建人 user_id（创建 Solution 的用户）';
COMMENT ON COLUMN axiqra_solution.updated_by IS '最后修改人 user_id（维护者或作者）';

CREATE INDEX IF NOT EXISTS idx_solution_created_by ON axiqra_solution (created_by) WHERE created_by IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_solution_updated_by ON axiqra_solution (updated_by) WHERE updated_by IS NOT NULL;

-- ===================== 回填现有数据 =====================
-- 现有记录的 created_by 回填为 author_id / owner_id（最接近真实创建人的字段）
-- updated_by 暂不填（无法追溯历史修改人）
UPDATE axiqra_engineering_trace SET created_by = author_id WHERE created_by IS NULL AND author_id IS NOT NULL;
UPDATE axiqra_solution SET created_by = author_id WHERE created_by IS NULL AND author_id IS NOT NULL;
UPDATE axiqra_workspace SET created_by = owner_id WHERE created_by IS NULL AND owner_id IS NOT NULL;
UPDATE axiqra_user SET created_by = id WHERE created_by IS NULL;
UPDATE axiqra_user SET updated_by = id WHERE updated_by IS NULL;

-- ===================== 验证 =====================
-- SELECT 'axiqra_user' AS tbl, COUNT(*) AS total, SUM(CASE WHEN created_by IS NOT NULL THEN 1 ELSE 0 END) AS with_created_by FROM axiqra_user
-- UNION ALL
-- SELECT 'axiqra_workspace', COUNT(*), SUM(CASE WHEN created_by IS NOT NULL THEN 1 ELSE 0 END) FROM axiqra_workspace
-- UNION ALL
-- SELECT 'axiqra_engineering_trace', COUNT(*), SUM(CASE WHEN created_by IS NOT NULL THEN 1 ELSE 0 END) FROM axiqra_engineering_trace
-- UNION ALL
-- SELECT 'axiqra_solution', COUNT(*), SUM(CASE WHEN created_by IS NOT NULL THEN 1 ELSE 0 END) FROM axiqra_solution;
