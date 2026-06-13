---

name: Axiqra S1 MVP 生产级完整架构

overview: 基于已验证的产品文档，建设生产级 Spring Boot + Vue 3 + Docker 全栈项目。代码库位于 axiqra-project/（axiqra-infra + axiqra-website 已就绪，axiqra-code 阶段开始）。采用"先地基后盖房"Round 架构：Round 1（日志+安全+审计）→ Round 2（Common 层）→ Round 3（Auth+Workspace）→ Round 4（Quota+Connect+MCP）→ Round 5（Search+Solution）→ Round 6（Trace+Case）→ Round 7（Feedback+Review+Contribution）→ Round 8（前端+测试+验收）。覆盖 S1 全部核心闭环：登录 → Connect 会话 → 搜索 → Solution → Trace 回传 → Project Case → Public Case → Review → Feedback。技术栈：MyBatis-Flex + CockroachDB（20张表）+ PostgreSQL 审计库（8张表）+ Redis + RabbitMQ + MinIO + Sa-Token + Docker 全套基础设施 + 全模块可插拔架构（Port 接口 + 适配器模式）+ 并发保护与容灾自愈体系（限流/熔断/线程池隔离/HikariCP/HealthCheck/restart:unless-stopped）+ TLS/签名/加密安全体系（算法可插拔，可切换国密）+ SLF4J 结构化日志（MDC traceId+userId）+ Resilience4j 熔断降级 + 优雅关闭 + 完整测试体系（单元/集成/E2E + 覆盖率报告）+ 工具模型归因与排行榜（WBS-026）+ 先设计再开发流程（Guardian Agent 守门 + 小步提交规范）+ GitHub Actions CI 强制门卫。

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

  # 目标：所有业务模块依赖的 DO/DTO/VO/枚举/错误码/port 接口

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

    commit: "[PR #28]"

  - id: r5b-solution-module

    content: "Round 5B: Solution 模块（SolutionController 详情/版本/反馈统计 + L0-L5 验证等级 + R0-R4 风险等级 + 状态机 Draft→Candidate→NeedsReview→Reviewed→Verified→Stable→Canonical）"

    wbs: [WBS-014]

    req: [REQ-SOL-001, REQ-SOL-002, REQ-SOL-003]

    status: completed

    commit: "[PR #28]"



  # ===================== Round 6: 第四层——Trace + Project Case + Public Case =====================

  # 前置：Round 5 完成

  # 目标：工程轨迹沉淀闭环和案例发布闭环

  - id: r6a-trace-module

    content: "Round 6A: Trace 模块（TraceController 提交 + 证据引用 + 用户确认 + 幂等键 + Trace Package 状态机 Draft→UserConfirmed→Submitted→NeedsReview，Trace 闭环）"

    wbs: [WBS-016]

    req: [REQ-TRC-001, REQ-TRC-003]

    status: completed

    commit: "[PR #30]"

  - id: r6b-project-case

    content: "Round 6B: Project Case 模块（ProjectCaseController 私有 Case 创建 + 空间复用 + 发布申请 + license_scope 校验，Project Case 闭环）"

    wbs: [WBS-017]

    req: [REQ-TRC-002, REQ-CAS-003]

    status: completed

    commit: "[PR #30]"

  - id: r6c-public-case

    content: "Round 6C: Public Case 模块（PublicCaseController 脱敏 + 授权 + 详情 + 列表 + 可见性控制，发布闭环）"

    wbs: [WBS-018]

    req: [REQ-CAS-001, REQ-CAS-002]

    status: completed

    commit: "[PR #30]"



  # ===================== Round 7: 第五层——Feedback + Review + Contribution =====================

  # 前置：Round 6 完成

  # 目标：调用反馈和治理闭环

  - id: r7a-invocation-feedback

    content: "Round 7A: Invocation 模块（InvocationController 上报 + 详情 + solution反馈统计）+ Feedback 模块（FeedbackController 反馈提交 + 影响 Solution 验证等级）"

    wbs: [WBS-015]

    req: [REQ-SOL-004, REQ-CON-001]

    status: completed

    commit: "[PR #31]"

  - id: r7b-review-module

    content: "Round 7B: Review 模块（ReviewController 审核队列 + R0-R4 分层 + reason_code + approve/reject/quarantine + 申诉队列 + 污染隔离）"

    wbs: [WBS-019, WBS-020]

    req: [REQ-GOV-001, REQ-GOV-002, REQ-GOV-003]

    status: completed

    commit: "[PR #31]"

  - id: r7c-contribution

    content: "Round 7C: Contribution 模块（ContributionLedgerService 贡献账本 + 积分 + 反作弊）"

    wbs: [WBS-021]

    req: [REQ-CON-001]

    status: completed

    commit: "[PR #31]"

  - id: r7d-tool-model

    content: "Round 7D: 工具模型归因（新增/回填记录 tool_name + reported_model_name）+ 全局工具模型排行榜（7 天成功率聚合 + sample_size<20 保护 + 榜单 API）"

    wbs: [WBS-026]

    req: [REQ-RANK-001, REQ-RANK-002]

    status: completed

    commit: "[PR #31]"



  # ===================== Round 8: 第六层——前端 + 测试 + 验收 =====================

  # 前置：Round 7 完成

  # 目标：完整系统交付

  - id: r8a-frontend

    content: "Round 8A: axiqra-frontend Vue 3（16 个页面，路由/菜单/权限码/页面元数据全部从后端 API 获取配置化渲染）"

    wbs: [WBS-023]

    req: [画面一览 P01-P16]

    status: in_progress

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

isProject: false

---



# Axiqra S1 MVP 生产级完整架构计划



> 严格遵循阿里巴巴 Java 开发手册（嵩山版 1.7.x）规范，选用 2026 年国际顶尖开源技术栈。



---



## 整体进度



| 阶段 | 状态 | PR |

|------|------|-----|

| R1 日志安全审计框架 | 已完成，合入 main | #19 |

| R2 通用验证 RBAC 框架 | 已完成，合入 main | #20 |

| R3 Auth + Workspace + Nav | 已完成，合入 main | #24 |

| R4 Quota + Connect + MCP/CLI | 已完成，合入 main | #25 |

| R5 Search + Solution | 已完成，合入 main | #28 |

| R6 Trace + Project Case + Public Case | 已完成，合入 main | #30 |

| R7 Feedback + Review + Contribution + ToolModel | 已完成，待合入 main | #31 |

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



**状态：已完成，合入 main（PR #28）**



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

## R6: Trace + Project Case + Public Case

**状态：已完成，合入 main（PR #30）**

### R6A Trace 模块

- `POST /api/traces`：提交工程轨迹（幂等键 `idempotency_key` 防止重复提交）
- `GET /api/traces`：获取轨迹列表（支持 `status`、`workspace_id` 过滤）
- `GET /api/traces/{id}`：获取轨迹详情（含证据引用列表）
- Trace 状态机：`DRAFT` -> `USER_CONFIRMED` -> `SUBMITTED` -> `NEEDS_REVIEW`
- 证据引用：`TraceEvidenceRefEntity` 关联轨迹与外部证据（JSON 幂列化）
- API 签名认证 + 软删除 + 乐观锁

### R6B Project Case 模块

- `POST /api/project-cases`：在 Workspace 内创建私有 Case（复用 solution_id + workspace_id）
- `POST /api/project-cases/{id}/apply`：申请发布（生成对应的 PublicCase 草稿）
- `GET /api/project-cases`：列表（支持 `workspace_id`、`status`、`tech_stack` 过滤）
- `GET /api/project-cases/{id}`：详情（author 可见任何状态，其他人只可见 `APPROVED`）
- `license_scope` 校验：发布时检查关联 Solution 的 license_scope 权限

### R6C Public Case 模块

- `GET /api/public-cases`：公开案例列表（`status=APPROVED`，分页 + 多条件过滤）
- `GET /api/public-cases/{id}`：脱敏详情（自动过滤 `api_key`/`token`/`password` 等敏感字段）
- 可见性控制：`visibility_scope`（`private/workspace/enterprise/public`）+ 空间障离
- 授权机制：发布时关联 `AuthorizationEntity`，支持授权记录查询

### R6 代码提交记录

```
30  feat: add R6 trace and case workflow support
29  fix: tighten controller resource responses and async error handling
```

---

## R7: Invocation + Feedback + Review + Contribution + ToolModel

**状态：已完成，待合入 main（PR #31）**

### R7A Invocation + Feedback 模块

- `POST /api/invocations`：上报 AI Tool 调用结果（幂等键 `request_id`，支持 worked/partial/failed/not_applicable）
- `GET /api/invocations/{id}`：调用详情（带权限校验，只能查看自己的调用）
- `GET /api/invocations/solutions/{id}/feedback-stats`：Solution 反馈统计
- `POST /api/feedbacks`：提交反馈（关联 invocation_id，支持 JSON 证据引用幂列化）
- `GET /api/feedbacks`：反馈列表（支持 `invocation_id`、`feedback_type`、`solution_id` 过滤）
- `GET /api/feedbacks/solutions/{id}/stats`：Solution 反馈统计（WORKED/FAILED/NOT_APPLICABLE 计数）
- 反馈影响 Solution 验证等级：WORKED 提升 L0->L2，FAILED 降低，L3+ 不变
- `@RequireScope` 覆盖：`connect:write/read`、`feedback:read/write`

### R7B Review 模块

- `GET /api/reviews/pending`：待审核队列（R0~R4 分层，`QUARANTINED` 优先级最低）
- `GET /api/reviews/{id}`：审核详情（含 `reason_code`/`notes`/`appeal_content`）
- `POST /api/reviews/{id}/approve`：`APPROVED` -> `VERIFIED`，更新 Solution 验证等级
- `POST /api/reviews/{id}/reject`：`REJECTED`，reasonCode 记录违规原因
- `POST /api/reviews/{id}/quarantine`：`QUARANTINED`，污染障离并停止扩散
- `POST /api/reviews/{id}/appeal`：甲诉（REJECTED/QUARANTINED -> APPEAL_IN_PROGRESS）
- 乐观锁保护 + `@RequireScope("review:write")`
- Review 决策全量写入独立 PostgreSQL 审计库

### R7C Contribution 模块

- `GET /api/contributions/users/{userId}/summary`：用户贡献汇总（总积分 + 分类计数）
- `GET /api/contributions/users/{userId}/records`：贡献记录列表（支持 `limit` 分页）
- `ContributionLedgerEntity`：贡献账本，支持 `trace_created`/`solution_published`/`case_published` 事件
- 积分策略：trace_created +5 / solution_published +20 / case_published +50
- `@RequireScope("contribution:read")`

### R7D ToolModel 模块

- `GET /api/tool-models/leaderboard`：工具模型排行榜（7 天成功率聚合，`scope_type`/`scope_id` 过滤）
- `sample_size < 20` 保护：样本不足时不展示成功率，防止误导
- `POST /api/tool-models/backfill`：归因回填（tool_name + reported_model_name + requestId）
- 全局公开榜单（scopeType=public）无需认证；空间/企业榜单需对应 scope
- `@RequireScope("public:read")` 或更高权限

### R7 代码提交记录

```
822b164a fix: address CodeRabbit second-round review comments
dfae7450 fix: resolve remaining API review issues from GitHub Actions
fbfce19  fix(R7): resolve PR review issues from CodeRabbit, Codex, and Agnes
4c0cfc4  feat: implement Invocation + Feedback + Review + Contribution + ToolModel modules
```




## 一、技术栈总览



| 层级 | 技术选型 | 版本 | 说明 |

|------|---------|------|------|

| 后端框架 | Spring Boot | 3.4.x | JDK 17+，Jakarta EE |

| ORM | **MyBatis-Flex** | 1.9+ | 零第三方依赖，性能比 MyBatis-Plus 快 5-10 倍，数据脱敏/加密/多租户全免费 |

| 数据库 | **CockroachDB** | 24.x | PostgreSQL 协议兼容，Apache 2.0，单机起步，自动横向扩展，多区域强一致 |

| 审计库 | **PostgreSQL** | 16+ | 独立部署，仅存储 append-only 审计日志 |

| 分库分表/读写分离 | **Apache ShardingSphere-JDBC** | 5.5.x | Apache 顶级项目，透明路由，平滑升级分片 |

| 连接池 | Druid | 1.2.x | 监控面板 |

| 权限认证 | Sa-Token | 1.42.x | 轻量，Redis 分布式会话，RBAC |

| 缓存 | Redis | 7.x | Sa-Token 会话 + 配额计数 |

| 对象存储 | **MinIO** | 2025+ | S1 预留，证据文件 SSE-S3 服务端加密 |

| API 文档 | Knife4j | 4.5.x | Swagger 增强 |

| 工具库 | Hutool | 5.8.x | 阿里开源 |

| 日志框架 | SLF4J + Logback | - | 结构化日志，异步写入，滚动回滚 |

| 前端 | Vue 3 + TypeScript + Naive UI | 3.x / 4.x | 国际主流组件库 |

| 构建 | Vite | 6.x | 现代快 |

| 容器化 | Docker + Docker Compose | 24.x | 所有服务容器化，文件化管理 |

| 证书 | Let's Encrypt | - | HTTPS 自动续期（生产环境） |



**所有组件均为 Apache 2.0 / MIT 开源，无商业锁定。**



---



## 二、完整项目结构



```

axiqra-project/                         # 项目根目录（整个代码结构的总文件夹）

│

├── axiqra-code/                         # 后端代码仓库

│   ├── axiqra-common/                   # 通用层

│   │   └── src/main/java/com/axiqra/common/

│   │       ├── domain/do/             # 数据对象（对应数据库表）

│   │       ├── domain/dto/             # 请求 DTO

│   │       ├── domain/vo/              # 视图 DTO

│   │       ├── domain/enums/           # 枚举（每个字段加中文注释）

│   │       ├── exception/              # 统一异常

│   │       ├── util/                   # 工具类（中文注释）

│   │       └── constant/               # 常量类

│   │

│   ├── axiqra-core/                     # 核心业务层

│   │   └── src/main/java/com/axiqra/core/

│   │       ├── mapper/                # MyBatis-Flex Mapper（中文注释）

│   │       ├── service/               # Service 接口

│   │       └── service/impl/          # 业务实现（中文注释）

│   │

│   ├── axiqra-api/                     # API 层

│   │   └── src/main/java/com/axiqra/api/

│   │       ├── controller/            # REST Controller（中文注释）

│   │       ├── config/                # 配置类（中文注释）

│   │       ├── security/              # Sa-Token + API 签名认证

│   │       └── exception/             # 全局异常处理

│   │

│   ├── axiqra-start/                   # 启动模块

│   │   └── src/main/java/com/axiqra/

│   │       └── AxiqraApplication.java

│   │   └── src/main/resources/

│   │       ├── application.yml        # 主配置（dev/staging/prod）

│   │       ├── application-dev.yml

│   │       ├── application-prod.yml

│   │       └── logback-spring.xml     # SLF4J 日志配置（滚动/分级/结构化）

│   │

│   ├── Dockerfile                     # 后端多阶段构建（maven build → java 运行）

│   └── pom.xml                        # 父 pom（所有依赖版本管理）

│

├── axiqra-infra/                       # Docker 基础设施（所有中间件）

│   ├── docker-compose.yml             # 主编排（所有服务统一前缀 axiqra_）

│   ├── .env                          # 环境变量（敏感信息，不提交 Git）

│   ├── docker/

│   │   ├── cockroachdb/

│   │   │   ├── docker-compose.yml    # CockroachDB 单机 + TLS + healthcheck

│   │   │   └── init.sql              # 业务库建表脚本

│   │   ├── postgres-audit/

│   │   │   ├── docker-compose.yml    # PostgreSQL 审计库 + healthcheck

│   │   │   └── init.sql             # 审计库建表脚本

│   │   ├── redis/

│   │   │   └── docker-compose.yml    # Redis + healthcheck

│   │   └── minio/

│   │       ├── docker-compose.yml    # MinIO + TLS + healthcheck

│   │       └── init.sh               # MinIO 初始化脚本（创建 bucket）

│   │

│   └── scripts/

│       ├── init-all.sh                # 一键初始化所有中间件

│       └── wait-for-it.sh             # 等待依赖就绪脚本（docker 健康检查）

│

├── .env.example                        # 环境变量模板（复制为 .env）

├── Makefile                            # 一键命令（make up / make logs / make down）

├── .gitignore                          # 排除 node_modules / target / .env / logs

└── README.md                           # 项目文档 + 快速启动指南

```



---



## 三、Docker 基础设施规范



### 3.1 统一命名风格（强制）



**所有资源统一前缀 `axiqra_`**，不得随意命名：



| 资源类型 | 命名格式 | 示例 |

|---------|---------|------|

| 容器名 | `axiqra_服务名_角色` | `axiqra_cockroachdb_primary`、`axiqra_redis_cache` |

| 网络 | `axiqra_internal_net` | 所有容器加入此网络 |

| Volume | `axiqra_服务名_data` | `axiqra_cockroachdb_data`、`axiqra_audit_data` |

| 服务名 | `axiqra-core`、`axiqra-frontend` | docker-compose service name |



### 3.2 docker-compose.yml 主文件



```yaml

version: "3.9"



# ============================================================

# Axiqra S1 MVP 全量中间件编排

# 启动命令：cd axiqra-infra && docker compose up -d

# 查看日志：cd axiqra-infra && docker compose logs -f

# 停止：cd axiqra-infra && docker compose down

# ============================================================



services:

  # -------------------- CockroachDB 业务主库 --------------------

  axiqra_cockroachdb_primary:

    image: cockroachdb/cockroach:v24.1.0

    container_name: axiqra_cockroachdb_primary

    hostname: axiqra-cockroachdb

    command: start-single-node --insecure

    ports:

      - "26257:26257"  # SQL 端口

      - "26258:26258"  # HTTP 管理端口

    volumes:

      - axiqra_cockroachdb_data:/cockroach/cockroach-data

      - ./docker/cockroachdb/init.sql:/docker-entrypoint-initdb.d/init.sql

    networks:

      - axiqra_internal_net

    healthcheck:

      test: ["CMD", "cockroach", "sql", "--insecure", "-e", "SELECT 1"]

      interval: 10s

      timeout: 5s

      retries: 10

      start_period: 30s



  # -------------------- PostgreSQL 审计库 --------------------

  axiqra_postgres_audit:

    image: postgres:16-alpine

    container_name: axiqra_postgres_audit

    environment:

      POSTGRES_DB: axq_audit_db

      POSTGRES_USER: ${AUDIT_DB_USER}

      POSTGRES_PASSWORD: ${AUDIT_DB_PASSWORD}

    ports:

      - "5432:5432"

    volumes:

      - axiqra_audit_data:/var/lib/postgresql/data

      - ./docker/postgres-audit/init.sql:/docker-entrypoint-initdb.d/init.sql

    networks:

      - axiqra_internal_net

    healthcheck:

      test: ["CMD-SHELL", "pg_isready -U ${AUDIT_DB_USER} -d axq_audit_db"]

      interval: 10s

      timeout: 5s

      retries: 5

      start_period: 20s



  # -------------------- Redis 缓存 --------------------

  axiqra_redis_cache:

    image: redis:7-alpine

    container_name: axiqra_redis_cache

    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes

    ports:

      - "6379:6379"

    volumes:

      - axiqra_redis_data:/data

    networks:

      - axiqra_internal_net

    healthcheck:

      test: ["CMD", "redis-cli", "--no-auth-warning", "-a", "${REDIS_PASSWORD}", "ping"]

      interval: 10s

      timeout: 5s

      retries: 5



  # -------------------- 后端 API 服务 --------------------

  axiqra_core:

    build:

      context: ../axiqra-code

      dockerfile: axiqra-start/Dockerfile

    container_name: axiqra_core

    environment:

      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}

      DB_PASSWORD: ${DB_PASSWORD}

      REDIS_PASSWORD: ${REDIS_PASSWORD}

      AUDIT_DB_PASSWORD: ${AUDIT_DB_PASSWORD}

      JWT_SECRET: ${JWT_SECRET}

      FIELD_ENCRYPT_KEY: ${FIELD_ENCRYPT_KEY}

    ports:

      - "8080:8080"

      - "9090:9090"   # Actuator 监控端口

    volumes:

      - axiqra_logs:/var/log/axiqra    # 日志文件持久化

    depends_on:

      axiqra_cockroachdb_primary:

        condition: service_healthy

      axiqra_postgres_audit:

        condition: service_healthy

      axiqra_redis_cache:

        condition: service_healthy

    networks:

      - axiqra_internal_net

    healthcheck:

      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]

      interval: 30s

      timeout: 10s

      retries: 5

      start_period: 60s



networks:

  axiqra_internal_net:

    name: axiqra_internal_net

    driver: bridge



volumes:

  axiqra_cockroachdb_data:

    name: axiqra_cockroachdb_data

  axiqra_audit_data:

    name: axiqra_audit_data

  axiqra_redis_data:

    name: axiqra_redis_data

  axiqra_logs:

    name: axiqra_logs

```



---



## 四、日志系统规范



### 4.1 日志设计目标



- **可排查**：每条日志带 traceId、userId、class、method、message，格式统一

- **可回滚**：按日期 + 大小滚动，保留 30 天 info/warn，90 天 error

- **异步写入**：不阻塞业务线程

- **双重输出**：同时写文件和输出到 stdout（docker logs 可看）

- **分级存储**：info、warn、error 分文件存储



### 4.2 logback-spring.xml 配置



```xml

<?xml version="1.0" encoding="UTF-8"?>

<configuration scan="true" scanPeriod="30 seconds">



    <!-- ===================== 变量定义 ===================== -->

    <property name="LOG_HOME" value="/var/log/axiqra"/>

    <property name="APP_NAME" value="axiqra"/>

    <property name="MAX_FILE_SIZE" value="100MB"/>

    <property name="MAX_HISTORY_INFO" value="30"/>

    <property name="MAX_HISTORY_WARN" value="30"/>

    <property name="MAX_HISTORY_ERROR" value="90"/>

    <property name="TOTAL_SIZE_CAP" value="10GB"/>



    <!-- ===================== Console 输出（Docker logs 可看） ===================== -->

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">

        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">

            <layout class="ch.qos.logback.classic.PatternLayout">

                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [%X{traceId:-NO_TRACE}] [%X{userId:-NO_USER}] [%thread] %-40logger{50} - %msg%n</pattern>

            </layout>

        </encoder>

    </appender>



    <!-- ===================== Info 日志文件（每日滚动，保留 30 天） ===================== -->

    <appender name="INFO_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">

        <file>${LOG_HOME}/${APP_NAME}-info.log</file>

        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">

            <fileNamePattern>${LOG_HOME}/${APP_NAME}-info.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>

            <maxFileSize>${MAX_FILE_SIZE}</maxFileSize>

            <maxHistory>${MAX_HISTORY_INFO}</maxHistory>

            <totalSizeCap>${TOTAL_SIZE_CAP}</totalSizeCap>

        </rollingPolicy>

        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">

            <layout class="ch.qos.logback.classic.PatternLayout">

                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{traceId:-NO_TRACE}] [%X{userId:-NO_USER}] [%thread] %-40logger{50} - %msg%n</pattern>

            </layout>

        </encoder>

        <filter class="ch.qos.logback.classic.filter.LevelFilter">

            <level>INFO</level>

            <onMatch>ACCEPT</onMatch>

            <onMismatch>DENY</onMismatch>

        </filter>

    </appender>



    <!-- ===================== Warn 日志文件（每日滚动，保留 30 天） ===================== -->

    <appender name="WARN_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">

        <file>${LOG_HOME}/${APP_NAME}-warn.log</file>

        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">

            <fileNamePattern>${LOG_HOME}/${APP_NAME}-warn.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>

            <maxFileSize>${MAX_FILE_SIZE}</maxFileSize>

            <maxHistory>${MAX_HISTORY_WARN}</maxHistory>

            <totalSizeCap>${TOTAL_SIZE_CAP}</totalSizeCap>

        </rollingPolicy>

        <encoder>

            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{traceId:-NO_TRACE}] [%X{userId:-NO_USER}] [%thread] %-40logger{50} - %msg%n</pattern>

        </encoder>

        <filter class="ch.qos.logback.classic.filter.LevelFilter">

            <level>WARN</level>

            <onMatch>ACCEPT</onMatch>

            <onMismatch>DENY</onMismatch>

        </filter>

    </appender>



    <!-- ===================== Error 日志文件（每日滚动，保留 90 天） ===================== -->

    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">

        <file>${LOG_HOME}/${APP_NAME}-error.log</file>

        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">

            <fileNamePattern>${LOG_HOME}/${APP_NAME}-error.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>

            <maxFileSize>${MAX_FILE_SIZE}</maxFileSize>

            <maxHistory>${MAX_HISTORY_ERROR}</maxHistory>

            <totalSizeCap>${TOTAL_SIZE_CAP}</totalSizeCap>

        </rollingPolicy>

        <encoder>

            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{traceId:-NO_TRACE}] [%X{userId:-NO_USER}] [%thread] %-40logger{50} - %msg%n</pattern>

        </encoder>

        <filter class="ch.qos.logback.classic.filter.LevelFilter">

            <level>ERROR</level>

            <onMatch>ACCEPT</onMatch>

            <onMismatch>DENY</onMismatch>

        </filter>

    </appender>



    <!-- ===================== 异步包装器（不阻塞业务线程） ===================== -->

    <appender name="ASYNC_INFO" class="ch.qos.logback.classic.AsyncAppender">

        <queueSize>512</queueSize>

        <discardingThreshold>0</discardingThreshold>

        <includeCallerData>false</includeCallerData>

        <appender-ref ref="INFO_FILE"/>

    </appender>

    <appender name="ASYNC_WARN" class="ch.qos.logback.classic.AsyncAppender">

        <queueSize>512</queueSize>

        <discardingThreshold>0</discardingThreshold>

        <includeCallerData>false</includeCallerData>

        <appender-ref ref="WARN_FILE"/>

    </appender>

    <appender name="ASYNC_ERROR" class="ch.qos.logback.classic.AsyncAppender">

        <queueSize>512</queueSize>

        <discardingThreshold>0</discardingThreshold>

        <includeCallerData>false</includeCallerData>

        <appender-ref ref="ERROR_FILE"/>

    </appender>



    <!-- ===================== 日志级别配置 ===================== -->

    <logger name="com.axiqra" level="INFO"/>

    <logger name="com.mybatisflex" level="WARN"/>

    <logger name="org.apache.shardingsphere" level="WARN"/>

    <logger name="org.springframework" level="WARN"/>

    <logger name="org.apache.http" level="WARN"/>

    <root level="INFO">

        <appender-ref ref="CONSOLE"/>

        <appender-ref ref="ASYNC_INFO"/>

        <appender-ref ref="ASYNC_WARN"/>

        <appender-ref ref="ASYNC_ERROR"/>

    </root>



</configuration>

```



### 4.3 日志规范代码示例



```java

@Slf4j

@Service

public class DemoService {



    public void searchBeforeAct(Long userId, String query) {

        log.info("【搜索】开始搜索，userId={}, query={}", userId, query);



        try {

            if (quotaExceeded) {

                log.warn("【搜索配额】用户配额超限，userId={}, currentQuota=10, attempted=11");

                throw new BizException(ErrorCode.QUOTA_LIMITED);

            }

            log.info("【搜索】搜索成功，userId={}, resultCount={}", userId, results.size());



        } catch (BizException e) {

            log.error("【搜索】搜索失败，userId={}, errorCode={}, errorMsg={}",

                    userId, e.getErrorCode(), e.getMessage());

            throw e;

        } catch (Exception e) {

            log.error("【搜索】系统异常，userId={}, query={}, 异常信息={}",

                    userId, query, e.getMessage(), e);

            throw new SysException(e);

        }

    }

}

```



```java

@Slf4j

@Component

@Order(1)

public class TraceIdFilter implements Filter {



    private static final String TRACE_ID_HEADER = "X-Trace-Id";



    @Override

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)

            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        HttpServletResponse httpResponse = (HttpServletResponse) response;



        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);

        if (traceId == null || traceId.isBlank()) {

            traceId = UUID.randomUUID().toString().replace("-", "");

        }

        MDC.put("traceId", traceId);

        httpResponse.setHeader(TRACE_ID_HEADER, traceId);



        log.info("【请求入口】method={}, uri={}, traceId={}",

                httpRequest.getMethod(), httpRequest.getRequestURI(), traceId);



        try {

            chain.doFilter(request, response);

        } finally {

            MDC.clear();

        }

    }

}

```



---



## 五、安全体系



### 5.1 全链路 TLS 加密



| 传输链路 | S1 做法 | 证书 |

|---------|--------|------|

| 用户浏览器 → Nginx → API | HTTPS (TLS 1.3) | Let's Encrypt（生产）或自签名（开发） |

| API → CockroachDB | PostgreSQL TLS 连接 | CockroachDB 内置证书或自签 |

| API → PostgreSQL 审计库 | TLS 连接 | 自签或云托管证书 |

| API → Redis | 开发用明文，**生产必须 TLS + ACL** | - |

| API → MinIO | HTTPS + MinIO 签名认证 + SSE-S3 加密 | MinIO 内置 TLS |



### 5.2 API 签名认证（HMAC-SHA256）



**所有 AI Tool / MCP 调用 Search API 必须带签名**，防止请求被篡改和伪造：



```

请求头：

X-Axiqra-App-Id: app_xxx                      ← 应用 ID

X-Axiqra-Timestamp: 1717500000000              ← 时间戳（毫秒，5 分钟内有效）

X-Axiqra-Signature: hmac_sha256(              ← HMAC-SHA256 签名

    appId + timestamp + requestBody,

    appSecret

)

X-Axiqra-Nonce: uuid                          ← 防重放 nonce

```



### 5.3 敏感字段加密（AES-256-GCM）



使用 MyBatis-Flex 字段加密功能，对 `api_key`、`token`、`secret` 等字段自动加解密。



### 5.4 密钥管理原则



| 密钥类型 | 存储位置 | 禁止 |

|---------|---------|------|

| 数据库密码 | 环境变量 `DB_PASSWORD` | 硬编码、Git、镜像 |

| API 签名密钥 | 环境变量 `APP_SECRET` | 硬编码、Git、镜像 |

| JWT 签名密钥 | 环境变量 `JWT_SECRET` | 硬编码、Git、镜像 |

| 字段加密密钥 | 环境变量 `FIELD_ENCRYPT_KEY` | 硬编码、Git、镜像 |



**原则：密钥只存在于环境变量和 `.env` 文件（.env 不提交 Git）。**



### 5.5 应用层安全清单



| 安全场景 | 做法 |

|---------|------|

| SQL 注入 | MyBatis-Flex 参数化查询，零 SQL 拼接 |

| XSS | Vue 3 默认沙盒 + 后端输出编码 |

| CSRF | Sa-Token Session 机制天然防护 |

| 敏感字段暴露 | MyBatis-Flex 数据脱敏（api_key、token、password 输出 `****`） |

| 请求重放 | 时间戳（5 分钟窗口）+ Nonce（Redis 存储） |

| 暴力破解 | 登录失败计数，5 次后锁定 15 分钟 |

| 速率限制 | Redis + Bucket4j 或 Guava RateLimiter |

| CORS | 仅允许配置的域名，禁止 `*` |

| 审计日志 | 登录、搜索、配额、Trace 提交、审核决策**全量写入审计库** |



---



## 六、数据库表结构（CockroachDB + PostgreSQL）



> 所有表必备三字段：`id`（主键）、`gmt_create`（datetime）、`gmt_modified`（datetime）



### 6.1 CockroachDB 业务库（20 张表）



详见 `axiqra-infra/docker/cockroachdb/init.sql`，包含：



- `axq_user` 用户表

- `axq_workspace` 工作空间表

- `axq_connect_session` Connect 会话表

- `axq_solution` 方案表

- `axq_engineering_trace` 工程轨迹表

- `axq_invocation` 调用记录表

- `axq_feedback` 反馈表

- `axq_review` 审核记录表

- `axq_public_case` 公开案例表

- `axq_project_case` 项目案例表

- `axq_membership` 成员关系表

- `axq_candidate_seed` 候选种子表

- `axq_contribution_ledger` 贡献账本表

- `axq_policy_decision_log` 策略决策日志表

- `axq_trace_evidence_ref` 轨迹证据引用表

- `axq_tool_model_performance_daily` 工具模型日性能表

- `axq_tool_model_attribution` 工具模型归因表

- `axq_tool_model_leaderboard_snapshot` 工具模型排行榜快照表

- `axq_authorization` 授权表

- `axq_user_profile` 用户画像表



### 6.2 PostgreSQL 审计库（8 张表）



详见 `axiqra-infra/docker/postgres-audit/init.sql`，包括：



- `axiqra_audit` 主审计表（append-only，禁止 UPDATE 和 DELETE）

- `axiqra_authorization_audit` 授权审计表

- `axiqra_invocation_audit` 调用审计表

- `axiqra_policy_decision_audit` 策略决策审计表

- `axiqra_quota_audit` 配额审计表

- `axiqra_rate_limit_audit` 限流审计表

- `axiqra_review_decision_audit` 审核决策审计表

- `axiqra_tool_model_attribution_audit` 工具模型归因审计表



---



## 七、阿里开发手册强制规范清单



### 7.1 编程规约（强制）

- 所有覆写方法必须加 `@Override` 注解

- POJO 类禁止设定属性默认值

- 禁止在 POJO 类中同时存在 `isXxx()` 和 `getXxx()` 方法

- Object equals 使用 `常量.equals(对象)` 避免空指针

- 整型包装类比较使用 `equals()`

- 浮点数比较禁止用 `==`



### 7.2 命名规约（强制）

- 类名 UpperCamelCase，方法名/参数名/变量名 lowerCamelCase

- 常量全部大写 + 下划线：MAX_STOCK_COUNT

- DO/BO/DTO/VO 类名加对应后缀

- 包名统一小写



### 7.3 注释规约（强制）

- 类/属性/方法注释使用 Javadoc 规范 `/** 内容 */`

- 所有类必须添加 `@author` + `@date`

- 抽象方法 Javadoc 说明：返回值、参数、异常、做什么事

- 枚举类型每个字段必须有中文注释



### 7.4 建表规约（强制）

- **表必备三字段**：`id`（主键）、`gmt_create`（datetime）、`gmt_modified`（datetime）

- 布尔字段：`is_xxx`，tinyint unsigned（1=是，0=否）

- 小数类型：`decimal`，禁止 float/double

- varchar 不预分配

- 主键索引：`pk_字段名`；唯一索引：`uk_字段名`；普通索引：`idx_字段名`

- 更新记录必须同步更新 `gmt_modified`

- 超过 3 表禁止 join



---



## 八、联调验收测试用例



```

1. 注册用户 A（BASE_USER 角色）

2. 登录，创建一个 Connect 会话

3. 调用 POST /api/search/before-act，确认 quota 计数（10/10）

4. 提交一个 Trace（POST /api/traces），确认 token 脱敏生效

5. 用 REVIEWER 角色登录，审核该 Trace（approve）

6. 再次搜索，确认新 Solution 出现在结果中

7. 触发第 11 次搜索，确认 HTTP 429 + error_code=QUOTA_LIMITED

8. 退出登录，访问公开搜索，确认只返回 public 解决方案

9. 验证 CockroachDB 主库写入成功 + 审计日志写入独立 PostgreSQL

10. 查看 /var/log/axiqra/ 下的日志文件，确认 info/warn/error 分级存储

11. 用无效签名调用 MCP 接口，确认 HTTP 403 + INVALID_SIGNATURE

12. 用过期时间戳调用，确认 HTTP 403 + TIMESTAMP_EXPIRED

```



---



## 九、实施顺序



```

Step 1: axiqra-infra/

        → docker-compose.yml（所有中间件统一前缀 axiqra_）

        → CockroachDB + PostgreSQL + Redis + MinIO docker-compose

        → init.sql 建表脚本

        → .env.example + Makefile



Step 2: axiqra-code Maven 多模块脚手架

        → 父 pom（MyBatis-Flex 1.9 + Sa-Token 1.42 + Hutool 5.8）

        → axiqra-common / axiqra-core / axiqra-api / axiqra-start

        → Dockerfile（多阶段构建）

        → application.yml（dev/staging/prod 多环境）



Step 3: axiqra-code 日志系统

        → logback-spring.xml（滚动/分级/异步/结构化）

        → TraceIdFilter（请求追踪 + MDC）

        → 日志规范示例代码



Step 4: axiqra-code 安全体系

        → ApiSignatureFilter（HMAC-SHA256 + 时间戳 + Nonce 防重放）

        → AesEncryptUtil（AES-256-GCM 字段加密）

        → CORS 配置（仅允许配置域名）

        → RateLimiter（Redis + 配额强制）



Step 5: axiqra-common 层

        → 枚举类（MyBatis-Flex 注解支持，全部中文注释）

        → DO 类（@TableName + @TableId，全部中文注释）

        → DTO/VO 类（中文注释）

        → 统一返回 ApiResponse（中文注释）

        → 脱敏工具类（中文注释）



Step 6: axiqra-core 层

        → MyBatis-Flex Mapper（中文注释）

        → Service 实现（含配额强制、脱敏、审计写独立库、中文注释）



Step 7: axiqra-api 层

        → Sa-Token Security 配置（Redis 会话）

        → 全局异常处理（中文注释）

        → Controller（Auth + Search + Trace + Solution + Review、中文注释）



Step 8: axiqra-frontend

        → npm create vue（Naive UI）

        → Dockerfile（多阶段构建 + nginx 反向代理）

        → 登录/注册 + 搜索页 + Solution 详情 + Trace 提交 + 个人空间



Step 9: axiqra-infra 一键启动

        → make up（docker compose up -d）

        → 验证所有服务 healthcheck 通过

        → 验证日志文件生成

        → 联调验收完整闭环

```



---



## 十、关键原则（红线）



1. **配额红线**：第 11 次 MCP 搜索必须返回 HTTP 429 + `QUOTA_LIMITED`，不得静默降级

2. **脱敏红线**：Trace 提交时使用 MyBatis-Flex Entity 监听器自动脱敏 `api_key`、`token`、`password`

3. **审计红线**：登录、搜索、提交、审核必须写入独立 PostgreSQL 审计库

4. **权限红线**：搜索结果在召回前过滤，不走后过滤

5. **建表红线**：所有表必须包含 `id, gmt_create, gmt_modified` 三字段

6. **更新红线**：所有 UPDATE 操作必须同步更新 `gmt_modified`

7. **日志红线**：所有业务方法必须有 info/warn/error 分级日志，不得空白日志

8. **密钥红线**：所有密钥不得硬编码、不得进 Git、不得进 Docker 镜像

9. **签名红线**：MCP 调用必须验证 HMAC-SHA256 签名 + 时间戳 + Nonce



---



## 十一、全模块可插拔架构



> 业务代码永远只依赖 Port 接口，适配器按配置自动加载。切换实现不改源码，只改配置。



### 11.1 Port 接口体系（所有外部能力抽象为接口）



```

axiqra-common/src/main/java/com/axiqra/common/

└── port/                              # 所有外部能力抽象为 Port 接口

    ├── storage/                       # 存储（数据库）

    │   ├── DataStoragePort.java       # 通用数据存储（save/find/update/delete）

    │   └── AuditStoragePort.java      # 审计日志存储

    │

    ├── cache/                         # 缓存

    │   ├── CachePort.java             # 通用缓存（get/set/evict）

    │   └── QuotaCounterPort.java      # 配额计数（Redis atomic ops）

    │

    ├── search/                        # 搜索

    │   └── SearchEnginePort.java      # 搜索接口（index/query/delete）

    │

    ├── queue/                          # 消息队列

    │   └── MessageQueuePort.java      # 消息发送/消费（S1 用 Redis Pub/Sub）

    │

    ├── auth/                          # 认证

    │   ├── AuthPort.java             # 登录/登出/验证

    │   └── TokenProviderPort.java    # Token 颁发/验证

    │

    ├── object/                        # 对象存储

    │   └── ObjectStoragePort.java    # 上传/下载/删除/签名URL

    │

    ├── lock/                          # 分布式锁

    │   └── DistributedLockPort.java # 加锁/解锁

    │

    └── security/                      # 安全

        ├── SignatureStrategy.java     # 签名策略

        ├── EncryptionStrategy.java   # 加密策略

        └── PasswordHashStrategy.java # 密码哈希策略

```



### 11.2 适配器体系（按配置自动加载）



```

axiqra-code/

├── axiqra-common/                     # Port 接口定义（所有接口在这里）

│

├── axiqra-adapter/                   # 适配器父模块（pom.xml）

│   ├── axiqra-adapter-redis/        # S1 缓存适配器（实现 CachePort / QuotaCounterPort）

│   ├── axiqra-adapter-postgres/     # PostgreSQL 审计库适配器

│   └── ...

```



### 11.3 可插拔模块清单



| 模块 | S1 实现 | S2+ 可切换 | 切换方式 |

|------|---------|-----------|---------|

| **数据库** | CockroachDB | PostgreSQL / MySQL / Oracle | 改配置 |

| **ORM** | MyBatis-Flex | JPA / MyBatis-Plus | 改依赖 + 配置 |

| **缓存** | Redis | Memcached / Hazelcast | 改配置 |

| **对象存储** | MinIO | AWS S3 / 阿里云 OSS | 改配置 |

| **API 签名** | HMAC-SHA256 | SM2 / ECDSA / RSA-PSS | 改配置 `security.algorithm.api-signature` |

| **字段加密** | AES-256-GCM | SM4-GCM | 改配置 |

| **密码哈希** | BCrypt | Argon2id | 改配置 |

| **JWT** | HS256 | RS256 / ES256 / SM2 | 改配置 |



---



## 十二、并发保护与可靠性体系



> 层层设防，永远不让系统崩溃。宁可拒绝服务，不能让系统雪崩。



### 12.1 并发保护（多层防线）



```

请求进来

    │

    ▼

┌─────────────────┐

│  1. Nginx 网关层  │  ← limit_req_zone 限流

│  (100 QPS/IP)    │  超出 → 429 Too Many Requests

└────────┬────────┘

         ▼

┌─────────────────┐

│  2. Spring 过滤器 │  ← 全局限流 + 熔断

│  (配额强制)        │  超出 → 429 + 记录日志

└────────┬────────┘

         ▼

┌─────────────────┐

│  3. Service 层   │  ← 单接口限流

│  (Resilience4j)  │  超出 → 业务拒绝

└────────┬────────┘

         ▼

┌─────────────────┐

│  4. 数据库层      │  ← 连接池保护

│  (HikariCP)      │  最多 20 连接

└─────────────────┘

```



### 12.2 熔断降级（Resilience4j）



```java

@CircuitBreaker(name = "searchService", fallbackMethod = "searchFallback")

public List<SolutionVO> search(SearchQuery query) {

    return searchEnginePort.query(query);

}



public List<SolutionVO> searchFallback(SearchQuery query, Exception e) {

    log.warn("【搜索】服务熔断，降级返回缓存，error={}", e.getMessage());

    return cachePort.get("search:" + query.getKeyword())

        .map(list -> (List<SolutionVO>) list)

        .orElse(Collections.emptyList());

}

```



### 12.3 数据库连接池保护



```yaml

spring:

  datasource:

    hikari:

      maximum-pool-size: 20           # 最多 20 连接

      minimum-idle: 5                # 最小空闲连接

      connection-timeout: 5000       # 获取连接超时 5s

      idle-timeout: 300000          # 空闲超时 5 分钟

      max-lifetime: 1800000         # 连接最大生命周期 30 分钟

```



### 12.4 容灾体系



| 层次 | 容灾机制 | 自愈方式 |

|------|---------|---------|

| **应用层** | Health Check + 熔断降级 | Docker HEALTHCHECK → 自动重启 |

| **CockroachDB** | 3 节点，数据三副本，自动选举 | 节点挂了 → 自动选主，数据不丢失 |

| **Redis** | 主从 + 哨兵，自动故障转移 | 节点挂了 → 哨兵自动切换 |

| **限流保护** | Bucket4j 配额强制 | 超出系统承载 → 429 拒绝，不崩溃 |

| **幂等设计** | version / idempotency-key | 重试不产生重复数据 |



### 12.5 自愈机制



```yaml

# docker-compose.yml 中的健康检查 + 自愈配置

axiqra_core:

  restart: unless-stopped          # 崩溃自动重启

  healthcheck:

    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]

    interval: 30s

    timeout: 10s

    retries: 3                      # 连续 3 次失败 → 自动重启

    start_period: 60s

```



### 12.6 优雅关闭



```yaml

server:

  shutdown: graceful                    # 收到 SIGTERM → 先停收流量

  tomcat:

    connection-timeout: 5000

```



### 12.7 监控端点



```yaml

management:

  endpoints:

    web:

      exposure:

        include: health,info,metrics,prometheus,logfile

      base-path: /actuator

  endpoint:

    health:

      show-details: always

      probes:

        enabled: true              # liveness + readiness 分开探测

```



---



## 十三、API 接口文档规范



### 13.1 API 接口清单（完整）



| 模块 | 接口 | 方法 | 说明 |

|------|------|------|------|

| **Auth** | `/api/auth/register` | POST | 用户注册 |

| **Auth** | `/api/auth/login` | POST | 用户登录 |

| **Auth** | `/api/auth/logout` | POST | 用户登出 |

| **Auth** | `/api/auth/me` | GET | 获取当前用户信息 |

| **Auth** | `/api/auth/nav` | GET | 累加式导航菜单 |

| **Workspace** | `/api/workspaces` | POST | 创建工作空间 |

| **Workspace** | `/api/workspaces` | GET | 获取用户所有空间 |

| **Workspace** | `/api/workspaces/{id}` | GET | 获取空间详情 |

| **Workspace** | `/api/workspaces/{id}` | PUT | 更新空间 |

| **Workspace** | `/api/workspaces/{id}` | DELETE | 删除空间 |

| **Member** | `/api/workspaces/{id}/members` | POST | 添加成员 |

| **Member** | `/api/workspaces/{id}/members/{userId}` | PUT | 更新成员角色 |

| **Member** | `/api/workspaces/{id}/members/{userId}` | DELETE | 移除成员 |

| **Session** | `/api/sessions` | POST | 创建 Connect 会话 |

| **Session** | `/api/sessions` | GET | 获取会话列表 |

| **Session** | `/api/sessions/{uuid}` | GET | 获取会话详情 |

| **Search** | `/api/search/before-act` | POST | AI Tool 搜索（MCP） |

| **Search** | `/api/search/public` | GET | 公开搜索 |

| **Trace** | `/api/traces` | POST | 提交工程轨迹 |

| **Trace** | `/api/traces/{id}` | GET | 获取轨迹详情 |

| **Trace** | `/api/traces` | GET | 获取轨迹列表 |

| **Solution** | `/api/solutions/{id}` | GET | 获取方案详情 |

| **Solution** | `/api/solutions/{id}/versions` | GET | 获取方案版本历史 |

| **Invocation** | `/api/invocations` | POST | 记录调用结果 |

| **Feedback** | `/api/feedbacks` | POST | 提交反馈 |

| **Review** | `/api/reviews` | GET | 获取待审核列表 |

| **Review** | `/api/reviews/{id}` | POST | 提交审核决策 |

| **Public Case** | `/api/public-cases` | GET | 公开案例列表 |

| **Public Case** | `/api/public-cases/{id}` | GET | 公开案例详情 |

| **Tool Model** | `/api/tool-models/leaderboard` | GET | 工具模型排行榜 |

| **Health** | `/actuator/health` | GET | 健康检查 |

| **Metrics** | `/actuator/metrics` | GET | 监控指标 |



### 13.2 统一错误码



```java

public enum ErrorCode {

    // ========== 通用错误（00xxx）==========

    SUCCESS(0, "成功"),

    PARAM_INVALID(001, "参数错误"),

    SYSTEM_ERROR(999, "系统错误"),



    // ========== 认证错误（01xxx）==========

    AUTH_USERNAME_EXISTS(101, "用户名已存在"),

    AUTH_EMAIL_EXISTS(102, "邮箱已被注册"),

    AUTH_CREDENTIALS_ERROR(103, "用户名或密码错误"),

    AUTH_TOKEN_EXPIRED(104, "Token已过期"),

    AUTH_TOKEN_INVALID(105, "Token无效"),

    AUTH_UNAUTHORIZED(106, "未登录"),

    AUTH_FORBIDDEN(107, "无权限"),



    // ========== 搜索错误（02xxx）==========

    SEARCH_QUOTA_LIMITED(201, "搜索配额已用尽"),

    SEARCH_SIGNATURE_INVALID(202, "API签名无效"),

    SEARCH_TIMESTAMP_EXPIRED(203, "请求时间戳已过期"),

    SEARCH_NONCE_REUSED(204, "Nonce已被使用（重放攻击）"),

    SEARCH_NO_RESULT(205, "未找到匹配的解决方案"),

    SEARCH_ENGINE_ERROR(299, "搜索引擎异常"),



    // ========== Trace 错误（03xxx）==========

    TRACE_SUBMIT_SUCCESS(301, "轨迹提交成功，待审核"),

    TRACE_IDEM_KEY_EXISTS(302, "重复提交（幂等键已存在）"),

    TRACE_NOT_FOUND(303, "轨迹不存在"),

    TRACE_INVALID_WORKSPACE(304, "无权访问此空间"),

    TRACE_REVIEW_PENDING(305, "轨迹正在审核中"),



    // ========== 审核错误（04xxx）==========

    REVIEW_NOT_FOUND(401, "审核记录不存在"),

    REVIEW_ALREADY_DONE(402, "该轨迹已审核"),

    REVIEW_UNAUTHORIZED(403, "无权进行此审核操作"),

    REVIEW_DECISION_INVALID(404, "无效的审核决策"),



    // ========== 配额错误（05xxx）==========

    QUOTA_LIMITED(501, "配额超限，请升级套餐"),

    QUOTA_CHECK_FAILED(502, "配额检查失败"),

    ;

}

```



---



## 十四、测试文档体系



### 14.1 测试金字塔



```

                    ▲

                   /│\        E2E 测试（少量，关键路径）

                  / │ \       端到端验证完整业务流程

                 /  │  \

                /───┼───\     集成测试（适量，接口 + Service）

               /    │    \    测试 Port 接口与外部依赖的集成

              /     │     \

             /──────┼──────\  单元测试（大量，覆盖每个类）

           /        │        \ 测试单个方法的逻辑

         ─────────────────────────────────────────────────────────

        /    覆盖率目标：单元 ≥80% 集成 ≥60% E2E ≥40% 关键路径   \

       ─────────────────────────────────────────────────────────

```



### 14.2 测试目录结构



```

axiqra-project/

└── test-cases/                        # 测试用例归档

    ├── R1-日志安全审计框架/

    │   ├── 01-审核报告/

    │   ├── 02-单元测试/

    │   └── 03-测试用例/

    ├── R3-Auth_Workspace_Nav/

    │   ├── 01-审核报告/

    │   ├── 02-单元测试/

    │   └── 03-测试用例/

    └── ...

```



### 14.3 单元测试规范



```java

@DisplayName("HMAC-SHA256 签名策略测试")

class HmacSignatureStrategyTest {



    private SignatureStrategy signatureStrategy;

    private static final String SECRET = "test-secret-key-256bit!!";



    @BeforeEach

    void setUp() {

        signatureStrategy = new HmacSignatureStrategy();

    }



    @Test

    @DisplayName("签名和验签：正常数据应验签成功")

    void signAndVerify_validData_shouldPass() {

        String payload = "app_123" + System.currentTimeMillis() + "nonce_abc";

        String signature = signatureStrategy.sign(payload, SECRET);



        assertNotNull(signature);

        assertTrue(signature.length() == 64);

        assertTrue(signatureStrategy.verify(payload, signature, SECRET));

    }



    @Test

    @DisplayName("篡改数据：验签应失败")

    void verify_tamperedData_shouldFail() {

        String payload = "original_payload";

        String signature = signatureStrategy.sign(payload, SECRET);



        String tamperedPayload = "tampered_payload";

        assertFalse(signatureStrategy.verify(tamperedPayload, signature, SECRET));

    }

}

```



---



## 十五、开发流程规范（先设计再开发）



### 15.1 开发流程（强制）



```

收到需求

    │

    ▼

1. 设计阶段（必须完成以下文档才能写代码）

    ├── 编写/更新 API 接口文档（OpenAPI 注解）

    ├── 编写测试用例（单元/集成/E2E）

    ├── 评审设计方案（同行 review）

    │

    ▼

2. 测试驱动开发（TDD，不确定时必须）

    ├── 先写测试（确定接口契约）

    ├── 运行测试（失败，符合预期）

    ├── 写实现代码

    ├── 运行测试（通过）

    ├── 重构（测试仍通过）

    │

    ▼

3. 代码实现

    ├── axiqra-common（DO/DTO/VO/枚举/工具类）

    ├── axiqra-core（Service + Mapper）

    ├── axiqra-api（Controller + 配置）

    │

    ▼

4. 代码审查（PR 必须通过）

    ├── 单元测试覆盖率 ≥80%

    ├── 所有测试通过

    ├── 阿里开发手册检查通过

    ├── 安全检查通过（不引入硬编码密钥/SQL 注入/XSS）

    │

    ▼

5. 合并到主分支

    └── CI 自动运行完整测试套件

```



### 15.2 PR 合并前检查清单



```

PR 检查清单（每次合并必须通过）：

[ ] 所有单元测试通过（mvn test）

[ ] 所有集成测试通过（mvn verify）

[ ] 单元测试覆盖率 ≥80%

[ ] API 文档已更新（Knife4j 可访问）

[ ] 无硬编码密钥/密码

[ ] 无 SQL 注入风险（全部参数化查询）

[ ] 新增代码符合阿里开发手册规范

[ ] 日志规范符合要求（info/warn/error 分级）

[ ] 所有 Port 接口已抽象（不直接依赖实现类）

[ ] 测试用例和测试代码已保留

```



---



## 十六、关键原则（红线）



> **红线是高压线，永不违反，违反即事故。**



```

1.  配额红线：第 11 次 MCP 搜索必须返回 HTTP 429 + QUOTA_LIMITED，不得静默降级

2.  脱敏红线：Trace 提交时使用 Entity 监听器自动脱敏 api_key/token/password

3.  审计红线：登录、搜索、提交、审核必须写入独立 PostgreSQL 审计库

4.  权限红线：搜索结果在召回前过滤，不走后过滤

5.  建表红线：所有表必须包含 id, gmt_create, gmt_modified 三字段

6.  更新红线：所有 UPDATE 操作必须同步更新 gmt_modified

7.  日志红线：所有业务方法必须有 info/warn/error 分级日志，不得空白日志

8.  密钥红线：所有密钥不得硬编码、不得进 Git、不得进 Docker 镜像

9.  签名红线：MCP 调用必须验证 HMAC-SHA256 签名 + 时间戳 + Nonce

10. 算法红线：所有算法必须走策略接口，禁硬编码；只用白名单算法；国密预留 S2 可切换

11. 熔断红线：任何外部调用必须加熔断，否则单点故障拖垮全系统

12. 限流红线：所有对外接口必须限流，宁可返回 429，不能让系统崩溃

13. 超时红线：所有外部调用必须设 timeout（HTTP/DB/Redis）

14. 资源红线：线程池/连接池必须有上限，禁止无限制创建

15. 重启红线：所有容器必须 restart: unless-stopped，崩溃必须自愈

16. 健康检查红线：所有容器必须 HEALTHCHECK，死了必须被发现

17. 幂等红线：所有写操作必须幂等，重试不产生重复数据

18. 优雅关闭红线：收到 SIGTERM → 先停收流量 → 等现有请求处理完 → 再退出

19. 监控红线：所有组件状态必须可观测，死了必须有告警

20. 文档红线：所有 API 必须有完整文档；所有测试用例必须保留；先设计再开发

```



---



## 十七、模块化说明（对齐 D10/D11）



```

- 当前：Maven 多模块单体（axiqra-common + axiqra-core + axiqra-api + axiqra-start）

- S2 拆分：每个 axiqra-core 子模块 → 独立微服务（search-service、trace-service 等）

- S2 引入：Spring Cloud（Circuit Breaker、Config Center、OpenFeign）

- 算法演进：S2 可一键切换国密套件，配置改一行代码零改动

- 适配器演进：S2 可一键切换数据库/缓存/存储/消息队列，配置改一行代码零改动

- 原则：服务拆分由指标触发，不是由代码量触发

```



---



## 十八、开发轮次说明（先地基后盖房，一层一层推进）



> **核心理念**：像盖房子一样，先打地基（Round 1-2），再盖第一层（Round 3），再盖第二层（Round 4），依此类推。地基不稳，上层必塌。



### 18.1 Round 架构说明



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



### 18.2 依赖关系图



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



### 18.3 前端配置化原则



> **所有前端配置（路由、菜单、权限码、页面元数据）必须从后端 API 获取，不写死值。**



| 前端内容 | 后端 API 来源 | 说明 |

|---------|-------------|------|

| 菜单项 | `GET /api/auth/nav` | 累加式，返回用户可见菜单 |

| 权限码 | `GET /api/auth/permissions` | RBAC 权限矩阵 |

| 页面路由 | `GET /api/config/routes` | 动态路由注册 |

| 页面元数据 | `GET /api/config/pages` | 页面标题/图标/权限要求 |

| 枚举值 | `GET /api/config/enums` | RiskLevel/VerificationLevel 等 |

| 搜索字段 | `GET /api/config/search-fields` | 搜索配置化 |



### 18.4 WBS 详细对照表



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

| R6A | WBS-016 | Trace 模块 | REQ-TRC-001/003 | ✅ 已完成 |

| R6B | WBS-017 | Project Case 模块 | REQ-TRC-002 | ✅ 已完成 |

| R6C | WBS-018 | Public Case 模块 | REQ-CAS-001~003 | ✅ 已完成 |

| R7A | WBS-015 | Invocation + Feedback | REQ-SOL-004 | ✅ 已完成 |

| R7B | WBS-019/020 | Review 模块 | REQ-GOV-001~003 | ✅ 已完成 |

| R7C | WBS-021 | Contribution 账本 + 积分 | REQ-CON-001 | ✅ 已完成 |

| R7D | WBS-026 | 工具模型归因 + 排行榜 | REQ-RANK-001/002 | ✅ 已完成 |

| R8A | WBS-023 | 前端页面（16 页面，配置化） | 画面一览 | 进行中 |

| R8B | WBS-024 | 单元/集成/E2E + 覆盖率 | 测试验收 | 待开发 |

| R8C | WBS-025 | S1 Gate + 13 红线 | 05-03 | 待开发 |



---



## 十九、GitHub Actions CI 流水线



> **CI 是自动门卫，每次 push 和 PR 都强制运行。测试不通过，禁止合并。**



### 19.1 CI 流程



```

开发者 push / PR

    │

    ▼

GitHub Actions 触发

    │

    ├── 1. 拉取代码

    ├── 2. 安装 Maven 依赖

    ├── 3. 运行单元测试（mvn test）

    ├── 4. 运行集成测试（mvn verify）

    ├── 5. 生成覆盖率报告（mvn test jacoco:report）

    ├── 6. 检查覆盖率（≥80%）

    │

    ▼

结果判定

    ├─ 全部通过 → PR 可以合并（✅ green）

    └─ 任何一项失败 → PR 禁止合并（❌ red + 失败原因）

```



### 19.2 GitHub Branch Protection 设置



```

main 分支必须设置：

  ✅ Require a pull request before merging（必须 PR）

  ✅ Require status checks to pass before merging（必须通过所有 CI job）

  ✅ Required status checks: unit-tests, integration-tests, coverage, build

  ✅ Do not allow bypassing the above rules（禁止管理员跳过）

```



### 19.3 CI 失败时的处理



```

CI 失败通知：

  → GitHub PR 页面显示红色 X

  → 开发者收到 GitHub 邮件通知

  → 开发者修复代码

  → 新 commit → 自动触发 CI

  → 直到 CI 全部 green 才能继续



禁止：

  ✗ 手动 override CI 结果强制合并

  ✗ 修改 CI 配置让测试通过（作弊）

  ✗ 跳过某些测试 job

```



---



## 待办事项



- [x] PR #28 合入 main

- [x] `test-cases/R5-*/` 测试用例目录（执行记录、覆盖率说明、审核报告）

- [x] `docs/R5-启动与计划同步说明.md` 更新 R5 完成状态

- [x] `ROADMAP.md` 更新 Phase 描述（对应 S1 MVP 范围）

- [x] R6 开发（Trace + Project Case + Public Case）

- [x] R7 开发（Feedback + Review + Contribution + 排行榜）

- [ ] R8 开发（前端 + 测试 + S1 Gate 验收）



---



## 下一步



R7 合入 main 后，进入 R8 阶段：



- axiqra-frontend Vue 3 前端（16 页面，路由/菜单/权限码全部从后端 API 获取配置化渲染）

- 测试体系（单元测试 + 集成测试 + E2E 测试 + 覆盖率报告 ≥80%）

- S1 Gate 验收（Gate 1-5 逐个通过 + 缺陷关闭 + 13 项红线验证）



