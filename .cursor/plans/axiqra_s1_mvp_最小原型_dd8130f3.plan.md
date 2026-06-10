# Axiqra S1 MVP 最小原型计划

更新时间：2026-06-10（Round 架构版重建）

---

## 整体进度

| 阶段 | 状态 | PR |
|------|------|-----|
| R1 日志安全审计框架 | 已完成，合入 main | #19 |
| R2 通用验证 RBAC 框架 | 已完成，合入 main | #20 |
| R3 Auth + Workspace + Nav | 已完成，合入 main | #24 |
| R4 Quota + Connect + MCP/CLI | 已完成，合入 main | #25 |
| R5 Search + Solution | 已完成（待合入 main） | #27 |
| R6 Trace + Project Case + Public Case | 待开发 | — |
| R7 Feedback + Review + Contribution | 待开发 | — |
| R8 前端 + 测试 + 验收 | 待开发 | — |

---

## R1: 日志安全审计框架

**状态：已完成，合入 main（PR #19）**

- 审计接口（`AuditPort`）与事件类型（Authorization、Invocation、PolicyDecision、Quota、RateLimit、ReviewDecision、ToolModelAttribution）
- 审计写入 MySQL `axiqra_audit` 表，带审计人、操作类型、资源类型、资源标识、决策结果、触发规则 ID
- ABAC 策略引擎（含规则 ID 链与违规记录）
- RLS（行级安全）设计说明

---

## R2: 通用验证 RBAC 框架

**状态：已完成，合入 main（PR #20）**

- Scope 模型：`resource:action` 格式（如 `connect:read`、`search:admin`）
- `WorkspaceType`：`personal / team / enterprise`
- `VisibilityScope`：`private / workspace / enterprise / public`
- RBAC 鉴权守卫（含 AuthorizingManager 集成）
- 全局鉴权过滤器

---

## R3: Auth + Workspace + Nav

**状态：已完成，合入 main（PR #24）**

- 用户注册 / 登录（密码 + 盐值 SHA-256）
- Workspace 创建、查询、更新、删除
- 成员管理（邀请、角色变更、移除）
- 软删除与乐观锁
- 全局鉴权过滤器集成

---

## R4: Quota + Connect + MCP/CLI

**状态：已完成，合入 main（PR #25）**

- **Quota**：每日配额控制，超限返回 `429`，明确错误语义
- **RateLimit**：分钟级限流，`retry_after` 响应头
- **Connect**：会话创建、doctor 检测、状态流转与持久化适配
- **MCP/CLI**：S1 核心闭环样例、协议实现与测试样例

---

## R5: Search + Solution

**状态：已完成，待合入 main（PR #27）**

### 功能交付

#### Search（`POST /search/before-act`）

- RBAC 鉴权（`search:read` scope）
- 可见范围解析（个人 / Workspace / Enterprise / Public）
- 多条件过滤（domain、tech_stack、status、verification_level、risk_level、labels）
- 结果打分与排序（相关性、验证等级、发布时间综合）
- **空结果时创建 Candidate Seed**：当结果为空且 `includeCandidateSeed=true` 时，在调用方事务内安全创建种子记录，支持重复插入的幂等重试
- 分页（limit 1-50，默认 20）

#### Solution 详情（`GET /solutions/{id}`）

- 权限校验（visibility_scope + status + risk_level + verification_level）
- **作者例外**：方案作者可查看自己的 DRAFT / HIGH_RISK / UNVERIFIED 方案
- 版本历史映射（SolutionVersionVO）
- 反馈统计聚合（WORKED / PARTIAL / FAILED / NOT_APPLICABLE）
- CandidateSeed 关联

#### 核心实体 / DTO / VO

| 类型 | 文件 |
|------|------|
| DTO | `SearchRequest`、`ConnectSessionCreateRequest` |
| VO | `SearchResponseVO`、`SearchResultItemVO`、`SolutionDetailVO`、`SolutionVersionVO`、`SolutionFeedbackStatsVO`、`CandidateSeedVO` |
| Enum | `FeedbackType`（WORKED / PARTIAL / FAILED / NOT_APPLICABLE） |

#### 数据库迁移

- `migration-003`：candidate-seed 唯一索引改为 `(workspace_id, query_hash) WHERE is_deleted = FALSE`，解决并发 + 软删除兼容性问题

### R5 代码提交记录

```
9e9f9576 test: cover remaining error scenarios in SolutionServiceImplTest
24ab6f4e fix: allow authors to inspect restricted solutions
bb5a33a6 fix: address solution review feedback
f253e685 fix: align candidate seed transaction handling
6aa3592d fix: harden search candidate seed persistence
5a3be079 fix: harden connect and workspace runtime guards
```

---

## 开发轮次说明（先地基后盖房，一层一层推进）

> **核心理念**：像盖房子一样，先打地基（Round 1-2），再盖第一层（Round 3），再盖第二层（Round 4），依此类推。地基不稳，上层必塌。

### Round 架构说明

| Round | 名称 | 核心目标 | 前置依赖 |
|-------|------|---------|---------|
| **Round 1** | 地基：日志+安全+审计框架 | 所有后续代码的公共基础设施 | 无（从零开始） |
| **Round 2** | 地基：Common 层 | 所有业务模块依赖的 DO/DTO/VO/枚举/错误码 | Round 1 完成 |
| **Round 3** | 第一层：Auth + Workspace | 用户能登录，能看到自己的空间 | Round 2 完成 |
| **Round 4** | 第二层：Quota + Connect + MCP/CLI | 搜索有配额，AI 工具能接入 | Round 3 完成 |
| **Round 5** | 第三层：Search + Solution | 核心搜索闭环和方案详情 | Round 4 完成 |
| **Round 6** | 第四层：Trace + Project Case + Public Case | 工程轨迹和案例管理闭环 | Round 5 完成 |
| **Round 7** | 第五层：Invocation + Feedback + Review + Contribution | 调用反馈和治理闭环 | Round 6 完成 |
| **Round 8** | 第六层：前端 + 测试 + 验收 | 完整系统交付 | Round 7 完成 |

### 依赖关系图

```
Round 1（地基：日志/安全/审计）
  │
  ├── R1A: logback 增强
  ├── R1B: TraceIdFilter + 全局异常
  ├── R1C: API 签名 Filter
  ├── R1D: 加密/哈希/脱敏工具
  └── R1E: PostgreSQL 审计库 8 表 + AuditPort
              └─────────────────────┐
─────────────────────────────────┘
                                      ▼
Round 2（地基：Common 层）◄──────── 依赖 Round 1
  ├── R2A: 20 张表 DO Entity + 枚举
  ├── R2B: 错误码 + 统一异常
  ├── R2C: 基础 DTO/VO + port 接口
  └── R2D: Sa-Token 登录/登出基础
  │
  ▼
Round 3（第一层：Auth + Workspace）◄──── 依赖 Round 2
  ├── R3A: Auth 模块（登录/注册/Profile/RBAC）
  ├── R3B: Workspace 模块（CRUD + 成员管理）
  └── R3C: 累加式导航 API
  │
  ▼
Round 4（第二层：Quota + Connect + MCP/CLI）◄── 依赖 Round 3
  ├── R4A: Quota（Redis 每日配额）+ RateLimit（每分钟限流）
  ├── R4B: Connect 模块（会话创建 + doctor 检测）
  └── R4C: MCP Server + CLI 工具
  │
  ▼
Round 5（第三层：Search + Solution）◄────────── 依赖 Round 4
  ├── R5A: Search 模块（权限预过滤 + 多路召回 + Candidate Seed）
  └── R5B: Solution 模块（详情/版本 + L0-L5/R0-R4 状态机）
  │
  ▼
Round 6（第四层：Trace + Case）◄──────────────── 依赖 Round 5
  ├── R6A: Trace 模块（提交 + 证据 + 用户确认）
  ├── R6B: Project Case 模块（私有 Case + license_scope）
  └── R6C: Public Case 模块（脱敏 + 授权 + 发布）
  │
  ▼
Round 7（第五层：Feedback + Review + Contribution）◄ 依赖 Round 6
  ├── R7A: Invocation + Feedback 模块
  ├── R7B: Review 模块（R0-R4 + 申诉）
  ├── R7C: Contribution + Credential 模块
  └── R7D: 工具模型归因 + 排行榜
  │
  ▼
Round 8（第六层：前端 + 测试 + 验收）◄─────────── 依赖 Round 7
  ├── R8A: Vue 3 前端（16 页面，路由/菜单/权限码全部从后端获取）
  ├── R8B: 测试体系（单元/集成/E2E + 覆盖率）
  └── R8C: S1 Gate 验收（5 Gate + 13 红线验证）
```

### 前端配置化原则

> **所有前端配置（路由、菜单、权限码、页面元数据）必须从后端 API 获取，不写死值。**

| 前端内容 | 后端 API 来源 | 说明 |
|---------|-------------|------|
| 菜单项 | `GET /api/auth/nav` | 累加式，返回用户可见菜单 |
| 权限码 | `GET /api/auth/permissions` | RBAC 权限矩阵 |
| 页面路由 | `GET /api/config/routes` | 动态路由注册 |
| 页面元数据 | `GET /api/config/pages` | 页面标题/图标/权限要求 |
| 枚举值 | `GET /api/config/enums` | RiskLevel/VerificationLevel 等 |
| 搜索字段 | `GET /api/config/search-fields` | 搜索配置化 |

### 完整 Round 任务清单

```yaml
todos:
  # ===================== Round 1: 地基——日志 + 安全 + 审计框架 =====================
  # 前置：Step 1-2 已完成（Docker 脚手架就绪）
  # 目标：所有后续代码依赖这套日志规范，安全 Filter、审计基础设施
  - id: r1a-logback
    content: "Round 1A: logback-spring.xml 增强（MDC traceId+userId + async wrapper + archive 目录 + info/warn/error 分文件滚动）"
    wbs: [WBS-001]
    req: []
    status: completed
    commit: "[PR #19]"
  - id: r1b-trace-id-filter
    content: "Round 1B: TraceIdFilter（请求入口生成 traceId 入 MDC）+ 全局异常处理（统一返回格式 + error 日志 + GlobalExceptionHandler）"
    wbs: [WBS-001]
    req: []
    status: completed
    commit: "[PR #19]"
  - id: r1c-api-signature
    content: "Round 1C: API 签名认证 Filter（HMAC-SHA256 + timestamp + nonce 防重放，公开接口白名单跳过）"
    wbs: [WBS-004, WBS-005]
    req: [REQ-PER-003]
    status: completed
    commit: "[PR #19]"
  - id: r1d-crypto-utils
    content: "Round 1D: 加密工具（AES-256-GCM AesEncryptUtil）+ 密码哈希工具（BCrypt PasswordHashUtil）+ 脱敏工具（DataMaskingUtil api_key/token/password → ****）"
    wbs: [WBS-004, WBS-005]
    req: [REQ-PER-003]
    status: completed
    commit: "[PR #19]"
  - id: r1e-audit-schema
    content: "Round 1E: PostgreSQL 审计库 8 张表（init.sql）+ AuditLog Entity + AuditPort 接口 + AuditAdapter 实现"
    wbs: [WBS-003]
    req: [REQ-AUD-001, REQ-AUD-002]
    status: completed
    commit: "[PR #19]"

  # ===================== Round 2: 地基——Common 层 =====================
  # 前置：Round 1 完成
  # 目标：所有业务模块依赖的 DO/DTO/VO/枚举/错误码/Port 接口
  - id: r2a-do-entities
    content: "Round 2A: 20 张表 DO Entity（MyBatis-Flex @Table 注解）+ 所有枚举类（StatusEnum/RiskLevelEnum/VerificationLevelEnum/FeedbackTypeEnum/QueueTypeEnum 等）"
    wbs: [WBS-001, WBS-002]
    req: []
    status: completed
    commit: "[PR #20]"
  - id: r2b-error-codes
    content: "Round 2B: 统一错误码（ErrorCode 00xxx~09xxx 五位码）+ 统一异常（BizException/SysException/ParamException）+ 全局异常处理器"
    wbs: [WBS-001]
    req: []
    status: completed
    commit: "[PR #20]"
  - id: r2c-base-dto-vo
    content: "Round 2C: 基础 DTO/VO（PageRequest/PageResponse/ApiResponse 等）+ Port 接口（QuotaPort/AuditPort/CachePort/SignaturePort/EncryptionPort）"
    wbs: [WBS-001]
    req: []
    status: completed
    commit: "[PR #20]"
  - id: r2d-satoken-base
    content: "Round 2D: Sa-Token 登录/登出基础（StpLogic 自定义登录逻辑）+ Token 生成/验证 + Redis 会话"
    wbs: [WBS-004]
    req: [REQ-PER-003]
    status: completed
    commit: "[PR #20]"

  # ===================== Round 3: 第一层——Auth + Workspace =====================
  # 前置：Round 2 完成
  # 目标：用户能登录、能注册、能管理自己的空间
  - id: r3a-auth-module
    content: "Round 3A: Auth 模块（UserController login/logout/register/profile/info + UserService + UserMapper + RBAC 权限框架 + ABAC 策略引擎）"
    wbs: [WBS-004, WBS-005]
    req: [REQ-PER-001, REQ-PER-002, REQ-PER-003]
    status: completed
    commit: "[PR #24]"
  - id: r3b-workspace-module
    content: "Round 3B: Workspace 模块（WorkspaceController list/create/detail/delete + WorkspaceService + MembershipService + 4 类空间 CRUD）"
    wbs: [WBS-007]
    req: [REQ-PER-001, REQ-PER-004]
    status: completed
    commit: "[PR #24]"
  - id: r3c-cumulative-nav-api
    content: "Round 3C: 累加式导航 API（GET /api/auth/nav = base_user_nav + space_membership_nav + granted_scope_nav + governance_nav + admin_nav，配置化返回）"
    wbs: [WBS-006]
    req: [REQ-PER-002]
    status: completed
    commit: "[PR #24]"

  # ===================== Round 4: 第二层——Quota + Connect + MCP/CLI =====================
  # 前置：Round 3 完成
  # 目标：搜索有配额限制，AI 工具能接入会话
  - id: r4a-quota-rate-limit
    content: "Round 4A: Quota 模块（每日 quota Redis 统计，第 11 次返回 HTTP 429 + QUOTA_EXCEEDED）+ RateLimit 模块（每分钟限流 + retry_after 响应头）"
    wbs: [WBS-008, WBS-009]
    req: [REQ-AIC-005]
    status: completed
    commit: "[PR #25]"
  - id: r4b-connect-module
    content: "Round 4B: Connect 模块（ConnectController 会话创建 + doctor 检测 8 条标准 + 接入会话状态机 created→instruction_copied→tool_started→doctor_running→connected/degraded/failed）"
    wbs: [WBS-010]
    req: [REQ-AIC-001, REQ-AIC-002, REQ-AIC-004]
    status: completed
    commit: "[PR #25]"
  - id: r4c-mcp-cli
    content: "Round 4C: MCP Server（7 个工具 axiqra.search_before_act/get_solution/get_public_case/submit_trace/submit_feedback/create_candidate_seed/doctor）+ CLI 工具（6 个命令）"
    wbs: [WBS-011, WBS-012]
    req: [REQ-AIC-003]
    status: completed
    commit: "[PR #25]"

  # ===================== Round 5: 第三层——Search + Solution =====================
  # 前置：Round 4 完成
  # 目标：核心搜索闭环（用户能搜索到 Solution）和方案详情
  - id: r5a-search-module
    content: "Round 5A: Search 模块（SearchController search_before_act + 权限预过滤 + 多路召回 + 排序融合 + Candidate Seed 生成 + 空结果处理，搜索闭环）"
    wbs: [WBS-013]
    req: [REQ-SEA-001, REQ-SEA-002, REQ-SEA-003, REQ-SEA-004, REQ-SEA-005]
    status: completed
    commit: "[PR #27]"
  - id: r5b-solution-module
    content: "Round 5B: Solution 模块（SolutionController 详情/版本/反馈统计 + L0-L5 验证等级 + R0-R4 风险等级 + 状态机 Draft→Candidate→NeedsReview→Reviewed→Verified→Stable→Canonical）"
    wbs: [WBS-014]
    req: [REQ-SOL-001, REQ-SOL-002, REQ-SOL-003]
    status: completed
    commit: "[PR #27]"

  # ===================== Round 6: 第四层——Trace + Project Case + Public Case =====================
  # 前置：Round 5 完成
  # 目标：工程轨迹沉淀闭环和案例发布闭环
  - id: r6a-trace-module
    content: "Round 6A: Trace 模块（TraceController 提交 + 证据引用 + 用户确认 + 幂等键 + Trace Package 状态机 Draft→UserConfirmed→Submitted→NeedsReview，Trace 闭环）"
    wbs: [WBS-016]
    req: [REQ-TRC-001, REQ-TRC-003]
    status: pending
  - id: r6b-project-case
    content: "Round 6B: Project Case 模块（ProjectCaseController 私有 Case 创建 + 空间复用 + 发布申请 + license_scope 校验，Project Case 闭环）"
    wbs: [WBS-017]
    req: [REQ-TRC-002, REQ-CAS-003]
    status: pending
  - id: r6c-public-case
    content: "Round 6C: Public Case 模块（PublicCaseController 脱敏 + 授权 + 详情 + 列表 + 可见性控制，发布闭环）"
    wbs: [WBS-018]
    req: [REQ-CAS-001, REQ-CAS-002]
    status: pending

  # ===================== Round 7: 第五层——Feedback + Review + Contribution =====================
  # 前置：Round 6 完成
  # 目标：调用反馈和治理闭环
  - id: r7a-invocation-feedback
    content: "Round 7A: Invocation 模块（InvocationController worked/partial/failed/not_applicable）+ Feedback 模块（FeedbackController 反馈提交 + 影响 Solution 验证等级）"
    wbs: [WBS-015]
    req: [REQ-SOL-004, REQ-CON-001]
    status: pending
  - id: r7b-review-module
    content: "Round 7B: Review 模块（ReviewController 审核队列 + R0-R4 分层 + reason_code + approve/reject/quarantine + 申诉队列 + 污染隔离）"
    wbs: [WBS-019, WBS-020]
    req: [REQ-GOV-001, REQ-GOV-002, REQ-GOV-003]
    status: pending
  - id: r7c-contribution
    content: "Round 7C: Contribution 模块（ContributionLedgerService 贡献账本 + 积分 + 反作弊）+ Credential 模块（认证凭证预留 + 能力档案 + 认证审核者角色）"
    wbs: [WBS-021, WBS-022]
    req: [REQ-CON-001, REQ-CON-002]
    status: pending
  - id: r7d-tool-model
    content: "Round 7D: 工具模型归因（新增/回填记录 tool_name + reported_model_name）+ 全局工具模型排行榜（7 天成功率聚合 + sample_size<20 保护 + 榜单 API）"
    wbs: [WBS-026]
    req: [REQ-RANK-001, REQ-RANK-002]
    status: pending

  # ===================== Round 8: 第六层——前端 + 测试 + 验收 =====================
  # 前置：Round 7 完成
  # 目标：完整系统交付
  - id: r8a-frontend
    content: "Round 8A: axiqra-frontend Vue 3（16 个页面，路由/菜单/权限码/页面元数据全部从后端 API 获取配置化渲染）"
    wbs: [WBS-023]
    req: [画面一览 P01-P16]
    status: pending
  - id: r8b-testing
    content: "Round 8B: 测试体系（单元测试 + 集成测试 + E2E 测试 + 覆盖率报告 ≥80% + 47 条原子用例保留）"
    wbs: [WBS-024]
    req: []
    status: pending
  - id: r8c-release
    content: "Round 8C: S1 Gate 验收（Gate 1-5 逐个通过 + 缺陷关闭 + 10 个闭环端到端验证 + 13 项红线验证）"
    wbs: [WBS-025]
    req: []
    status: pending
```

### WBS 详细对照表

| Round | WBS ID | 任务 | 关联功能 | 状态 |
|-------|---------|------|---------|:----:|
| R1A/B | WBS-001 | logback-spring.xml 增强 + TraceIdFilter + 全局异常 | 共通 | ✅ 已完成 |
| R1C/D | WBS-004 | API 签名认证 Filter + 加密/哈希/脱敏工具 | REQ-PER-003 | ✅ 已完成 |
| R1E | WBS-003 | PostgreSQL 审计库 + AuditLog Entity + AuditPort | REQ-AUD-001/002 | ✅ 已完成 |
| R2A | WBS-002 | 20 张表 DO Entity + 枚举类 | 共通 | ✅ 已完成 |
| R2B | WBS-001 | 统一错误码 + 统一异常 | 共通 | ✅ 已完成 |
| R2C | WBS-001 | 基础 DTO/VO + Port 接口 | 共通 | ✅ 已完成 |
| R2D | WBS-004 | Sa-Token 登录/登出基础 | REQ-PER-003 | ✅ 已完成 |
| R3A | WBS-004/005 | Auth 模块（登录/RBAC/ABAC） | REQ-PER-001/002/003 | ✅ 已完成 |
| R3B | WBS-007 | Workspace 模块（CRUD + 成员） | REQ-PER-001/004 | ✅ 已完成 |
| R3C | WBS-006 | 累加式导航 API | REQ-PER-002 | ✅ 已完成 |
| R4A | WBS-008/009 | Quota + RateLimit | REQ-AIC-005 | ✅ 已完成 |
| R4B | WBS-010 | Connect 模块 + doctor | REQ-AIC-001/002/004 | ✅ 已完成 |
| R4C | WBS-011/012 | MCP Server + CLI | REQ-AIC-003 | ✅ 已完成 |
| R5A | WBS-013 | Search 模块（权限预过滤 + 召回） | REQ-SEA-001~005 | ✅ 已完成 |
| R5B | WBS-014 | Solution 模块（状态机 + 验证等级） | REQ-SOL-001~003 | ✅ 已完成 |
| R6A | WBS-016 | Trace 模块 | REQ-TRC-001/003 | 待开发 |
| R6B | WBS-017 | Project Case 模块 | REQ-TRC-002 | 待开发 |
| R6C | WBS-018 | Public Case 模块 | REQ-CAS-001~003 | 待开发 |
| R7A | WBS-015 | Invocation + Feedback | REQ-SOL-004 | 待开发 |
| R7B | WBS-019/020 | Review 模块 | REQ-GOV-001~003 | 待开发 |
| R7C | WBS-021/022 | Contribution + Credential | REQ-CON-001/002 | 待开发 |
| R7D | WBS-026 | 工具模型归因 + 排行榜 | REQ-RANK-001/002 | 待开发 |
| R8A | WBS-023 | 前端页面（16 页面，配置化） | 画面一览 | 待开发 |
| R8B | WBS-024 | 单元/集成/E2E + 覆盖率 | 测试验收 | 待开发 |
| R8C | WBS-025 | S1 Gate + 13 红线 | 05-03 | 待开发 |

---

## 技术栈总览

| 层级 | 技术选型 |
|------|---------|
| 后端框架 | Spring Boot 3.4.x + 模块化单体 |
| ORM | MyBatis-Flex（零第三方依赖，数据脱敏/加密全免费） |
| 数据库 | CockroachDB（PostgreSQL 协议，单机起步，Apache 2.0） |
| 审计库 | 独立 PostgreSQL（append-only，与业务库物理隔离） |
| 缓存/会话 | Redis |
| 消息队列 | RabbitMQ |
| 对象存储 | MinIO（SSE-S3 服务端加密） |
| 访问层 | MCP Server（7 工具）+ CLI（6 命令）+ REST API |
| 认证 | Sa-Token（StpLogic 自定义登录 + Redis 会话） |
| 前端 | Vue 3 + TypeScript + Naive UI + Pinia（路由/菜单/权限码全部从后端 API 配置化获取） |
| 容器 | Docker + docker-compose（全套基础设施，容器名统一前缀 `axiqra_`） |

---

## 待办事项

- [ ] PR #27 合入 main
- [ ] `test-cases/R5-*/` 测试用例目录（执行记录、覆盖率说明、审核报告）
- [ ] `docs/R5-启动与计划同步说明.md` 更新 R5 完成状态
- [ ] `ROADMAP.md` 更新 Phase 描述（对应 S1 MVP 范围）
- [ ] R6 开发（Trace + Project Case + Public Case）
- [ ] R7 开发（Feedback + Review + Contribution + 排行榜）
- [ ] R8 开发（前端 + 测试 + S1 Gate 验收）

---

## 下一步

R5 合入 main 后，进入 R6 阶段：

- Engineering Trace Package 的写入与读取 API
- Engineering Trace 与 Solution 的关联绑定
- Public Case 的发布与治理流程
- MCP Server 的完整协议覆盖
