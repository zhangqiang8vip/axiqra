# TASK 1 - Swagger / OpenAPI Interface Inventory

扫描日期：2026-06-23  
扫描方式：静态扫描 Controller、OpenAPI 注解、Sa-Token 配置、Spring Security 配置；未完成运行时 `/api/v3/api-docs` 验证。  
外部路径口径：`server.servlet.context-path=/api` 已计入表格。已修正 5 个 `/api/v1` Controller 前缀叠加问题，实际路径为 `/api/v1/...`。

## 一、官方资料核查

| 技术 | 官方资料 | 当前项目版本 | 是否适用 | 备注 |
| -- | -- | -- | -- | -- |
| FEAT-DOCS-001 Spring Boot | Spring Boot Reference Documentation - Developer Tools / Web MVC / Actuator, https://docs.spring.io/spring-boot/reference/ | 3.5.14 | 是 | 用于确认 DevTools、CORS、Actuator 与 Spring MVC 行为 |
| FEAT-DOCS-002 Knife4j | Knife4j 官方文档, https://doc.xiaominfo.com/docs/quick-start | 4.5.0 | 是 | 项目引入 `knife4j-openapi3-jakarta-spring-boot-starter` |
| FEAT-DOCS-003 OpenAPI | OpenAPI Specification - Security Scheme Object, https://spec.openapis.org/oas/latest.html | OpenAPI 3 | 是 | 用于 Bearer security scheme |
| FEAT-DOCS-004 Sa-Token | Sa-Token 官方文档, https://sa-token.cc/doc.html | 1.42.0 | 是 | 登录拦截、注解鉴权、Redis 会话 |

## 二、Swagger 总览

| 项 | 结果 |
| -- | -- |
| Swagger / OpenAPI 实现 | Knife4j 4.5.0 + Springdoc OpenAPI 注解能力 |
| 版本 | `knife4j.version=4.5.0`，Spring Boot `3.5.14` |
| Swagger JSON 地址 | 预期 `/api/v3/api-docs`，需运行时验证 |
| Swagger UI 地址 | Knife4j 预期 `/api/doc.html` |
| 接口总数 | TC-SWAGGER-001：55 个 Controller 接口，不含 Actuator |
| 公开接口数 | TC-SWAGGER-002：4 个 Controller 接口：Health 2 个、Auth 登录/注册 2 个 |
| 需要登录接口数 | TC-SWAGGER-003：51 个 Controller 接口 |
| 需要 scope / role 接口数 | TC-SWAGGER-004：19 个，其中 scope 15 个、workspace role 4 个 |
| 静态扫描还是运行时扫描 | 静态扫描 |
| 无法确认项 | `/api/v3/api-docs` 运行时实际内容、Knife4j UI 是否显示 Authorize、Actuator 实际暴露端点 |

## 三、接口明细表

| 序号 | Method | Path | Controller / Handler | 认证要求 | Scope / Role | 用途说明 | 请求对象 | 响应对象 | 错误码 | Swagger 是否暴露 | 备注 |
| -: | -- | -- | -- | -- | -- | -- | -- | -- | -- | -- | -- |
| 1 | GET | `/api/internal/health` | HealthController | 公开 | - | 健康检查 | - | Map | - | 是 | PERM-SWAGGER-PUBLIC-001 |
| 2 | GET | `/api/internal/health/verify` | HealthController | 公开 | - | 依赖深度检测 | - | Map | - | 是 | PERM-SWAGGER-PUBLIC-002 |
| 3 | POST | `/api/auth/login` | AuthController | 公开 | - | 用户登录并返回 Sa-Token | LoginRequest | LoginResponse | 400/401 | 是 | PERM-AUTH-PUBLIC-001 |
| 4 | POST | `/api/auth/register` | AuthController | 公开 | - | 注册用户 | RegisterRequest | UserInfoVO | 400 | 是 | PERM-AUTH-PUBLIC-002 |
| 5 | POST | `/api/auth/logout` | AuthController | 需登录 | - | 注销当前会话 | - | Void | 401 | 是 | SaInterceptor 未排除 logout |
| 6 | GET | `/api/auth/me` | AuthController | 需登录 | - | 获取当前用户信息 | - | UserInfoVO | 401 | 是 | 与 `/users/me` 有重叠 |
| 7 | PUT | `/api/auth/profile` | AuthController | 需登录 | - | 更新个人资料 | ProfileUpdateRequest | UserInfoVO | 401/400 | 是 | - |
| 8 | GET | `/api/connect/quota` | ConnectController | 需登录 | - | 查询当前配额状态 | - | QuotaStatusVO | 401 | 是 | - |
| 9 | GET | `/api/connect/rate-limit` | ConnectController | 需登录 | - | 查询限流窗口 | - | RateLimitStatusVO | 401 | 是 | 返回 Retry-After |
| 10 | GET | `/api/connect/doctor` | ConnectController | 需登录 | - | Connect doctor 检查 | query | ConnectDoctorVO | 401/400 | 是 | 文档草案写 POST，代码为 GET |
| 11 | GET | `/api/connect/sessions` | ConnectController | 需登录 | - | 查询我的接入会话 | - | List<ConnectSessionVO> | 401 | 是 | - |
| 12 | GET | `/api/connect/sessions/{sessionId}` | ConnectController | 需登录 | - | 查询接入会话详情 | sessionId | ConnectSessionVO | 401/404 | 是 | - |
| 13 | POST | `/api/connect/sessions` | ConnectController | 需登录 | - | 创建接入会话 | ConnectSessionCreateRequest | ConnectSessionVO | 401/429 | 是 | FEAT-CONNECT-001 |
| 14 | POST | `/api/search/before-act` | SearchController | 需登录 | - | 任务前搜索 | SearchRequest | SearchResponseVO | 401 | 是 | FEAT-SEARCH-001 |
| 15 | GET | `/api/solutions/{solutionId}` | SolutionController | 需登录 | - | 获取 Solution 详情 | solutionId | SolutionDetailVO | 401/404 | 是 | FEAT-SOLUTION-001 |
| 16 | POST | `/api/traces` | TraceController | 需登录 | - | 创建 Trace 草稿 | TraceCreateRequest | TraceDetailVO | 401/400 | 是 | FEAT-TRACE-001 |
| 17 | POST | `/api/traces/{traceId}/confirm` | TraceController | 需登录 | - | 确认 Trace 结果 | TraceConfirmRequest | TraceDetailVO | 401/404 | 是 | - |
| 18 | POST | `/api/traces/{traceId}/submit` | TraceController | 需登录 | - | 提交 Trace 审核 | traceId | TraceDetailVO | 401/404 | 是 | - |
| 19 | GET | `/api/traces/{traceId}` | TraceController | 需登录 | - | 获取 Trace 详情 | traceId | TraceDetailVO | 401/404 | 是 | - |
| 20 | POST | `/api/project-cases` | ProjectCaseController | 需登录 | - | 从 Trace 创建 Project Case | ProjectCaseCreateRequest | ProjectCaseDetailVO | 401 | 是 | - |
| 21 | GET | `/api/project-cases/{caseId}` | ProjectCaseController | 需登录 | - | 获取 Project Case | caseId | ProjectCaseDetailVO | 401/404 | 是 | - |
| 22 | POST | `/api/project-cases/{caseId}/publish-request` | ProjectCaseController | 需登录 | - | 申请发布 Public Case | caseId + authorizationId | ProjectCaseDetailVO | 401/403 | 是 | 业务层需校验 owner |
| 23 | POST | `/api/public-cases/publish/{projectCaseId}` | PublicCaseController | 需登录 | - | 发布 Public Case | projectCaseId | PublicCaseDetailVO | 401 | 是 | - |
| 24 | GET | `/api/public-cases/{publicCaseId}` | PublicCaseController | 需登录 | - | 获取 Public Case | publicCaseId | PublicCaseDetailVO | 401/404 | 是 | - |
| 25 | GET | `/api/public-cases` | PublicCaseController | 需登录 | - | 列出公开 Case | limit | List<PublicCaseDetailVO> | 401 | 是 | 是否应公开待产品确认 |
| 26 | POST | `/api/v1/invocations` | InvocationController | 需登录 | connect:write | 上报调用结果 | InvocationReportRequest | InvocationDetailVO | 401/403 | 是 | TASK-DEV-004 已修路径 |
| 27 | GET | `/api/v1/invocations/{invocationId}` | InvocationController | 需登录 | connect:read | 获取调用详情 | invocationId | InvocationDetailVO | 401/403 | 是 | - |
| 28 | GET | `/api/v1/invocations/solutions/{solutionId}/feedback-stats` | InvocationController | 需登录 | feedback:read | 获取 Solution 反馈统计 | solutionId | SolutionFeedbackStatsVO | 401/403 | 是 | - |
| 29 | POST | `/api/v1/feedbacks` | FeedbackController | 需登录 | feedback:write | 提交反馈 | FeedbackSubmitRequest | FeedbackDetailVO | 401/403 | 是 | 草案为 `/invocations/{id}/feedback` |
| 30 | GET | `/api/v1/feedbacks` | FeedbackController | 需登录 | feedback:read | 获取反馈列表 | query | List<FeedbackDetailVO> | 401/403 | 是 | - |
| 31 | GET | `/api/v1/feedbacks/solutions/{solutionId}/stats` | FeedbackController | 需登录 | feedback:read | Solution 反馈统计 | solutionId | SolutionFeedbackStatsVO | 401/403 | 是 | - |
| 32 | GET | `/api/v1/contributions/users/{userId}/summary` | ContributionController | 需登录 | contribution:read | 用户贡献汇总 | userId | ContributionSummaryVO | 401/403 | 是 | - |
| 33 | GET | `/api/v1/contributions/users/{userId}/records` | ContributionController | 需登录 | contribution:read | 用户贡献记录 | userId + limit | List | 401/403 | 是 | - |
| 34 | GET | `/api/v1/reviews/pending` | ReviewController | 需登录 | review:read | 待审核队列 | queue + limit | List<ReviewDetailVO> | 401/403 | 是 | - |
| 35 | GET | `/api/v1/reviews/{reviewId}` | ReviewController | 需登录 | review:read | 审核详情 | reviewId | ReviewDetailVO | 401/403 | 是 | - |
| 36 | POST | `/api/v1/reviews/{reviewId}/approve` | ReviewController | 需登录 | review:write | 审核通过 | query | ReviewDetailVO | 401/403 | 是 | 审计高优先级 |
| 37 | POST | `/api/v1/reviews/{reviewId}/reject` | ReviewController | 需登录 | review:write | 审核拒绝 | query | ReviewDetailVO | 401/403 | 是 | - |
| 38 | POST | `/api/v1/reviews/{reviewId}/quarantine` | ReviewController | 需登录 | review:write | 隔离内容 | query | ReviewDetailVO | 401/403 | 是 | - |
| 39 | POST | `/api/v1/reviews/{reviewId}/appeal` | ReviewController | 需登录 | review:write | 申诉 | appealContent | ReviewDetailVO | 401/403 | 是 | - |
| 40 | POST | `/api/policy/evaluate` | PolicyController | 需登录 | - | 策略评估 | PolicyEvaluationRequest | PolicyEvaluationVO | 401 | 是 | 报告旧版写 GET，代码为 POST |
| 41 | POST | `/api/policy/enforce` | PolicyController | 需登录 | admin:all | 强制策略评估 | PolicyEvaluationRequest | Void | 401/403 | 是 | RISK-AUTH-001 |
| 42 | GET | `/api/policy/check` | PolicyController | 需登录 | - | 快速 scope 检查 | scope | Boolean | 401 | 是 | - |
| 43 | GET | `/api/workspaces` | WorkspaceController | 需登录 | - | 我的工作空间列表 | PageRequest | PageResponse<WorkspaceVO> | 401 | 是 | - |
| 44 | POST | `/api/workspaces` | WorkspaceController | 需登录 | - | 创建工作空间 | WorkspaceCreateRequest | WorkspaceVO | 401 | 是 | - |
| 45 | GET | `/api/workspaces/{workspaceId}` | WorkspaceController | 需登录 | - | 工作空间详情 | workspaceId | WorkspaceVO | 401/404 | 是 | - |
| 46 | PUT | `/api/workspaces/{workspaceId}` | WorkspaceController | 需登录 | WorkspaceRole.OWNER | 更新工作空间 | WorkspaceUpdateRequest | WorkspaceVO | 401/403 | 是 | PERM-ROLE-001 |
| 47 | DELETE | `/api/workspaces/{workspaceId}` | WorkspaceController | 需登录 | WorkspaceRole.OWNER | 删除工作空间 | workspaceId | Void | 401/403 | 是 | PERM-ROLE-002 |
| 48 | GET | `/api/workspaces/{workspaceId}/members` | WorkspaceController | 需登录 | - | 成员列表 | MemberQueryRequest | PageResponse<MemberVO> | 401 | 是 | - |
| 49 | POST | `/api/workspaces/{workspaceId}/members` | WorkspaceController | 需登录 | WorkspaceRole.ADMIN | 添加成员 | query | MemberVO | 401/403 | 是 | PERM-ROLE-003 |
| 50 | PUT | `/api/workspaces/{workspaceId}/members` | WorkspaceController | 需登录 | WorkspaceRole.OWNER | 更新成员角色 | MemberRoleUpdateRequest | Void | 401/403 | 是 | PERM-ROLE-004 |
| 51 | DELETE | `/api/workspaces/{workspaceId}/members/{memberId}` | WorkspaceController | 需登录 | WorkspaceRole.ADMIN | 移除成员 | ids | Void | 401/403 | 是 | 注解要求 ADMIN，描述说 admin/owner |
| 52 | GET | `/api/users/me` | UserController | 需登录 | - | 当前用户信息 | - | UserInfoVO | 401 | 是 | 与 `/auth/me` 重叠 |
| 53 | GET | `/api/users/{userId}` | UserController | 需登录 | - | 用户公开信息 | userId | UserPublicVO | 401 | 是 | - |
| 54 | GET | `/api/auth/nav` | NavController | 需登录 | - | 导航菜单 | - | NavResponseVO | 401 | 是 | `@SaCheckLogin` |
| 55 | GET | `/api/v1/tool-models/leaderboard` | ToolModelController | 需登录 | - | 工具模型排行榜 | query | List<ToolModelLeaderboardVO> | 401 | 是 | 已无 `public:read` 注解 |

## 四、按业务模块汇总

| 模块 | 接口数 | 主要用途 | 认证特点 | 风险 |
| -- | --: | -- | -- | -- |
| Auth | 5 | 登录、注册、登出、当前用户、资料更新 | 登录/注册公开，其余需登录 | `/auth/me` 与 `/users/me` 重叠 |
| Health | 2 | 健康检查 | 公开 | 深度检查是否暴露依赖细节需运行时确认 |
| Connect | 6 | 会话、doctor、限流、配额 | 需登录 | doctor 草案 method 不一致 |
| Search / Solution / Trace | 6 | 搜索、方案、轨迹 | 需登录 | 低 |
| Case / PublicCase | 6 | 私有案例、公开案例、发布 | 需登录，业务层校验 owner | 发布链路需防越权 |
| Invocation / Feedback / Contribution | 8 | 调用、反馈、贡献 | 需 scope | 路径已修复为 `/api/v1` |
| Review / Policy | 9 | 审核与策略 | 多数需 scope | `policy/enforce` 高风险 |
| Workspace / User / Nav | 12 | 工作空间、成员、用户、导航 | 部分需 workspace role | 成员删除注解/描述需对齐 |
| ToolModel | 1 | 排行榜 | 需登录 | 是否公开待产品确认 |

## 五、与 Axiqra 文档草案对比

| 文档接口 | 当前 Swagger 是否存在 | 当前代码是否存在 | 差异 | 建议动作 |
| -- | -- | -- | -- | -- |
| `POST /api/connect/sessions` | 静态判断存在 | 是 | 一致 | 无 |
| `POST /api/connect/doctor` | 静态判断存在但 method 不同 | 是 | 代码为 `GET /api/connect/doctor` | AC-SWAGGER-001：确认草案改 GET 或代码改 POST |
| `POST /api/search/before-act` | 静态判断存在 | 是 | 一致 | 无 |
| `GET /api/solutions/{id}` | 静态判断存在 | 是 | 参数名为 `solutionId` | 文档统一占位符命名 |
| `POST /api/traces` | 静态判断存在 | 是 | 一致 | 无 |
| `POST /api/project-cases/{id}/publish-request` | 静态判断存在 | 是 | 额外 `authorizationId` | 文档补充参数 |
| `POST /api/invocations/{id}/feedback` | 否 | 否，现为 `/api/v1/feedbacks` | 资源建模不一致 | 产品决定保留 feedback 资源还是补别名 |
| `POST /api/candidate-seeds` | 否 | Service 存在、Controller 缺失 | REST 暴露缺口 | TASK-DEV-009 新增 Controller |

## 六、结论

1. TC-SWAGGER-001：当前静态扫描 Controller 接口 55 个，不含 Actuator；运行时 Swagger 总数仍需 `/api/v3/api-docs` 确认。
2. PERM-SWAGGER-001：公开接口 4 个：`GET /api/internal/health`、`GET /api/internal/health/verify`、`POST /api/auth/login`、`POST /api/auth/register`。
3. PERM-SWAGGER-002：其余 51 个 Controller 接口需要登录；其中 15 个需要 scope，4 个需要 workspace role。
4. API-SWAGGER-001：已新增 OpenAPI Bearer security scheme 配置，仍需运行时确认 Knife4j 是否显示授权入口。
5. RISK-SWAGGER-001：未发现明显“未受保护但应受保护”的业务接口；PublicCase 列表、ToolModel 排行榜是否应公开需要产品确认。
6. RISK-SWAGGER-002：`/auth/logout` 文案说幂等，但 Sa-Token 登录拦截器未排除该路径，未登录请求会被拦截；需确认是否按文案公开幂等登出。
