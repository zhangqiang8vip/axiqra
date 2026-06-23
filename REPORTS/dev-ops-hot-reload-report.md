# Axiqra 开发运营评估报告

> 分支: `feature/dev-ops-hot-reload` | 日期: 2026-06-23 | JDK 17 | Maven 3.9.9

---

## 任务 1：Swagger / OpenAPI 接口盘点

### 一、Swagger 总览

| 项 | 结果 |
|---|---|
| **Swagger / OpenAPI 实现** | Knife4j 4.5.0 + Springdoc OpenAPI (共存) |
| **Knife4j 版本** | 4.5.0 (pom.xml: `knife4j.version`) |
| **Springdoc 版本** | 由 Spring Boot 3.5.14 BOM 管理（间接依赖） |
| **Swagger JSON 地址** | `/api/v3/api-docs` (context-path: `/api`) |
| **Knife4j UI 地址** | `/api/doc.html` (Knife4j 特有) |
| **Springdoc UI 地址** | `/api/swagger-ui/index.html` (springdoc.swagger-ui.enabled=false, 实际禁用) |
| **接口总数** | **37 个** (按 HTTP Method + Path 去重) |
| **公开接口数** | **2 个** (`/internal/health`, `/internal/health/verify`) |
| **需要登录接口数** | **37 个** (全部需要 Sa-Token 登录) |
| **需要 scope / role 接口数** | **19 个** (含 `@RequireScope` 注解) |
| **静态扫描还是运行时扫描** | **静态扫描** (项目无法启动，依赖服务未配置) |
| **无法确认项** | 1) Swagger UI 无法访问，Knife4j UI 需运行时确认<br>2) `/api/v3/api-docs` 实际内容需运行时确认<br>3) OpenAPI security scheme 配置是否完整<br>4) `InvocationController` 的 `/api/v1/invocations` 等接口与 context-path `/api` 的路径叠加情况 |

### 二、接口明细表

> 扫描方式：静态代码扫描（Controller 注解 + `@Operation` + `@RequireScope` + `@RequireWorkspaceRole`）
> 路径前缀说明：部分 Controller 使用 `@RequestMapping("/api/v1/...")` 会与 context-path `/api` 叠加，导致实际路径为 `/api/api/v1/...`，这是当前代码中的路径定义异常。

| 序号 | Method | Path | Controller | 认证要求 | Scope / Role | 用途说明 | 请求对象 | 响应对象 | 错误码 | Swagger暴露 | 备注 |
|---:|--------|------|-----------|---------|-------------|--------|--------|--------|------|-----------|---|
| 1 | GET | `/internal/health` | HealthController | 公开 | - | 健康检查主端点 | - | Map | - | 是 | 无任何认证 |
| 2 | GET | `/internal/health/verify` | HealthController | 公开 | - | 依赖服务深度检测（DB/Redis/Rabbit/MQ/MinIO） | - | Map | - | 部分 | 无 @Operation 注解 |
| 3 | GET | `/api/connect/quota` | ConnectController | 需登录 | - | 查询当前用户配额状态 | - | QuotaStatusVO | 401 | 是 | StpUtil.getLoginIdAsLong() |
| 4 | GET | `/api/connect/rate-limit` | ConnectController | 需登录 | - | 检查当前限流窗口，返回 Retry-After | - | RateLimitStatusVO | 401 | 是 | 含自定义响应头 |
| 5 | GET | `/api/connect/doctor` | ConnectController | 需登录 | - | 执行 Connect doctor 八项检查 | channel, toolType, workspaceId(可选) | ConnectDoctorVO | 401 | 是 | 参数有白名单校验 |
| 6 | GET | `/api/connect/sessions` | ConnectController | 需登录 | - | 查询我的 Connect 会话列表 | - | List\<ConnectSessionVO\> | 401 | 是 | - |
| 7 | GET | `/api/connect/sessions/{sessionId}` | ConnectController | 需登录 | - | 查询 Connect 会话详情 | sessionId(path) | ConnectSessionVO | 401 | 是 | - |
| 8 | POST | `/api/connect/sessions` | ConnectController | 需登录 | - | 创建 Connect 会话（含限流检查） | ConnectSessionCreateRequest | ConnectSessionVO | 401 | 是 | 返回 201 |
| 9 | POST | `/api/search/before-act` | SearchController | 需登录 | - | 搜索前权限预过滤与多路召回 | SearchRequest | SearchResponseVO | 401 | 是 | 文档草案接口，存在 |
| 10 | GET | `/api/solutions/{solutionId}` | SolutionController | 需登录 | - | 获取 Solution 详情（含版本与反馈统计） | solutionId(path) | SolutionDetailVO | 401 | 是 | 文档草案接口，存在 |
| 11 | POST | `/api/traces` | TraceController | 需登录 | - | 创建 Trace 草稿 | TraceCreateRequest | TraceDetailVO | 401 | 是 | 文档草案接口，存在 |
| 12 | POST | `/api/traces/{traceId}/confirm` | TraceController | 需登录 | - | 确认 Trace 执行结果 | traceId(path) + TraceConfirmRequest | TraceDetailVO | 401 | 是 | - |
| 13 | POST | `/api/traces/{traceId}/submit` | TraceController | 需登录 | - | 提交 Trace 进入审核 | traceId(path) | TraceDetailVO | 401 | 是 | - |
| 14 | GET | `/api/traces/{traceId}` | TraceController | 需登录 | - | 获取 Trace 详情 | traceId(path) | TraceDetailVO | 401 | 是 | - |
| 15 | POST | `/api/project-cases` | ProjectCaseController | 需登录 | - | 从 Trace 创建 Project Case | ProjectCaseCreateRequest | ProjectCaseDetailVO | 401 | 是 | - |
| 16 | GET | `/api/project-cases/{caseId}` | ProjectCaseController | 需登录 | - | 获取 Project Case 详情 | caseId(path) | ProjectCaseDetailVO | 401 | 是 | - |
| 17 | POST | `/api/project-cases/{caseId}/publish-request` | ProjectCaseController | 需登录 | - | 发起 Project Case 发布申请（需 workspace OWNER 授权） | caseId(path) + authorizationId(query) | ProjectCaseDetailVO | 401 | 是 | 文档草案接口，存在 |
| 18 | POST | `/api/public-cases/publish/{projectCaseId}` | PublicCaseController | 需登录 | - | 将 Project Case 发布为 Public Case | projectCaseId(path) | PublicCaseDetailVO | 401 | 是 | 路径含 publish，与草案命名不同 |
| 19 | GET | `/api/public-cases/{publicCaseId}` | PublicCaseController | 需登录 | - | 获取 Public Case 详情 | publicCaseId(path) | PublicCaseDetailVO | 401 | 是 | - |
| 20 | GET | `/api/public-cases` | PublicCaseController | 需登录 | - | 列出公开可读的 Public Case（含 ABAC 过滤） | limit(query, 可选) | List\<PublicCaseDetailVO\> | 401 | 是 | - |
| 21 | POST | `/api/invocations` | InvocationController | 需登录 | `connect:write` | 上报 AI 工具调用结果 | InvocationReportRequest | InvocationDetailVO | 401,403 | 是 | 路径前缀异常(见注) |
| 22 | GET | `/api/invocations/{invocationId}` | InvocationController | 需登录 | `connect:read` | 获取调用详情 | invocationId(path) | InvocationDetailVO | 401,403 | 是 | 路径前缀异常(注) |
| 23 | GET | `/api/invocations/solutions/{solutionId}/feedback-stats` | InvocationController | 需登录 | `feedback:read` | 获取指定 Solution 的反馈统计 | solutionId(path) | SolutionFeedbackStatsVO | 401,403 | 是 | 路径前缀异常(注) |
| 24 | POST | `/api/feedbacks` | FeedbackController | 需登录 | `feedback:write` | 提交反馈 | FeedbackSubmitRequest | FeedbackDetailVO | 401,403 | 是 | 路径前缀异常(注) |
| 25 | GET | `/api/feedbacks` | FeedbackController | 需登录 | `feedback:read` | 获取反馈列表 | targetType, targetId(query) | List\<FeedbackDetailVO\> | 401,403 | 是 | 路径前缀异常(注) |
| 26 | GET | `/api/feedbacks/solutions/{solutionId}/stats` | FeedbackController | 需登录 | `feedback:read` | 获取 Solution 反馈统计 | solutionId(path) | SolutionFeedbackStatsVO | 401,403 | 是 | 路径前缀异常(注) |
| 27 | GET | `/api/contributions/users/{userId}/summary` | ContributionController | 需登录 | `contribution:read` | 获取用户贡献汇总 | userId(path) | ContributionSummaryVO | 401,403 | 是 | 路径前缀异常(注) |
| 28 | GET | `/api/contributions/users/{userId}/records` | ContributionController | 需登录 | `contribution:read` | 获取用户贡献记录 | userId(path) + limit(query) | List\<ContributionRecord\> | 401,403 | 是 | 路径前缀异常(注) |
| 29 | GET | `/api/reviews/pending` | ReviewController | 需登录 | `review:read` | 获取待审核队列（HUMAN/AUTO/QUARANTINED） | queue, limit(query) | List\<ReviewDetailVO\> | 401,403 | 是 | 路径前缀异常(注) |
| 30 | GET | `/api/reviews/{reviewId}` | ReviewController | 需登录 | `review:read` | 获取审核详情 | reviewId(path) | ReviewDetailVO | 401,403 | 是 | 路径前缀异常(注) |
| 31 | POST | `/api/reviews/{reviewId}/approve` | ReviewController | 需登录 | `review:write` | 审核通过 | reviewId(path) + reasonCode, notes(query) | ReviewDetailVO | 401,403 | 是 | 路径前缀异常(注) |
| 32 | POST | `/api/reviews/{reviewId}/reject` | ReviewController | 需登录 | `review:write` | 审核拒绝 | reviewId(path) + reasonCode, notes(query) | ReviewDetailVO | 401,403 | 是 | 路径前缀异常(注) |
| 33 | POST | `/api/reviews/{reviewId}/quarantine` | ReviewController | 需登录 | `review:write` | 隔离内容 | reviewId(path) + reasonCode, notes(query) | ReviewDetailVO | 401,403 | 是 | 路径前缀异常(注) |
| 34 | POST | `/api/reviews/{reviewId}/appeal` | ReviewController | 需登录 | `review:write` | 申诉 | reviewId(path) + appealContent(query) | ReviewDetailVO | 401,403 | 是 | 路径前缀异常(注) |
| 35 | GET | `/api/policy/evaluate` | PolicyController | 需登录 | - | 策略评估（ABAC，注入当前用户） | PolicyEvaluationRequest | PolicyEvaluationVO | 401 | 是 | 高风险：subjectId 被拦截器覆盖 |
| 36 | POST | `/api/policy/enforce` | PolicyController | 需登录 | `admin:all` | 强制策略评估，高风险操作最终校验 | PolicyEvaluationRequest | Void | 401,403 | 是 | - |
| 37 | GET | `/api/policy/check` | PolicyController | 需登录 | - | 快速权限检查，当前用户是否持有指定 scope | scope(query) | Boolean | 401 | 是 | - |
| 38 | GET | `/api/workspaces` | WorkspaceController | 需登录 | - | 我的工作空间列表 | - | PageResponse\<WorkspaceVO\> | 401 | 是 | - |
| 39 | POST | `/api/workspaces` | WorkspaceController | 需登录 | - | 创建工作空间（自动设自己为 owner） | WorkspaceCreateRequest | WorkspaceVO | 401 | 是 | 返回 201 |
| 40 | GET | `/api/workspaces/{workspaceId}` | WorkspaceController | 需登录 | - | 工作空间详情 | workspaceId(path) | WorkspaceVO | 401 | 是 | - |
| 41 | PUT | `/api/workspaces/{workspaceId}` | WorkspaceController | 需登录 | `WorkspaceRole.OWNER` | 更新工作空间 | workspaceId(path) + WorkspaceUpdateRequest | WorkspaceVO | 401,403 | 是 | Workspace 角色校验 |
| 42 | DELETE | `/api/workspaces/{workspaceId}` | WorkspaceController | 需登录 | `WorkspaceRole.OWNER` | 删除工作空间 | workspaceId(path) | Void(204) | 401,403 | 是 | Workspace 角色校验 |
| 43 | GET | `/api/workspaces/{workspaceId}/members` | WorkspaceController | 需登录 | - | 成员列表 | workspaceId(path) | PageResponse\<MemberVO\> | 401 | 是 | - |
| 44 | POST | `/api/workspaces/{workspaceId}/members` | WorkspaceController | 需登录 | `WorkspaceRole.ADMIN` | 添加成员 | workspaceId + userId, role(query) | MemberVO | 401,403 | 是 | Workspace 角色校验 |
| 45 | PUT | `/api/workspaces/{workspaceId}/members` | WorkspaceController | 需登录 | `WorkspaceRole.OWNER` | 更新成员角色 | workspaceId(path) + MemberRoleUpdateRequest | Void(204) | 401,403 | 是 | Workspace 角色校验 |
| 46 | DELETE | `/api/workspaces/{workspaceId}/members/{memberId}` | WorkspaceController | 需登录 | `WorkspaceRole.ADMIN` | 移除成员 | workspaceId + memberId(path) | Void(204) | 401,403 | 是 | Workspace 角色校验 |
| 47 | GET | `/api/users/me` | UserController | 需登录 | - | 获取当前用户信息 | - | UserInfoVO | 401 | 是 | - |
| 48 | GET | `/api/users/{userId}` | UserController | 需登录 | - | 获取指定用户公开信息（不含隐私字段） | userId(path) | UserPublicVO | 401 | 是 | - |
| 49 | GET | `/api/auth/nav` | NavController | 需登录 | - | 获取累加式导航菜单 | - | NavResponseVO | 401 | 是 | 路径含 /auth，与草案命名不同 |
| 50 | GET | `/api/tool-models/leaderboard` | ToolModelController | 需登录 | `public:read` | 获取工具模型排行榜 | scopeType, scopeId, toolName, limit(query) | List\<ToolModelLeaderboardVO\> | 401,403 | 是 | - |

> **注**: `InvocationController`, `FeedbackController`, `ContributionController`, `ReviewController` 的 `@RequestMapping` 为 `/api/v1/...`，与 `context-path: /api` 叠加后实际路径为 `/api/api/v1/...`，这是明显的路径定义错误（应直接用 `/v1/...`）。`ToolModelController` 同样有 `/api/v1/tool-models` 的叠加问题。

### 三、按业务模块汇总

| 模块 | 接口数 | 主要用途 | 认证特点 | 风险 |
|------|------:|--------|--------|------|
| **Connect（连接与会话）** | 6 | Quota 查询、限流探测、Doctor 检测、会话管理 | 全部需 Sa-Token 登录，无额外 scope | 中：rate-limit 返回 Retry-After 可能被滥用 |
| **Search（搜索）** | 1 | 任务前搜索权限预过滤与多路召回 | 需登录，无 scope | 低 |
| **Solution（方案）** | 1 | 获取方案详情 | 需登录，无 scope | 低 |
| **Trace（工程轨迹）** | 4 | 创建、确认、提交、查询工程轨迹 | 需登录，无 scope | 低 |
| **Project Case（私有案例）** | 3 | 从 Trace 创建 Case、查询、发布申请 | 需登录，publish-request 需 workspace OWNER | 中：publish-request 含 workspace 授权 ID 参数，需防止越权 |
| **Public Case（公开案例）** | 3 | 发布、详情、列表 | 需登录，无 scope（ABAC 层过滤可见性） | 中：list 接口应考虑是否需要 scope |
| **Invocation（调用记录）** | 3 | 上报调用、查询详情、反馈统计 | 需登录 + scope（`connect:write/read`, `feedback:read`） | 中：路径叠加异常，需修正 |
| **Feedback（反馈）** | 3 | 提交反馈、列表、统计 | 需登录 + scope（`feedback:write/read`） | 中：路径叠加异常，需修正 |
| **Contribution（贡献）** | 2 | 用户贡献汇总与记录 | 需登录 + scope（`contribution:read`） | 中：路径叠加异常，需修正 |
| **Review（审核）** | 6 | 待审队列、详情、审批/拒绝/隔离/申诉 | 需登录 + scope（`review:read/write`） | 中：路径叠加异常；审核操作需记录审计日志 |
| **Policy（策略引擎）** | 3 | 策略评估、强制评估、权限检查 | 需登录，enforce 需 `admin:all` scope | 高：enforce 是高风险操作，已有 scope 保护 |
| **Workspace（工作空间）** | 9 | CRUD、成员管理 | 需登录，部分接口有 Workspace 角色校验（OWNER/ADMIN） | 中：角色校验依赖 RbacService，需确保 ABAC 层一致性 |
| **User（用户）** | 2 | 当前用户信息、用户公开信息 | 需登录 | 低 |
| **Nav（导航）** | 1 | 累加式菜单 | 需 Sa-Token 登录（`@SaCheckLogin`） | 低 |
| **ToolModel（工具模型）** | 1 | 排行榜 | 需登录 + scope（`public:read`） | 低 |
| **Health（健康检查）** | 2 | 健康检查 | **公开，无认证** | 低（内部网络使用） |
| **Actuator** | ~4 | health, info, metrics, prometheus | `/actuator/health/**` 和 `/actuator/info` 公开 | 低（已在 Sa-TokenConfig 中排除） |

### 四、与 Axiqra 文档草案对比

| 文档接口 | 当前 Swagger 是否存在 | 当前代码是否存在 | 差异 | 建议动作 |
|---------|:---------:|:---------:|------|---------|
| `POST /api/connect/sessions` | 是 | 是 | 完整实现 | 无 |
| `POST /api/connect/doctor` | 是（GET） | 是 | 草案为 POST，当前为 GET；草案参数与当前一致 | 确认为 GET 后同步草案文档 |
| `POST /api/search/before-act` | 是 | 是 | 完整实现 | 无 |
| `GET /api/solutions/{id}` | 是 | 是 | 完整实现 | 无 |
| `POST /api/traces` | 是 | 是 | 完整实现 | 无 |
| `POST /api/project-cases/{id}/publish-request` | 是 | 是 | 完整实现，额外参数 `authorizationId` | 无 |
| `POST /api/invocations/{id}/feedback` | **否** | **部分** | 草案：`POST /api/invocations/{id}/feedback`<br>当前：`POST /api/feedbacks`，在 FeedbackController | 重构：`invocations/{id}/feedback` 应映射到 `InvocationController` 或保持现状（当前设计更 RESTful，按 feedback 资源建模） |
| `POST /api/candidate-seeds` | **否** | **否** | 文档草案中提及，当前代码中无对应 Controller | 需新增 `CandidateSeedController`（代码中已有 `CandidateSeedServiceImpl`，说明后端已实现，Controller 缺失） |

**CandidateSeed 缺口说明**：代码中存在 `com.axiqra.core.service.impl.CandidateSeedServiceImpl`，说明后端业务逻辑已实现，但缺少对应的 REST Controller 暴露接口。这是一个需要立即补充的缺口。

### 五、结论

**1. 当前 Swagger 一共有多少接口？**

静态扫描共 **50 个** Controller 接口（不含 Actuator 端点）+ 约 4 个 Actuator 端点（health, info, metrics, prometheus），合计约 **54 个**。实际数量需运行时 `/api/v3/api-docs` 确认（路径叠加异常可能导致 OpenAPI 扫描结果与预期不符）。

**2. 哪些接口需要认证？**

- **公开接口（2 个）**：`/internal/health`、`/internal/health/verify`
- **需要 Sa-Token 登录（48 个）**：全部业务接口
- **需要 scope（19 个）**：含 `@RequireScope` 注解的接口（connect:write/read, feedback:write/read, contribution:read, review:read/write, public:read, admin:all）
- **需要 Workspace 角色（4 个）**：含 `@RequireWorkspaceRole` 的接口（OWNER: 3个, ADMIN: 1个）

**3. 每个接口分别是干什么的？**

见上述"接口明细表"用途说明列和"按业务模块汇总"。

**4. Swagger 配置是否需要修正？**

**需要修正**，主要问题：

1. **路径叠加异常**：`InvocationController`/`FeedbackController`/`ContributionController`/`ReviewController`/`ToolModelController` 的 `@RequestMapping` 使用 `/api/v1/...`，与 context-path `/api` 叠加后实际路径为 `/api/api/v1/...`。应改为 `/v1/...`。
2. **Knife4j 与 Springdoc 并存但 Springdoc UI 已禁用**：`springdoc.swagger-ui.enabled=false` 但仍引入 springdoc 依赖，造成冗余。建议统一使用 Knife4j 或移除 springdoc 依赖。
3. **OpenAPI Security Scheme 未配置**：`application.yml` 中有 `sa-token` 配置，但无对应 OpenAPI security scheme 注解或配置类声明 `Authorization` Bearer scheme，导致 Knife4j UI 不显示"Authorize"按钮。
4. **`/internal/health/verify` 缺少 `@Operation` 注解**：接口文档不完整。
5. **无全局 `securitySchemes` 配置**：Swagger 文档未声明认证方式，客户端无法知道需要传递什么凭证。

**5. 是否存在未受保护但应该受保护的接口？**

**无**。所有业务接口（48个）均需要 Sa-Token 登录。`/internal/health` 和 `/internal/health/verify` 为内部健康检查，部署在 K8s 内部网络，风险可控。

**6. 是否存在应该公开但被错误拦截的接口？**

**需要确认**：`ToolModelController.getLeaderboard` 要求 `public:read` scope，但排行榜本意是公开数据。该接口设计意图需要产品确认：
- 如果排行榜确实是公开的（类似 GitHub Trending），应去掉 scope 要求，改为"登录后可查看"或直接公开。
- 如果排行榜需要记录查看行为，应保留 `public:read` scope。

---

## 任务 2：热部署 / 热更新开发方式

### 一、官方资料核查

| 技术 | 官方资料 | 当前项目版本 | 是否适用 | 备注 |
|------|---------|-----------|--------|------|
| **Spring Boot DevTools** | [Spring Boot 3.5 Reference - DevTools](https://docs.spring.io/spring-boot/reference/using/devtools.html) | 未引入（pom.xml 无 `spring-boot-devtools` 依赖） | **适用** | 仅需加依赖即可，无需额外配置 |
| **Spring Boot DevTools Restart** | 同上 | - | 适用 | DevTools 默认重启 ClassLoader + 静态资源热更新 |
| **Spring Boot LiveReload** | 同上 | - | 适用（前端） | DevTools 内置 Livereload server (35729)，需浏览器插件 |
| **Maven Continuous Build** | [mvn compile + 插件](https://maven.apache.org/plugins/) | Maven 3.9.9 | 部分适用 | 适合后端编译，前端需另配 |
| **JRebel** | [JRebel Official](https://www.jrebel.com/) | 未引入 | 可选商业方案 | 不默认引入 |
| **Docker Compose Develop / Watch** | [Docker Docs - Compose File Spec](https://docs.docker.com/compose/compose-file/08-config-files/) | Docker Compose v2 | 适用（Docker 环境开发） | 需 Docker Desktop 4.x+ |
| **Vite HMR** | [Vite Official - HMR](https://vite.dev/guide/hmr.html) | 项目无前端（无 package.json） | **不适用（当前）** | 前端未交付，可作为规划方案 |
| **Vue 3 HMR** | [Vue Official - HMR](https://vuejs.org/guide/extras/hmr.html) | 同上 | 不适用（当前） | 同上 |
| **pnpm / npm dev server** | [pnpm](https://pnpm.io/) / [npm](https://docs.npmjs.com/) | 同上 | 不适用（当前） | 同上 |
| **Docker bind mount** | [Docker - Bind mounts](https://docs.docker.com/engine/storage/bind-mounts/) | Docker Compose 已配置 volumes | 适用 | 基础设施服务数据持久化，代码开发用 watch |
| **IntelliJ IDEA Auto-Compile** | [IntelliJ IDEA - Build Project](https://www.jetbrains.com/help/idea/compiling-applications.html) | - | 适用（IDE 开发模式） | 可配合 DevTools |

### 二、热部署方案对比

| 方案 | 后端热部署 | 前端热更新 | 依赖服务 | 优点 | 缺点 | 推荐场景 |
|------|---------|---------|--------|------|------|---------|
| **A: 本机运行 + Docker 依赖** | Spring Boot DevTools（`spring-boot-devtools` 依赖）| 无（当前无前端） | Docker Compose 启动 PostgreSQL/Redis/Rabbit/MinIO | 零配置；启动快；本地调试方便 | 需手动重启两次（首次 classloader 问题）；Docker 容器需单独管理 | **当前阶段首选（当前项目状态）** |
| **B: Docker Compose Watch / bind mount** | `docker compose watch` + bind mount 源码目录到容器内 | 同上 + 容器内 Vite dev server | 全部容器化 | 前后端统一管理；环境一致性好；无需本地安装 JDK/Node | Docker 性能开销；文件监听有延迟（~1-2s）；容器内构建资源占用 | 团队协作；CI/CD 集成；Windows 文件系统性能差 |
| **C: IDE 开发模式** | IntelliJ IDEA 自动编译 + DevTools | Vite HMR（未来前端） | 本地服务或 Docker | 开发体验最佳；断点调试；代码补全 | IDE 许可成本；配置复杂；团队一致性差 | 个人开发者；深度调试 |

### 三、推荐方案

**推荐方案：A（当前阶段）+ B（未来方向）**

#### 当前阶段（A 方案：Spring Boot DevTools + 本机运行）

**适用开发者**：全栈工程师、本地调试优先者  
**启动命令**：

```bash
# 1. 启动依赖服务
cd axiqra-project/axiqra-infra
docker compose up -d

# 2. 设置环境变量（必需）
$env:DB_PASSWORD="your_db_password"
$env:REDIS_PASSWORD="your_redis_password"
$env:AUDIT_APP_DB_USER="axiqra_audit_app"
$env:AUDIT_DB_PASSWORD="your_audit_password"
$env:AUDIT_APP_DB_PASSWORD="your_audit_app_password"
$env:RABBITMQ_DEFAULT_USER="guest"
$env:RABBITMQ_DEFAULT_PASS="guest"
$env:JWT_SECRET="your-jwt-secret-min-32-chars"
$env:FIELD_ENCRYPT_KEY="your-32-char-encryption-key"
$env:MINIO_ACCESS_KEY="minioadmin"
$env:MINIO_SECRET_KEY="minioadmin"

# 3. 启动后端（DevTools 自动热加载）
cd axiqra-project/axiqra-code
D:\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run -pl axiqra-start -am -Dspring-boot.run.profiles=dev
```

**必要配置文件**：`pom.xml`（需加 DevTools 依赖）

#### 未来方向（B 方案：Docker Compose Watch）

**适用开发者**：团队协作、追求环境一致性  
**Docker Compose Watch 配置**（需新增到 `axiqra-infra/docker-compose.yml`）：

```yaml
services:
  axiqra_core:
    build:
      context: ../axiqra-code
      dockerfile: axiqra-start/Dockerfile
    develop:
      watch:
        - path: ../axiqra-code/axiqra-api/src
          action: rebuild
          target: /app/classes
        - path: ../axiqra-code/axiqra-core/src
          action: rebuild
          target: /app/classes
        - path: ../axiqra-code/axiqra-common/src
          action: rebuild
          target: /app/classes
    # ... 其余配置见现有 docker-compose.yml
```

**启动命令**：

```bash
cd axiqra-project/axiqra-infra
docker compose watch
# 或
docker compose up --watch
```

### 四、需要修改的文件清单

| 文件 | 修改内容 | 是否框架配置 | 风险 |
|------|---------|-----------|------|
| `axiqra-start/pom.xml` | 新增 `spring-boot-devtools` 依赖 | **是（框架配置）** | 低：DevTools 依赖不会进入生产打包 |
| `axiqra-infra/docker-compose.yml` | 新增 `develop: watch` 配置和 `axiqra_core` 服务定义 | 部分（框架配置） | 中：需取消 `axiqra_core` 服务的注释并修正 build context |
| `axiqra-start/Dockerfile` | 可能需要调整 WORKDIR 以支持 watch target | 部分 | 低：当前 Dockerfile 未查看，需确认 |
| `axiqra-project/axiqra-infra/.env` | 需创建包含所有必需环境变量 | 是（环境配置） | 低 |

### 五、验收标准

| 编号 | Given | When | Then |
|------|-------|------|------|
| AC-HOT-001 | 开发者启动本地开发环境 | 修改后端 Controller 或 Service | 服务自动重启或无需手动重建镜像即可生效 |
| AC-HOT-002 | 开发者启动前端 dev server | 修改 Vue 页面或组件 | 浏览器自动 HMR 更新 |
| AC-HOT-003 | Docker 依赖服务已启动 | 重启后端或前端 | PostgreSQL、Redis、MinIO 数据不丢失 |
| AC-HOT-004 | 热部署失败 | 查看控制台日志 | 能看到明确错误和修复提示 |

---

## 任务 3：框架优先、配置优先的实现审查

### 一、框架优先审查表

| 位置 | 当前做法 | 推荐框架 / 配置 | 官方依据 | 是否建议改 | 原因 | 风险 |
|------|--------|----------|---------|---------|------|------|
| **API 文档（Swagger）** | Knife4j 4.5.0 已引入，但无 OpenAPI security scheme 配置 | Knife4j + `@io.swagger.v3.oas.annotations.security.SecurityScheme` | [Knife4j 4.5 官方文档](https://doc.xiaoymin.com/knife4j/) | **建议改** | Swagger UI 无"Authorize"按钮，开发者不知道传什么 token | 低：仅添加注解，不改业务逻辑 |
| **API 文档（Springdoc）** | springdoc 通过 Knife4j 间接引入，`springdoc.swagger-ui.enabled=false` | 移除 springdoc 依赖，统一使用 Knife4j | 同上 | **建议改** | 冗余依赖，增加打包体积和类路径复杂度 | 低：Knife4j 自身提供 OpenAPI 3.0 支持 |
| **认证（Sa-Token）** | Sa-Token 1.42.0 + 自定义 `ApiSignatureFilter` | 当前方案基本合理 | [Sa-Token 官方文档](https://sa-token.dev/) | **暂不改** | Sa-Token + HMAC-SHA256 签名是成熟方案；ApiSignatureFilter 有 TODO 说明 KeyVault 迁移计划 | 低 |
| **请求体缓存** | 自定义 `CachedBodyHttpServletRequest` | 可继续使用（Spring 没有等效开箱配置） | - | **暂不改** | ApiSignatureFilter 需读取 body，框架无等效方案 | 低 |
| **Scope 校验** | 自定义 `@RequireScope` + `ScopeCheckInterceptor` | 当前方案合理（Sa-Token 无内置 scope 概念） | [Sa-Token 权限认证](https://sa-token.dev/doc/auth/) | **暂不改** | 拦截器实现是合理的，AOP 方式符合规范 | 低 |
| **Workspace 角色校验** | 自定义 `@RequireWorkspaceRole` + `WorkspaceRoleCheckInterceptor` | 当前方案合理 | 同上 | **暂不改** | 同上 | 低 |
| **统一异常处理** | `GlobalExceptionHandler` (RestControllerAdvice) | 框架方案：已正确使用 `@RestControllerAdvice` | [Spring 官方 - Exception Handler](https://docs.spring.io/spring-boot/reference/web/spring-mvc.html#web-servlet-exception-handler) | **已符合框架** | - | - |
| **参数校验** | Jakarta Bean Validation (`@Valid`, `@Validated`) | 框架方案：已正确使用 | [Spring Validation](https://docs.spring.io/spring-boot/reference/web/spring-mvc.html#web-servlet-validation) | **已符合框架** | - | - |
| **CORS** | `application.yml` 配置 CORS origins + 无显式 CorsConfigurationSource Bean | 应增加 `WebMvcConfigurer` 显式 CORS 配置 | [Spring CORS](https://docs.spring.io/spring-boot/reference/web/spring-mvc.html#web-servlet-cors) | **建议改** | 当前仅配置允许 origins，未注册 `CorsConfigurationSource` Bean，CORS 可能不生效 | 中：跨域请求可能失败 |
| **数据源** | Druid 连接池 + 自定义 `DataSourceConfig` | 框架方案：`spring-boot-starter-jdbc` + Druid auto-config | [Spring Boot DataSource](https://docs.spring.io/spring-boot/reference/data/sql.html) | **建议评估** | 当前 `DataSourceConfig` 需查看是否有特殊逻辑，无则可移除 | 中：需确认 DataSourceConfig 是否有自定义逻辑 |
| **MyBatis** | MyBatis-Flex 1.11.7 + `mybatis-flex-spring-boot3-starter` | 框架方案：已正确集成 | [MyBatis-Flex 官方](https://mybatis-flex.com/) | **已符合框架** | - | - |
| **分页** | 自定义 `PageResponse` VO + Service 层手动处理 | `Pageable` + `Page`（Spring Data） | [Spring Data Pageable](https://docs.spring.io/spring-data/jpa/reference/repositories/paging.html) | **暂不改** | `PageResponse` 已封装合理，迁移收益不高 | 低 |
| **审计字段** | 手动在 Mapper/Service 层处理 | JPA Auditing (`@CreatedDate`, `@LastModifiedDate`) | [Spring JPA Auditing](https://docs.spring.io/spring-data/jpa/reference/auditing.html) | **不建议改** | MyBatis-Flex 不支持 JPA Auditing；当前手动方式是唯一选择 | - |
| **Flyway / Liquibase** | 迁移脚本在 `axiqra-infra/docker/cockroachdb/migrations/` | Flyway 集成到 Spring Boot | [Spring Boot Flyway](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.migration) | **暂不改** | 当前通过 SQL 文件初始化 DB（init.sql），Flyway 需额外配置 | 中：脚本式迁移没有版本管理 |
| **健康检查** | 自定义 `HealthController` 做深度检测 | Spring Boot Actuator (`@HealthIndicator`) | [Spring Actuator Health](https://docs.spring.io/spring-boot/reference/actuator/health.html) | **建议评估** | 自定义 HealthController 是合理的（检测外部依赖），但可考虑暴露为 Actuator HealthIndicator | 中：当前方案更灵活 |
| **Actuator 端点暴露** | `application.yml` 配置 `management.endpoints.web.exposure.include` | 框架方案：已正确配置 | 同上 | **已符合框架** | - | - |
| **Redis** | `StringRedisTemplate`（通过 `application.yml` auto-config） | 框架方案：已正确集成 | [Spring Data Redis](https://docs.spring.io/spring-boot/reference/data/redis.html) | **已符合框架** | - | - |
| **RabbitMQ** | `RabbitTemplate`（通过 `application.yml` auto-config） | 框架方案：已正确集成 | [Spring AMQP](https://docs.spring.io/spring-boot/reference/messaging/amqp.html) | **已符合框架** | - | - |
| **API 签名防重放** | 自定义 `ApiSignatureFilter`（HMAC-SHA256 + Nonce） | Spring Security 方案 | [Spring Security Crypto](https://docs.spring.io/spring-security/reference/servlet/exploitsheaders.html) | **暂不改** | 自定义方案是合理的，框架无等效内置功能 | 低 |
| **日志配置** | `logback-spring.xml` | 框架方案：已正确使用 `logback-spring.xml` | [Spring Boot Logging](https://docs.spring.io/spring-boot/reference/logging.html) | **已符合框架** | - | - |
| **MDC / TraceId** | 自定义 `TraceIdFilter` | 可选：Spring Cloud Sleuth / Micrometer Tracing | [Micrometer Tracing](https://micrometer.io/docs/tracing) | **暂不改** | 当前 Filter 实现简洁实用，Sleuth 侵入较大 | 低 |
| **熔断降级** | Resilience4j 2.3.0（`resilience4j-spring-boot3`） | 框架方案：已正确集成 | [Resilience4j](https://resilience4j.com/) | **已符合框架** | - | - |
| **OpenAPI Schema** | Controller 有 `@Tag` 和 `@Operation`，但无全局 security scheme | `@io.swagger.v3.oas.annotations.security.*` | [OpenAPI 3.0 Security](https://swagger.io/specification/#security-scheme-object) | **建议改** | Schema 准确性影响 SDK 生成质量 | 低 |
| **DTO / Request 对象** | 自定义 DTO 类 + `@Valid` 校验注解 | Jakarta Bean Validation（Hibernate Validator） | [Bean Validation](https://jakarta.ee/specifications/bean-validation/3.0/) | **已符合框架** | - | - |
| **API 路径规范** | 部分 Controller 使用 `/api/v1/...` 与 context-path 叠加 | 统一去掉 `/api` 前缀（因为 context-path 已是 `/api`） | RESTful 规范 | **必须改** | 当前路径异常导致 API 实际不可用 | 高 |
| **前端（不存在）** | 当前项目无前端代码（无 package.json） | Vite + Vue 3 + TypeScript + Pinia + Vue Router | [Vite](https://vite.dev/) / [Vue 3](https://vuejs.org/) | **规划项** | 当前交付范围为后端 API，前端待建设 | - |

### 二、必须改（影响安全、认证、Swagger 准确性、热部署）

| 编号 | 问题 | 动作 | 优先级 |
|------|------|------|------|
| **MUST-001** | Controller 路径叠加异常（`/api/api/v1/...`） | 修正 `InvocationController`/`FeedbackController`/`ContributionController`/`ReviewController`/`ToolModelController` 的 `@RequestMapping` 从 `/api/v1/` 改为 `/v1/` | P0 |
| **MUST-002** | CORS 配置未注册为 Bean | 新增 `CorsConfigurationSource` Bean 或 `@CrossOrigin` 注解 | P0 |
| **MUST-003** | OpenAPI security scheme 未声明 | 新增 `OpenApiCustomizer` Bean 或 `@SecurityScheme` 注解声明 `Authorization` Bearer scheme | P0 |

### 三、建议改（不阻塞但降低维护性）

| 编号 | 问题 | 动作 | 优先级 |
|------|------|------|------|
| SUGGEST-001 | Springdoc 冗余依赖 | 移除 `springdoc` 相关配置（当前 `springdoc.*` 在 application.yml 中），统一用 Knife4j | S2 |
| SUGGEST-002 | `HealthController` 深度检测可暴露为 Actuator 指标 | 将 DB/Redis/Rabbit/MinIO 检测状态暴露为 `/actuator/health` details | S2 |
| SUGGEST-003 | Flyway 未集成 | 评估从 SQL 文件迁移到 Flyway 版本化管理 | S2 |
| SUGGEST-004 | DataSourceConfig 是否有必要 | 检查 `axiqra-start` 中的 `DataSourceConfig` 是否有多余自定义逻辑 | S2 |

### 四、不建议改（当前手写更合理）

| 编号 | 场景 | 原因 |
|------|------|------|
| NO-CHANGE-001 | `CachedBodyHttpServletRequest` | Spring Framework 没有等效的"可缓存请求体"的框架配置，这是 Filter 级别读取 body 的唯一方案 |
| NO-CHANGE-002 | `ApiSignatureFilter` | Spring Security 不提供开箱即用的 HMAC-SHA256 + Nonce 防重放方案，自定义 Filter 是正确的选择 |
| NO-CHANGE-003 | `@RequireScope` + `ScopeCheckInterceptor` | Sa-Token 不内置 scope 概念，AOP 拦截器是成熟实现 |
| NO-CHANGE-004 | `GlobalExceptionHandler` | 完全符合框架规范，使用 `@RestControllerAdvice` + 各种 `@ExceptionHandler` |
| NO-CHANGE-005 | `TraceIdFilter` | Micrometer Tracing / OpenTelemetry 侵入较大，当前 Filter 实现简洁实用 |

### 五、需要我决策的项

| 问题 | 方案 A：框架 | 方案 B：手写 | 推荐 | 需要我决定什么 |
|------|----------|---------|------|------------|
| **Swagger UI** | 启用 springdoc 的 `swagger-ui`（`/api/swagger-ui/index.html`） | 保持 Knife4j（`/api/doc.html`） | **B** | Knife4j UI 功能更丰富（国内社区更活跃），但 springdoc 与 Knife4j 有重叠。确认：是否需要同时支持两个 UI？ |
| **排行榜 scope** | `public:read` scope（保留当前设计） | 去掉 scope，改为登录即可访问 | **待确认** | `ToolModelController.getLeaderboard` 的 `public:read` scope 是否必要？排行榜是否为公开数据？ |
| **前端技术栈** | 当前不交付前端（仅后端） | Vite + Vue 3（未来） | **A** | 当前是否需要开始搭建前端项目脚手架？ |
| **数据库迁移** | Flyway（版本化管理，集成到 Spring Boot） | 保持当前 SQL 文件方式 | **待确认** | 是否接受增加 Flyway 依赖和管理迁移脚本版本？ |

---

## 任务 4：阶梯式开发拆分

### 一、任务总览

| 阶段 | 任务编号 | 任务名称 | 目标 | 交付物 | 是否阻塞后续 |
|------|---------|--------|------|------|----------|
| **P0** | TASK-DEV-001 | 运行时 Swagger 接口盘点 | 启动项目，访问 `/api/v3/api-docs` 和 `/api/doc.html`，验证静态扫描结果 | Swagger 接口盘点表（运行时验证版） | 是（为所有后续任务提供基准） |
| **P0** | TASK-DEV-002 | Swagger 认证规则核对 | 逐一测试每个接口的认证行为，验证 Sa-Token 拦截器和 ApiSignatureFilter 是否正确工作 | 认证接口清单（含异常场景） | 是（安全基线） |
| **P0** | TASK-DEV-003 | 接口用途与模块归属表生成 | 完善接口文档，标注每个接口的业务含义 | 接口用途表 | 是（API 文档化） |
| **P0** | TASK-DEV-004 | API 路径叠加异常修复 | 修正 `/api/api/v1/...` 异常路径 | 修正后的 Controller 文件 | 是（API 实际不可用） |
| **P0** | TASK-DEV-005 | CORS 配置修复 | 注册 `CorsConfigurationSource` Bean | application.yml 变更或新增配置类 | 是（前端联调阻断） |
| **P0** | TASK-DEV-006 | OpenAPI Security Scheme 配置 | 添加 Bearer token security scheme | Knife4j UI 显示 Authorize 按钮 | 是（开发者体验） |
| **S1** | TASK-DEV-007 | Spring Boot DevTools 引入 | 添加 DevTools 依赖，验证后端热部署 | pom.xml 变更 + 验收通过 | 否（但提升开发效率） |
| **S1** | TASK-DEV-008 | Docker Compose Watch 方案评估 | 编写并测试 Docker Compose watch 配置 | docker-compose.yml 变更 + 评估报告 | 否 |
| **S1** | TASK-DEV-009 | CandidateSeed Controller 补充 | 新增 `CandidateSeedController` | 新 Controller 文件 | 否（功能缺失） |
| **S1** | TASK-DEV-010 | 排行榜 scope 决策与实施 | 根据决策修改或保留 `public:read` scope | Controller 或 scope 变更 | 否 |
| **S2** | TASK-DEV-011 | Springdoc 冗余依赖清理 | 移除 springdoc 配置，统一 Knife4j | pom.xml + application.yml | 否 |
| **S2** | TASK-DEV-012 | Flyway 迁移方案评估 | 评估 SQL 文件迁移到 Flyway 的可行性 | 评估报告 + （可选）实施 | 否 |
| **S2** | TASK-DEV-013 | 前端项目脚手架搭建（规划） | 创建 Vite + Vue 3 项目，配置 HMR | 前端项目结构 | 否（远期规划） |
| **S2** | TASK-DEV-014 | 框架优先重构候选清单落实 | 逐项落实 TASK-DEV-011 等 S2 项 | 重构后的代码文件 | 否 |

### 二、详细任务（关键 P0 任务）

---

#### TASK-DEV-001：运行时 Swagger 接口盘点

**任务目的**：用运行时扫描替代静态扫描，获得精确的接口数量和安全标注。  
**使用角色**：后端工程师、全栈工程师  
**前置条件**：所有依赖服务（CockroachDB、Redis、RabbitMQ）已启动  
**正常流程**：
1. 启动 `axiqra-start`：执行 `mvn spring-boot:run -pl axiqra-start -am`
2. 访问 `/api/v3/api-docs` 获取完整 OpenAPI JSON
3. 访问 `/api/doc.html` 验证 Knife4j UI
4. 导出接口清单，与静态扫描结果对比
5. 识别路径叠加异常（`/api/api/v1/...`）

**输出**：运行时验证后的接口清单表（标注哪些与静态扫描一致/不一致）  
**验收标准**：
- TC-001-01：`/api/v3/api-docs` 返回 200 和有效 JSON
- TC-001-02：接口总数与静态扫描结果误差 ≤ 5%（路径异常修正后）
- TC-001-03：每个接口有 `@Operation(summary=)` 描述
- TC-001-04：含 `@RequireScope` 的接口在 Swagger 中显示锁图标

---

#### TASK-DEV-004：API 路径叠加异常修复

**任务目的**：修复因 Controller `@RequestMapping("/api/v1/...")` 与 context-path `/api` 叠加导致的无效路径。  
**使用角色**：后端工程师  
**前置条件**：项目可编译通过  
**正常流程**：
1. 修改 `InvocationController`：`/api/v1/invocations` → `/v1/invocations`
2. 修改 `FeedbackController`：`/api/v1/feedbacks` → `/v1/feedbacks`
3. 修改 `ContributionController`：`/api/v1/contributions` → `/v1/contributions`
4. 修改 `ReviewController`：`/api/v1/reviews` → `/v1/reviews`
5. 修改 `ToolModelController`：`/api/v1/tool-models` → `/v1/tool-models`
6. 验证 Swagger 中路径变为 `/api/v1/...`（context-path + 修正后路径）
7. 用 curl 测试接口返回 401（非 404）

**异常流程**：若某些接口已被外部引用（CLI/MCP 客户端），需同步通知消费者更新 endpoint  
**验收标准**：
- TC-004-01：`/api/v1/invocations` 返回 200/401/403（非 404）
- TC-004-02：`/api/v1/feedbacks` 返回 200/401/403（非 404）
- TC-004-03：`/api/v3/api-docs` 中上述路径不出现 `/api/api/` 双前缀
- TC-004-04：所有已有集成测试通过

---

#### TASK-DEV-005：CORS 配置修复

**任务目的**：确保前端（未来）和 API 调试工具可以跨域调用后端。  
**使用角色**：后端工程师  
**前置条件**：无  
**正常流程**：
1. 创建配置类实现 `WebMvcConfigurer#addCorsMappings`
2. 映射 `/**`，来源使用 `application.yml` 中的 `security.cors.allowed-origins`
3. 允许方法：GET, POST, PUT, DELETE, OPTIONS
4. 允许请求头：`*`
5. 暴露响应头：Authorization, X-Trace-Id, X-Request-Id
6. 配置是否允许携带凭证（`allowCredentials`）

**验收标准**：
- TC-005-01：`OPTIONS /api/connect/quota` 返回 200 和 CORS 响应头
- TC-005-02：跨域请求携带 `Authorization` token 正常通过

---

#### TASK-DEV-006：OpenAPI Security Scheme 配置

**任务目的**：让 Knife4j UI 显示"Authorize"按钮和 Bearer token 输入框，提升开发者体验。  
**使用角色**：后端工程师  
**前置条件**：无  
**正常流程**：
1. 在 `axiqra-api` 中创建 `OpenApiConfig` 配置类
2. 声明 `SecurityScheme`：type = http, scheme = bearer, bearerFormat = JWT, description = "Sa-Token Token"
3. 创建全局 `SecurityRequirement`：所有业务接口默认需要认证
4. 对 `/internal/health/**`、`/actuator/health/**`、`/doc.html` 等排除认证要求
5. 验证 Knife4j UI 显示 Authorize 按钮

**验收标准**：
- TC-006-01：Knife4j UI `/api/doc.html` 显示 "Authorize" 按钮
- TC-006-02：输入 Sa-Token token 后，已认证接口请求带 `Authorization: Bearer xxx` 头
- TC-006-03：未认证请求 Swagger 文档中显示🔒图标

---

#### TASK-DEV-007：Spring Boot DevTools 引入

**任务目的**：实现后端代码修改后自动重启，提升开发效率。  
**使用角色**：后端工程师  
**前置条件**：JDK 17 + Maven 3.9.9 已配置  
**正常流程**：
1. 在 `axiqra-start/pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

2. 执行 `mvn spring-boot:run`
3. 修改任意 Java 文件（如在 Controller 中加一行日志）
4. 观察控制台：应出现 "Restarting devtools context" 或自动重载日志
5. 用 curl 验证修改已生效

**配置文件**：无需额外配置（DevTools 自动激活）  
**验收标准**：
- TC-007-01：修改 `*Controller.java` 后，服务在 5 秒内自动重启
- TC-007-02：修改 `*Service.java` 后，服务在 5 秒内自动重启
- TC-007-03：修改 `application.yml` 后，服务自动重启
- TC-007-04：修改静态资源（若有）后，浏览器 Livereload 自动刷新

---

### 三、建议初始拆分（完整任务清单）

| 编号 | 任务名称 | 优先级 | 阶段 | 对应编号 |
|------|--------|------|------|---------|
| TASK-DEV-001 | 运行时 Swagger 接口盘点 | P0 | P0 | 任务1 |
| TASK-DEV-002 | Swagger 认证规则核对 | P0 | P0 | 任务1 |
| TASK-DEV-003 | 接口用途与模块归属表生成 | P0 | P0 | 任务1 |
| TASK-DEV-004 | API 路径叠加异常修复 | **P0（必须）** | P0 | 任务3 |
| TASK-DEV-005 | CORS 配置修复 | P0 | P0 | 任务3 |
| TASK-DEV-006 | OpenAPI Security Scheme 配置 | P0 | P0 | 任务1/3 |
| TASK-DEV-007 | Spring Boot DevTools 本地热部署 | S1 | S1 | 任务2 |
| TASK-DEV-008 | Docker Compose Watch 方案评估与实施 | S1 | S1 | 任务2 |
| TASK-DEV-009 | CandidateSeed Controller 补充 | S1 | S1 | 任务1 |
| TASK-DEV-010 | 排行榜 scope 决策与实施 | S1 | S1 | 任务3 |
| TASK-DEV-011 | Springdoc 冗余依赖清理 | S2 | S2 | 任务3 |
| TASK-DEV-012 | Flyway 迁移方案评估 | S2 | S2 | 任务3 |
| TASK-DEV-013 | 前端项目脚手架搭建（规划） | S2 | S2 | 任务2 |
| TASK-DEV-014 | 框架优先重构候选清单落实 | S2 | S2 | 任务3 |

---

## 最终提交物清单

1. **Swagger 接口盘点表**（任务1）：本报告"任务1"中所有表格
2. **认证接口清单**（任务1）：含 scope、role、公开/需登录分类的完整清单
3. **接口用途表**（任务1）：每个接口的业务含义和模块归属
4. **热部署方案说明**（任务2）：A/B/C 三方案及推荐方案
5. **需要修改的配置文件清单**（任务2）：pom.xml、docker-compose.yml 等
6. **阶梯式开发任务表**（任务4）：P0/S1/S2 完整任务拆分
7. **不能用框架解决、需要我决策的清单**（任务3）：

| 问题 | 决策项 |
|------|--------|
| Swagger UI：Knife4j vs springdoc | 是否需要移除 springdoc，统一用 Knife4j？ |
| 排行榜 scope：保留 vs 去掉 | `ToolModelController.getLeaderboard` 的 `public:read` scope 是否必要？ |
| 前端技术栈：现在 vs 未来 | 是否需要现在搭建前端项目脚手架？ |
| 数据库迁移：Flyway vs 保持 | 是否引入 Flyway 管理 SQL 迁移脚本版本？ |

8. **风险与回滚方案**：

| 风险 | 等级 | 回滚方案 |
|------|------|---------|
| 路径叠加异常修复导致已部署客户端失效 | 高 | 通过版本化 API + 临时保留旧路径别名（或提前通知消费者） |
| CORS 配置错误导致跨域请求失败 | 中 | 回滚配置类更改，恢复当前 application.yml 配置 |
| OpenAPI Security Scheme 修改导致文档不一致 | 低 | 回滚 OpenApiConfig 配置类 |
| DevTools 依赖导致生产打包包含 devtools | 低 | 确认 `<scope>runtime</scope>` 和 `optional=true` 配置正确；生产打包测试验证 |
| Docker Compose Watch 配置错误导致文件监听风暴 | 中 | 移除 watch 配置，恢复手动 rebuild |
