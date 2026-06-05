-- axiqra-project/axiqra-infra/docker/cockroachdb/init.sql
-- Axiqra 业务库建表脚本（S1 单机模式 CockroachDB）
-- 必须包含三字段：id、gmt_create、gmt_modified

CREATE DATABASE IF NOT EXISTS axiqra;
USE axiqra;

-- axq_user 用户表
CREATE TABLE axq_user (
    id                  BIGINT UNSIGNED  NOT NULL DEFAULT unique_rowid() COMMENT '用户ID，主键',
    gmt_create          TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '创建时间',
    gmt_modified        TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '更新时间',
    username            VARCHAR(64)      NOT NULL COMMENT '用户名',
    password_hash       VARCHAR(255)     NOT NULL COMMENT '密码（BCrypt加密）',
    email               VARCHAR(255)     NOT NULL COMMENT '邮箱',
    nickname            VARCHAR(100)     NULL COMMENT '昵称',
    tenant_id           BIGINT UNSIGNED  NULL COMMENT '租户ID（S3企业隔离用）',
    is_deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_username (username),
    UNIQUE INDEX idx_email (email),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_gmt_create (gmt_create)
);

-- axq_workspace 工作空间表
CREATE TABLE axq_workspace (
    id                  BIGINT UNSIGNED  NOT NULL DEFAULT unique_rowid() COMMENT '空间ID，主键',
    gmt_create          TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '创建时间',
    gmt_modified        TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '更新时间',
    owner_id            BIGINT UNSIGNED  NOT NULL COMMENT '空间所有者用户ID',
    workspace_name      VARCHAR(255)     NOT NULL COMMENT '空间名称',
    workspace_type      VARCHAR(20)      NOT NULL COMMENT '空间类型：personal/team/enterprise/public',
    tenant_id           BIGINT UNSIGNED  NULL COMMENT '租户ID',
    is_deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    INDEX idx_owner_id (owner_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_workspace_type (workspace_type)
);

-- axq_connect_session Connect 会话表
CREATE TABLE axq_connect_session (
    id                  BIGINT UNSIGNED  NOT NULL DEFAULT unique_rowid() COMMENT '会话ID，主键',
    gmt_create          TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '创建时间',
    gmt_modified        TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '更新时间',
    user_id             BIGINT UNSIGNED  NOT NULL COMMENT '创建用户ID',
    workspace_id        BIGINT UNSIGNED  NOT NULL COMMENT '目标空间ID',
    session_uuid        VARCHAR(64)      NOT NULL COMMENT '会话UUID（对外ID）',
    tool_type           VARCHAR(50)      NULL COMMENT '工具类型：cursor/codex/claude_code/gemini_cli/custom',
    status              VARCHAR(30)      NOT NULL DEFAULT 'created' COMMENT '会话状态',
    expires_at          TIMESTAMPTZ      NULL COMMENT '过期时间',
    last_seen_at        TIMESTAMPTZ      NULL COMMENT '最后访问时间',
    is_deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_session_uuid (session_uuid),
    INDEX idx_user_id (user_id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at)
);

-- axq_solution 方案表
CREATE TABLE axq_solution (
    id                  BIGINT UNSIGNED  NOT NULL DEFAULT unique_rowid() COMMENT '方案ID，主键',
    gmt_create          TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '创建时间',
    gmt_modified        TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '更新时间',
    workspace_id        BIGINT UNSIGNED  NOT NULL COMMENT '所属空间ID',
    author_id           BIGINT UNSIGNED  NOT NULL COMMENT '作者用户ID',
    trace_id            BIGINT UNSIGNED  NULL COMMENT '来源工程轨迹ID',
    solution_code       VARCHAR(64)      NOT NULL COMMENT '方案编码',
    title               VARCHAR(500)     NOT NULL COMMENT '方案标题',
    summary             TEXT             NULL COMMENT '方案摘要',
    verification_level  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '验证等级：0-L0~5-L5',
    risk_level          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '风险等级：0-R0~4-R4',
    status              VARCHAR(30)      NOT NULL DEFAULT 'draft' COMMENT '状态：draft/candidate/verified/stable/canonical/deprecated/quarantined',
    content_json        JSONB            NULL COMMENT 'AI执行视图内容',
    tech_stack          VARCHAR(255)     NULL COMMENT '技术栈',
    tags                VARCHAR(1000)    NULL COMMENT '标签，逗号分隔',
    visibility_scope    VARCHAR(20)      NOT NULL DEFAULT 'public' COMMENT '可见范围：private_only/project_only/team_only/enterprise_only/public',
    data_region         VARCHAR(20)      NULL COMMENT '数据区域',
    total_invocations   INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '总调用次数',
    success_count       INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '成功次数',
    partial_count       INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '部分成功次数',
    failed_count        INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '失败次数',
    tenant_id           BIGINT UNSIGNED  NULL COMMENT '租户ID',
    project_id          BIGINT UNSIGNED  NULL COMMENT '项目ID',
    schema_version      VARCHAR(20)      NOT NULL DEFAULT '1.0' COMMENT '数据结构版本',
    is_deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
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

-- axq_engineering_trace 工程轨迹表
CREATE TABLE axq_engineering_trace (
    id                  BIGINT UNSIGNED  NOT NULL DEFAULT unique_rowid() COMMENT '轨迹ID，主键',
    gmt_create          TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '创建时间',
    gmt_modified        TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '更新时间',
    workspace_id        BIGINT UNSIGNED  NOT NULL COMMENT '所属空间ID',
    author_id           BIGINT UNSIGNED  NOT NULL COMMENT '作者用户ID',
    solution_id         BIGINT UNSIGNED  NULL COMMENT '关联方案ID',
    trace_code          VARCHAR(64)      NOT NULL COMMENT '轨迹编码',
    summary             TEXT             NOT NULL COMMENT '任务摘要',
    worked              TEXT             NULL COMMENT '成功执行的路径和结果',
    failed              TEXT             NULL COMMENT '失败路径和误判',
    forward_path        TEXT             NULL COMMENT '正向执行路径（JSON）',
    reverse_path        TEXT             NULL COMMENT '反向路径',
    decision_path       TEXT             NULL COMMENT '关键决策点（JSON）',
    rollback_path       TEXT             NULL COMMENT '回滚路径',
    evidence_refs       JSONB            NULL COMMENT '证据引用',
    security_notes      TEXT             NULL COMMENT '安全注意事项',
    user_confirmation   VARCHAR(30)      NOT NULL DEFAULT 'pending' COMMENT '用户确认状态',
    idem_key            VARCHAR(255)     NULL COMMENT '幂等键（防重复提交）',
    schema_version      VARCHAR(20)      NOT NULL DEFAULT '1.0' COMMENT '轨迹包版本',
    index_status        VARCHAR(20)      NOT NULL DEFAULT 'pending' COMMENT '索引状态',
    status              VARCHAR(30)      NOT NULL DEFAULT 'draft' COMMENT '轨迹状态',
    review_status       VARCHAR(30)      NOT NULL DEFAULT 'pending' COMMENT '审核状态',
    review_id           BIGINT UNSIGNED  NULL COMMENT '审核记录ID',
    tenant_id           BIGINT UNSIGNED  NULL COMMENT '租户ID',
    is_deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_idem_key (idem_key),
    UNIQUE INDEX idx_trace_code (trace_code),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_review_status (review_status)
);

-- axq_invocation 调用记录表
CREATE TABLE axq_invocation (
    id                  BIGINT UNSIGNED  NOT NULL DEFAULT unique_rowid() COMMENT '调用ID，主键',
    gmt_create          TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '创建时间',
    gmt_modified        TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '更新时间',
    user_id             BIGINT UNSIGNED  NOT NULL COMMENT '发起用户ID',
    solution_id         BIGINT UNSIGNED  NOT NULL COMMENT '调用的方案ID',
    workspace_id        BIGINT UNSIGNED  NOT NULL COMMENT '调用时所在空间ID',
    invocation_code     VARCHAR(64)      NOT NULL COMMENT '调用编码',
    tool_type           VARCHAR(50)      NOT NULL COMMENT '工具类型',
    query_hash          VARCHAR(64)      NULL COMMENT '查询哈希（去重用）',
    risk_level          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次调用风险等级',
    required_confirmation TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否需要用户确认：0-否，1-是',
    confirmation_obtained TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否已获确认：0-否，1-是',
    result_type         VARCHAR(20)      NULL COMMENT '结果类型：worked/failed/partial/not_applicable',
    tenant_id           BIGINT UNSIGNED  NULL COMMENT '租户ID',
    is_deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_invocation_code (invocation_code),
    INDEX idx_user_id (user_id),
    INDEX idx_solution_id (solution_id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_result_type (result_type)
);

-- axq_feedback 反馈表
CREATE TABLE axq_feedback (
    id                  BIGINT UNSIGNED  NOT NULL DEFAULT unique_rowid() COMMENT '反馈ID，主键',
    gmt_create          TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '创建时间',
    gmt_modified        TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '更新时间',
    invocation_id       BIGINT UNSIGNED  NOT NULL COMMENT '关联调用记录ID',
    user_id             BIGINT UNSIGNED  NOT NULL COMMENT '反馈用户ID',
    feedback_type       VARCHAR(30)      NOT NULL COMMENT '反馈类型：worked/failed/partial/not_applicable',
    feedback_content    TEXT             NULL COMMENT '反馈内容',
    evidence_refs       JSONB            NULL COMMENT '证据引用',
    boundary_notes      TEXT             NULL COMMENT '适用范围边界说明',
    tenant_id           BIGINT UNSIGNED  NULL COMMENT '租户ID',
    is_deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    INDEX idx_invocation_id (invocation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_feedback_type (feedback_type)
);

-- axq_review 审核记录表
CREATE TABLE axq_review (
    id                  BIGINT UNSIGNED  NOT NULL DEFAULT unique_rowid() COMMENT '审核ID，主键',
    gmt_create          TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '创建时间',
    gmt_modified        TIMESTAMPTZ      NOT NULL DEFAULT now() COMMENT '更新时间',
    trace_id            BIGINT UNSIGNED  NOT NULL COMMENT '被审核的轨迹ID',
    reviewer_id         BIGINT UNSIGNED  NOT NULL COMMENT '审核人用户ID',
    review_type         VARCHAR(30)      NOT NULL COMMENT '审核类型：ai_first/human/certified/arbitration',
    risk_level          TINYINT UNSIGNED NOT NULL COMMENT '审核时判定风险等级',
    decision            VARCHAR(30)      NOT NULL COMMENT '审核决策：approve/approve_with_edits/request_more_info/reject/quarantine/downgrade/deprecate',
    decision_reason     VARCHAR(500)    NULL COMMENT '审核理由',
    reason_code         VARCHAR(50)      NULL COMMENT '原因码',
    tenant_id           BIGINT UNSIGNED  NULL COMMENT '租户ID',
    is_deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    INDEX idx_trace_id (trace_id),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_decision (decision)
);

-- 授予应用账号权限
GRANT ALL ON DATABASE axiqra TO root;
GRANT ALL ON TABLE axq_user, axq_workspace, axq_connect_session,
                    axq_solution, axq_engineering_trace, axq_invocation,
                    axq_feedback, axq_review TO root;
