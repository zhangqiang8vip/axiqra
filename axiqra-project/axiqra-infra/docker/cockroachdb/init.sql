-- axiqra-project/axiqra-infra/docker/cockroachdb/init.sql
-- Axiqra 业务库建表脚本（S1 单机模式 CockroachDB）
-- 表前缀统一为 axiqra_（与 axiqra- 项目名一致）

CREATE DATABASE IF NOT EXISTS axiqra;
USE axiqra;

-- ===================== 用户 =====================
CREATE TABLE IF NOT EXISTS axiqra_user (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NULL,
    avatar VARCHAR(500) NULL,
    tenant_id BIGINT NULL,
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_username (username),
    UNIQUE INDEX idx_email (email),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_gmt_create (gmt_create)
);

-- ===================== 工作空间 =====================
CREATE TABLE IF NOT EXISTS axiqra_workspace (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    owner_id BIGINT NOT NULL,
    workspace_name VARCHAR(255) NOT NULL,
    workspace_type VARCHAR(20) NOT NULL DEFAULT 'personal',
    tenant_id BIGINT NULL,
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_owner_id (owner_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_workspace_type (workspace_type),
    INDEX idx_gmt_create (gmt_create),
    UNIQUE INDEX idx_owner_workspace_name (owner_id, workspace_name) WHERE is_deleted = FALSE
);

-- ===================== 空间成员关系 =====================
CREATE TABLE IF NOT EXISTS axiqra_membership (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    user_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'member',
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_id BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_user_workspace_active (user_id, workspace_id) WHERE is_deleted = FALSE,
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_user_id (user_id),
    INDEX idx_tenant_id (tenant_id)
);

-- ===================== 工程项目 =====================
CREATE TABLE IF NOT EXISTS axiqra_project (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    workspace_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    tech_stack VARCHAR(255) NULL,
    environment VARCHAR(255) NULL,
    owner_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_owner_id (owner_id),
    INDEX idx_status (status)
);

-- ===================== Engineering Trace Package =====================
CREATE TABLE IF NOT EXISTS axiqra_engineering_trace (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    workspace_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    author_id BIGINT NOT NULL,
    tool_type VARCHAR(50) NULL,
    task_goal TEXT NOT NULL,
    context_snapshot JSONB NULL,
    forward_steps JSONB NULL,
    reverse_path JSONB NULL,
    decisions JSONB NULL,
    rollback_path JSONB NULL,
    outcome VARCHAR(32) NOT NULL DEFAULT 'draft',
    risk_level VARCHAR(32) NOT NULL DEFAULT 'R0',
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    user_confirmation VARCHAR(32) NOT NULL DEFAULT 'pending',
    idempotency_key VARCHAR(255) NULL,
    visibility_scope VARCHAR(32) NOT NULL DEFAULT 'private',
    index_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    review_id BIGINT NULL,
    solution_id BIGINT NULL,
    evolution_suggestion VARCHAR(32) NULL,
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_idempotency_key (idempotency_key),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_project_id (project_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_risk_level (risk_level),
    INDEX idx_outcome (outcome)
);

-- ===================== Trace 证据引用 =====================
-- 首次创建时 gmt_modified 为 NULL，请执行以下迁移后再改为 NOT NULL：
--   UPDATE axiqra_trace_evidence_ref
--   SET gmt_modified = COALESCE(gmt_create, now())
--   WHERE gmt_modified IS NULL;
--   ALTER TABLE axiqra_trace_evidence_ref ALTER COLUMN gmt_modified SET NOT NULL;
--   ALTER TABLE axiqra_trace_evidence_ref ALTER COLUMN gmt_modified SET DEFAULT now();
CREATE TABLE IF NOT EXISTS axiqra_trace_evidence_ref (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    trace_id BIGINT NOT NULL,
    uri VARCHAR(2000) NOT NULL,
    hash VARCHAR(128) NULL,
    type VARCHAR(32) NOT NULL,
    size_bytes BIGINT NULL,
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_id BIGINT NULL,
    PRIMARY KEY (id),
    INDEX idx_trace_id (trace_id),
    INDEX idx_tenant_id (tenant_id)
);

-- ===================== Project Case =====================
CREATE TABLE IF NOT EXISTS axiqra_project_case (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    trace_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    author_id BIGINT NOT NULL,
    authorization_id BIGINT NULL,
    visibility_scope VARCHAR(32) NOT NULL DEFAULT 'private',
    license_scope VARCHAR(32) NULL,
    redaction_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    review_id BIGINT NULL,
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_trace_id (trace_id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_visibility_scope (visibility_scope)
);

-- ===================== Public Case =====================
CREATE TABLE IF NOT EXISTS axiqra_public_case (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    source_case_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    redaction_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    review_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'candidate',
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_source_case (source_case_id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status)
);

-- ===================== Solution =====================
CREATE TABLE IF NOT EXISTS axiqra_solution (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    author_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    solution_code VARCHAR(64) NOT NULL,
    title VARCHAR(500) NOT NULL,
    domain VARCHAR(255) NULL,
    tech_stack VARCHAR(255) NULL,
    verification_level SMALLINT NOT NULL DEFAULT 0,
    risk_level SMALLINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'draft',
    visibility_scope VARCHAR(20) NOT NULL DEFAULT 'public',
    license_scope VARCHAR(32) NULL,
    tenant_id BIGINT NULL,
    source_case_id BIGINT NULL,
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_solution_code (solution_code),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_verification_level (verification_level),
    INDEX idx_risk_level (risk_level),
    INDEX idx_visibility_scope (visibility_scope),
    INDEX idx_tenant_id (tenant_id)
);

-- ===================== Solution 版本 =====================
CREATE TABLE IF NOT EXISTS axiqra_solution_version (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    solution_id BIGINT NOT NULL,
    version_number INT NOT NULL DEFAULT 1,
    steps JSONB NULL,
    applicable_context TEXT NULL,
    non_applicable_context TEXT NULL,
    evidence JSONB NULL,
    risk TEXT NULL,
    rollback TEXT NULL,
    is_active SMALLINT NOT NULL DEFAULT 0,
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_solution_version (solution_id, version_number),
    INDEX idx_solution_id (solution_id)
);

-- ===================== Invocation 调用记录 =====================
CREATE TABLE IF NOT EXISTS axiqra_invocation (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    request_id VARCHAR(96) NOT NULL,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    invocation_code VARCHAR(64) NOT NULL,
    tool_type VARCHAR(50) NOT NULL,
    query_hash VARCHAR(64) NULL,
    risk_level SMALLINT NOT NULL DEFAULT 0,
    required_confirmation SMALLINT NOT NULL DEFAULT 0,
    confirmation_obtained SMALLINT NOT NULL DEFAULT 0,
    result_type VARCHAR(20) NULL,
    tenant_id BIGINT NULL,
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_request_id (request_id),
    UNIQUE INDEX idx_invocation_code (invocation_code),
    INDEX idx_user_id (user_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_result_type (result_type),
    INDEX idx_gmt_create (gmt_create)
);

-- ===================== Feedback =====================
CREATE TABLE IF NOT EXISTS axiqra_feedback (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    invocation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    feedback_type VARCHAR(30) NOT NULL,
    feedback_content TEXT NULL,
    evidence_refs JSONB NULL,
    context_delta TEXT NULL,
    boundary_notes TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'accepted',
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_id BIGINT NULL,
    PRIMARY KEY (id),
    INDEX idx_invocation_id (invocation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_feedback_type (feedback_type),
    INDEX idx_tenant_id (tenant_id)
);

-- ===================== Review 审核任务 =====================
CREATE TABLE IF NOT EXISTS axiqra_review (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    object_type VARCHAR(32) NOT NULL,
    object_id BIGINT NOT NULL,
    queue VARCHAR(32) NOT NULL DEFAULT 'human',
    reviewer_id BIGINT NULL,
    risk_level VARCHAR(32) NOT NULL DEFAULT 'R0',
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_id BIGINT NULL,
    PRIMARY KEY (id),
    INDEX idx_object (object_type, object_id),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_queue (queue),
    INDEX idx_status (status),
    INDEX idx_tenant_id (tenant_id)
);

-- ===================== Authorization 授权快照 =====================
CREATE TABLE IF NOT EXISTS axiqra_authorization (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    owner_id BIGINT NOT NULL,
    scope VARCHAR(32) NOT NULL,
    license_scope VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    revoked_at TIMESTAMPTZ NULL,
    tenant_id BIGINT NULL,
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_owner_id (owner_id),
    INDEX idx_status (status),
    INDEX idx_tenant_id (tenant_id)
);

-- ===================== Contribution Ledger 贡献账本 =====================
CREATE TABLE IF NOT EXISTS axiqra_contribution_ledger (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    object_type VARCHAR(32) NOT NULL,
    object_id BIGINT NOT NULL,
    points INT NOT NULL DEFAULT 0,
    evidence_refs JSONB NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'recorded',
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_id BIGINT NULL,
    PRIMARY KEY (id),
    INDEX idx_actor_id (actor_id),
    INDEX idx_event_type (event_type),
    INDEX idx_object (object_type, object_id),
    INDEX idx_status (status),
    INDEX idx_tenant_id (tenant_id)
);

-- ===================== Candidate Seed =====================
CREATE TABLE IF NOT EXISTS axiqra_candidate_seed (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    workspace_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    query_hash VARCHAR(64) NOT NULL,
    task_goal TEXT NOT NULL,
    tech_stack VARCHAR(255) NULL,
    coverage_gap TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'candidate',
    assignee_id BIGINT NULL,
    solution_id BIGINT NULL,
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_id BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_query_hash (query_hash),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_tenant_id (tenant_id)
);

-- ===================== 工具模型归因 =====================
-- 首次创建时 gmt_modified 为 NULL，请执行以下迁移后再改为 NOT NULL：
--   UPDATE axiqra_tool_model_attribution
--   SET gmt_modified = COALESCE(gmt_create, now())
--   WHERE gmt_modified IS NULL;
--   ALTER TABLE axiqra_tool_model_attribution ALTER COLUMN gmt_modified SET NOT NULL;
--   ALTER TABLE axiqra_tool_model_attribution ALTER COLUMN gmt_modified SET DEFAULT now();
CREATE TABLE IF NOT EXISTS axiqra_tool_model_attribution (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    solution_id BIGINT NOT NULL,
    solution_version_id BIGINT NULL,
    attribution_source VARCHAR(32) NOT NULL,
    tool_type VARCHAR(50) NOT NULL,
    tool_name VARCHAR(160) NOT NULL,
    tool_vendor VARCHAR(160) NULL,
    tool_version VARCHAR(64) NOT NULL DEFAULT 'unknown',
    client_channel VARCHAR(32) NOT NULL,
    reported_model_provider VARCHAR(160) NOT NULL,
    reported_model_name VARCHAR(160) NOT NULL,
    reported_model_version VARCHAR(64) NULL,
    reported_model_source VARCHAR(32) NOT NULL,
    reported_model_confidence VARCHAR(32) NOT NULL,
    model_reported_at TIMESTAMPTZ NULL,
    missing_reason VARCHAR(255) NULL,
    request_id VARCHAR(96) NOT NULL,
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_id BIGINT NULL,
    PRIMARY KEY (id),
    INDEX idx_solution_id (solution_id),
    INDEX idx_tool_name (tool_name),
    INDEX idx_reported_model (reported_model_name),
    INDEX idx_request_id (request_id),
    INDEX idx_tenant_id (tenant_id)
);

-- ===================== 工具模型日粒度表现统计 =====================
CREATE TABLE IF NOT EXISTS axiqra_tool_model_performance_daily (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    stat_date DATE NOT NULL,
    solution_id BIGINT NOT NULL,
    tool_name VARCHAR(160) NOT NULL,
    reported_model_name VARCHAR(160) NOT NULL,
    domain VARCHAR(255) NULL,
    tech_stack VARCHAR(255) NULL,
    risk_level VARCHAR(32) NULL,
    eligible_count_7d INT NOT NULL DEFAULT 0,
    worked_count_7d INT NOT NULL DEFAULT 0,
    partial_count_7d INT NOT NULL DEFAULT 0,
    failed_count_7d INT NOT NULL DEFAULT 0,
    not_applicable_count_7d INT NOT NULL DEFAULT 0,
    success_rate_7d DECIMAL(5,4) NULL,
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_daily_stat (stat_date, solution_id, tool_name, reported_model_name),
    INDEX idx_date (stat_date),
    INDEX idx_tool_model (tool_name, reported_model_name)
);

-- ===================== 工具模型排行榜快照 =====================
CREATE TABLE IF NOT EXISTS axiqra_tool_model_leaderboard_snapshot (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    window_start DATE NOT NULL,
    window_end DATE NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    scope_id BIGINT NULL,
    tool_name VARCHAR(160) NOT NULL,
    reported_model_name VARCHAR(160) NOT NULL,
    domain VARCHAR(255) NULL,
    tech_stack VARCHAR(255) NULL,
    rank INT NOT NULL,
    success_rate_7d DECIMAL(5,4) NOT NULL,
    sample_size INT NOT NULL,
    rank_score DECIMAL(10,4) NULL,
    is_deleted BOOL NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_window (window_start, window_end),
    INDEX idx_scope (scope_type, scope_id),
    INDEX idx_tool_model_rank (tool_name, reported_model_name, rank)
);

-- 授权（使用 root 用户，CockroachDB 默认管理员）
GRANT ALL ON DATABASE axiqra TO root;
GRANT ALL ON TABLE axiqra_user, axiqra_workspace, axiqra_membership,
    axiqra_project, axiqra_engineering_trace, axiqra_trace_evidence_ref,
    axiqra_project_case, axiqra_public_case, axiqra_solution,
    axiqra_solution_version, axiqra_invocation, axiqra_feedback,
    axiqra_review, axiqra_authorization, axiqra_contribution_ledger,
    axiqra_candidate_seed, axiqra_tool_model_attribution,
    axiqra_tool_model_performance_daily,
    axiqra_tool_model_leaderboard_snapshot TO root;
