-- axiqra-project/axiqra-infra/docker/cockroachdb/init.sql
-- Axiqra 业务库建表脚本（S1 单机模式 CockroachDB）

CREATE DATABASE IF NOT EXISTS axiqra;
USE axiqra;

CREATE TABLE IF NOT EXISTS axq_user (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NULL,
    tenant_id BIGINT NULL,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_username (username),
    UNIQUE INDEX idx_email (email),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_gmt_create (gmt_create)
);

CREATE TABLE IF NOT EXISTS axq_workspace (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    owner_id BIGINT NOT NULL,
    workspace_name VARCHAR(255) NOT NULL,
    workspace_type VARCHAR(20) NOT NULL,
    tenant_id BIGINT NULL,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_owner_id (owner_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_workspace_type (workspace_type)
);

CREATE TABLE IF NOT EXISTS axq_connect_session (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    user_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    session_uuid VARCHAR(64) NOT NULL,
    tool_type VARCHAR(50) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'created',
    expires_at TIMESTAMPTZ NULL,
    last_seen_at TIMESTAMPTZ NULL,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_session_uuid (session_uuid),
    INDEX idx_user_id (user_id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at)
);

CREATE TABLE IF NOT EXISTS axq_solution (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    workspace_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    trace_id BIGINT NULL,
    solution_code VARCHAR(64) NOT NULL,
    title VARCHAR(500) NOT NULL,
    summary TEXT NULL,
    verification_level SMALLINT NOT NULL DEFAULT 0,
    risk_level SMALLINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'draft',
    content_json JSONB NULL,
    tech_stack VARCHAR(255) NULL,
    tags VARCHAR(1000) NULL,
    visibility_scope VARCHAR(20) NOT NULL DEFAULT 'public',
    data_region VARCHAR(20) NULL,
    total_invocations INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    partial_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    tenant_id BIGINT NULL,
    project_id BIGINT NULL,
    schema_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_solution_code (solution_code),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_author_id (author_id),
    INDEX idx_verification_level (verification_level),
    INDEX idx_risk_level (risk_level),
    INDEX idx_status (status),
    INDEX idx_visibility_scope (visibility_scope),
    INDEX idx_tenant_id (tenant_id)
);

CREATE TABLE IF NOT EXISTS axq_engineering_trace (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    workspace_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    solution_id BIGINT NULL,
    trace_code VARCHAR(64) NOT NULL,
    summary TEXT NOT NULL,
    worked TEXT NULL,
    failed TEXT NULL,
    forward_path TEXT NULL,
    reverse_path TEXT NULL,
    decision_path TEXT NULL,
    rollback_path TEXT NULL,
    evidence_refs JSONB NULL,
    security_notes TEXT NULL,
    user_confirmation VARCHAR(30) NOT NULL DEFAULT 'pending',
    idem_key VARCHAR(255) NULL,
    schema_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    index_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    status VARCHAR(30) NOT NULL DEFAULT 'draft',
    review_status VARCHAR(30) NOT NULL DEFAULT 'pending',
    review_id BIGINT NULL,
    tenant_id BIGINT NULL,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_idem_key (idem_key),
    UNIQUE INDEX idx_trace_code (trace_code),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_review_status (review_status)
);

CREATE TABLE IF NOT EXISTS axq_invocation (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    user_id BIGINT NOT NULL,
    solution_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    invocation_code VARCHAR(64) NOT NULL,
    tool_type VARCHAR(50) NOT NULL,
    query_hash VARCHAR(64) NULL,
    risk_level SMALLINT NOT NULL DEFAULT 0,
    required_confirmation SMALLINT NOT NULL DEFAULT 0,
    confirmation_obtained SMALLINT NOT NULL DEFAULT 0,
    result_type VARCHAR(20) NULL,
    tenant_id BIGINT NULL,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_invocation_code (invocation_code),
    INDEX idx_user_id (user_id),
    INDEX idx_solution_id (solution_id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_result_type (result_type)
);

CREATE TABLE IF NOT EXISTS axq_feedback (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    invocation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    feedback_type VARCHAR(30) NOT NULL,
    feedback_content TEXT NULL,
    evidence_refs JSONB NULL,
    boundary_notes TEXT NULL,
    tenant_id BIGINT NULL,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_invocation_id (invocation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_feedback_type (feedback_type)
);

CREATE TABLE IF NOT EXISTS axq_review (
    id BIGINT NOT NULL DEFAULT unique_rowid(),
    gmt_create TIMESTAMPTZ NOT NULL DEFAULT now(),
    gmt_modified TIMESTAMPTZ NOT NULL DEFAULT now(),
    trace_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    review_type VARCHAR(30) NOT NULL,
    risk_level SMALLINT NOT NULL,
    decision VARCHAR(30) NOT NULL,
    decision_reason VARCHAR(500) NULL,
    reason_code VARCHAR(50) NULL,
    tenant_id BIGINT NULL,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_trace_id (trace_id),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_decision (decision)
);

GRANT ALL ON DATABASE axiqra TO root;
GRANT ALL ON TABLE axq_user, axq_workspace, axq_connect_session,
                    axq_solution, axq_engineering_trace, axq_invocation,
                    axq_feedback, axq_review TO root;
