-- ============================================================
-- Axiqra S1: Solution 扩展字段
-- ============================================================
-- 补充 axiqra_solution 表中实体定义但 V3 migration 缺失的字段
-- 同步 entity 字段,避免 MyBatis SELECT * 失败
-- ============================================================

-- 1. 添加 Solution 扩展字段
ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS error_signature TEXT;

ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS environment VARCHAR(500);

ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS problem_type VARCHAR(100);

ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS evidence_count INT NOT NULL DEFAULT 0;

ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS failure_paths TEXT;

ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS applicability TEXT;

ALTER TABLE axiqra_solution
ADD COLUMN IF NOT EXISTS inapplicability TEXT;

-- 2. 注释说明
COMMENT ON COLUMN axiqra_solution.error_signature IS '错误签名: 解决方案对应的错误模式或异常哈希';
COMMENT ON COLUMN axiqra_solution.environment IS '运行环境: 部署语言/框架/版本';
COMMENT ON COLUMN axiqra_solution.problem_type IS '问题类型: 分类标签';
COMMENT ON COLUMN axiqra_solution.evidence_count IS '证据计数: Trace/Case 引用数';
COMMENT ON COLUMN axiqra_solution.failure_paths IS '失败路径: 已知会失败的场景';
COMMENT ON COLUMN axiqra_solution.applicability IS '适用条件: 何时使用该方案';
COMMENT ON COLUMN axiqra_solution.inapplicability IS '不适用边界: 何时不该使用该方案';

-- 3. 索引加速检索
CREATE INDEX IF NOT EXISTS idx_solution_error_signature
ON axiqra_solution (error_signature)
WHERE error_signature IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_solution_problem_type
ON axiqra_solution (problem_type)
WHERE problem_type IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_solution_environment
ON axiqra_solution (environment)
WHERE environment IS NOT NULL;