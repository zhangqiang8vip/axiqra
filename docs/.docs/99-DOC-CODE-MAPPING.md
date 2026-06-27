# Axiqra 文档代码对照表

> 版本：v1.0  
> 状态：Initial  
> 适用范围：每次 PR 更新同步，确保文档与代码保持一致  
> 维护方式：每次代码变更后必须同步更新本文档

---

## 1. API 端点总览

### 1.1 认证模块 (Auth)

| # | API | 方法 | 路径 | Controller | Service | Mapper | 状态 |
|---|-----|------|------|------------|---------|--------|------|
| A01 | 登录 | POST | `/auth/login` | `AuthController` | `UserService` | `UserMapper` | ✅ |
| A02 | 注册 | POST | `/auth/register` | `AuthController` | `UserService`, `WorkspaceService` | `UserMapper`, `WorkspaceMapper` | ✅ |
| A03 | 登出 | POST | `/auth/logout` | `AuthController` | - | - | ✅ |
| A04 | 当前用户 | GET | `/auth/me` | `AuthController` | `UserService` | `UserMapper` | ✅ |
| A05 | 更新资料 | PUT | `/auth/profile` | `AuthController` | `UserService` | `UserMapper` | ✅ |
| A06 | 设备码获取 | POST | `/auth/device/code` | `DeviceAuthController` | - | Redis | ✅ |
| A07 | 设备令牌轮询 | POST | `/auth/device/token` | `DeviceAuthController` | `UserService` | `UserMapper` | ✅ |
| A08 | 设备授权确认 | POST | `/auth/device/confirm` | `DeviceAuthController` | `UserService` | `UserMapper` | ✅ |
| A09 | 导航菜单 | GET | `/auth/nav` | `NavController` | `NavService` | `MembershipMapper` | ✅ |

### 1.2 搜索模块 (Search)

| # | API | 方法 | 路径 | Controller | Service | Mapper | 状态 |
|---|-----|------|------|------------|---------|--------|------|
| S01 | 搜索前执行 | POST | `/search/before-act` | `SearchController` | `SearchService`, `VectorSearchService` | `SolutionMapper` | ✅ |
| S02 | 公开搜索 | POST | `/search/public` | `SearchController` | `SearchService` | `SolutionMapper` | ✅ |

### 1.3 轨迹管理模块 (Trace)

| # | API | 方法 | 路径 | Controller | Service | Mapper | 状态 |
|---|-----|------|------|------------|---------|--------|------|
| T01 | 创建 Trace | POST | `/traces` | `TraceController` | `TraceService` | `EngineeringTraceMapper` | ✅ |
| T02 | 确认 Trace | POST | `/traces/{traceId}/confirm` | `TraceController` | `TraceService` | `EngineeringTraceMapper` | ✅ |
| T03 | 提交 Trace | POST | `/traces/{traceId}/submit` | `TraceController` | `TraceService` | `EngineeringTraceMapper` | ✅ |
| T04 | Trace 列表 | GET | `/traces` | `TraceController` | `TraceService` | `EngineeringTraceMapper` | ✅ |
| T05 | Trace 详情 | GET | `/traces/{traceId}` | `TraceController` | `TraceService` | `EngineeringTraceMapper` | ✅ |
| T06 | 提交证据 | POST | `/traces/{traceId}/evidence` | `TraceController` | `TraceService` | `TraceEvidenceRefMapper` | ✅ |

### 1.4 方案模块 (Solution)

| # | API | 方法 | 路径 | Controller | Service | Mapper | 状态 |
|---|-----|------|------|------------|---------|--------|------|
| SO01 | 从 Case 生成 | POST | `/solutions/from-project-case` | `SolutionController` | `SolutionService` | `SolutionMapper`, `ProjectCaseMapper` | ✅ |
| SO02 | 方案详情 | GET | `/solutions/{solutionId}` | `SolutionController` | `SolutionService` | `SolutionMapper` | ✅ |
| SO03 | 公开方案列表 | GET | `/solutions/public` | `SolutionController` | `SolutionService` | `SolutionMapper` | ✅ |
| SO04 | 公开方案详情 | GET | `/solutions/public/{solutionId}` | `SolutionController` | `SolutionService` | `SolutionMapper` | ✅ |
| SO05 | 提交审核 | POST | `/solutions/{solutionId}/submit-for-review` | `SolutionController` | `SolutionService` | `SolutionMapper` | ✅ |
| SO06 | 归档方案 | POST | `/solutions/{solutionId}/archive` | `SolutionController` | `SolutionService` | `SolutionMapper` | ✅ |

### 1.5 案例模块 (Case)

| # | API | 方法 | 路径 | Controller | Service | Mapper | PRD 文档 |
|---|-----|------|------|------------|---------|--------|----------|
| C01 | 创建 Project Case | POST | `/project-cases` | `ProjectCaseController` | `ProjectCaseService` | `ProjectCaseMapper` | D07 |
| C02 | Project Case 详情 | GET | `/project-cases/{caseId}` | `ProjectCaseController` | `ProjectCaseService` | `ProjectCaseMapper` | D07 |
| C03 | 发布申请 | POST | `/project-cases/{caseId}/publish-request` | `ProjectCaseController` | `ProjectCaseService` | `ProjectCaseMapper`, `AuthorizationMapper` | D07 |
| C04 | 发布为公开 | POST | `/public-cases/publish/{projectCaseId}` | `PublicCaseController` | `PublicCaseService` | `PublicCaseMapper` | D07 |
| C05 | 提交审核 | POST | `/public-cases/{publicCaseId}/submit-review` | `PublicCaseController` | `PublicCaseService` | `PublicCaseMapper`, `ReviewMapper` | D14 |
| C06 | 审核状态 | GET | `/public-cases/{publicCaseId}/review-status` | `PublicCaseController` | `PublicCaseService` | `PublicCaseMapper` | D14 |
| C07 | 公开 Case 详情 | GET | `/public-cases/{publicCaseId}` | `PublicCaseController` | `PublicCaseService` | `PublicCaseMapper` | D07 |
| C08 | 公开 Case 列表 | GET | `/public-cases` | `PublicCaseController` | `PublicCaseService` | `PublicCaseMapper` | D07 |

### 1.6 工作空间模块 (Workspace)

| # | API | 方法 | 路径 | Controller | Service | Mapper | PRD 文档 |
|---|-----|------|------|------------|---------|--------|----------|
| W01 | 我的空间列表 | GET | `/workspaces` | `WorkspaceController` | `WorkspaceService` | `WorkspaceMapper`, `MembershipMapper` | D13 |
| W02 | 创建空间 | POST | `/workspaces` | `WorkspaceController` | `WorkspaceService` | `WorkspaceMapper`, `MembershipMapper` | D13 |
| W03 | 空间详情 | GET | `/workspaces/{workspaceId}` | `WorkspaceController` | `WorkspaceService` | `WorkspaceMapper` | D13 |
| W04 | 更新空间 | PUT | `/workspaces/{workspaceId}` | `WorkspaceController` | `WorkspaceService` | `WorkspaceMapper` | D13 |
| W05 | 删除空间 | DELETE | `/workspaces/{workspaceId}` | `WorkspaceController` | `WorkspaceService` | `WorkspaceMapper`, `MembershipMapper` | D13 |
| W06 | 成员列表 | GET | `/workspaces/{workspaceId}/members` | `WorkspaceController` | `WorkspaceService` | `MembershipMapper` | D13 |
| W07 | 添加成员 | POST | `/workspaces/{workspaceId}/members` | `WorkspaceController` | `WorkspaceService` | `MembershipMapper` | D13 |
| W08 | 更新成员角色 | PUT | `/workspaces/{workspaceId}/members` | `WorkspaceController` | `WorkspaceService` | `MembershipMapper` | D13 |
| W09 | 移除成员 | DELETE | `/workspaces/{workspaceId}/members/{memberId}` | `WorkspaceController` | `WorkspaceService` | `MembershipMapper` | D13 |

### 1.7 用户模块 (User)

| # | API | 方法 | 路径 | Controller | Service | Mapper | 状态 |
|---|-----|------|------|------------|---------|--------|------|
| U01 | 当前用户信息 | GET | `/users/me` | `UserController` | `UserService` | `UserMapper` | ✅ |
| U02 | 用户信息 | GET | `/users/{userId}` | `UserController` | `UserService` | `UserMapper` | ✅ |

### 1.8 反馈模块 (Feedback)

| # | API | 方法 | 路径 | Controller | Service | Mapper | 状态 |
|---|-----|------|------|------------|---------|--------|------|
| F01 | 提交反馈 | POST | `/v1/feedbacks` | `FeedbackController` | `FeedbackService` | `FeedbackMapper` | ✅ |
| F02 | 反馈列表 | GET | `/v1/feedbacks` | `FeedbackController` | `FeedbackService` | `FeedbackMapper` | ✅ |
| F03 | 反馈统计 | GET | `/v1/feedbacks/solutions/{solutionId}/stats` | `FeedbackController` | `FeedbackService` | `FeedbackMapper` | ✅ |

### 1.9 审核模块 (Review)

| # | API | 方法 | 路径 | Controller | Service | Mapper | PRD 文档 |
|---|-----|------|------|------------|---------|--------|----------|
| R01 | 待审核队列 | GET | `/v1/reviews/pending` | `ReviewController` | `ReviewService`, `ReviewQueueService` | `ReviewMapper` | D14 |
| R02 | 审核详情 | GET | `/v1/reviews/{reviewId}` | `ReviewController` | `ReviewService` | `ReviewMapper` | D14 |
| R03 | 审核通过 | POST | `/v1/reviews/{reviewId}/approve` | `ReviewController` | `ReviewService` | `ReviewMapper` | D14 |
| R04 | 审核拒绝 | POST | `/v1/reviews/{reviewId}/reject` | `ReviewController` | `ReviewService` | `ReviewMapper` | D14 |
| R05 | 隔离内容 | POST | `/v1/reviews/{reviewId}/quarantine` | `ReviewController` | `ReviewService` | `ReviewMapper` | D14 |
| R06 | 申诉 | POST | `/v1/reviews/{reviewId}/appeal` | `ReviewController` | `ReviewService` | `ReviewMapper` | D14 |

### 1.10 连接模块 (Connect)

| # | API | 方法 | 路径 | Controller | Service | 状态 | PRD 文档 |
|---|-----|------|------|------------|---------|------|----------|
| CO01 | 配额状态 | GET | `/connect/quota` | `ConnectController` | `QuotaService` | ✅ | D09 |
| CO02 | 限流检查 | GET | `/connect/rate-limit` | `ConnectController` | `RateLimitService` | ✅ | D09 |
| CO03 | Doctor 检查 | GET | `/connect/doctor` | `ConnectController` | `ConnectService` | ✅ | D09 |
| CO04 | 会话列表 | GET | `/connect/sessions` | `ConnectController` | `ConnectService` | ✅ | D09 |
| CO05 | 会话详情 | GET | `/connect/sessions/{sessionId}` | `ConnectController` | `ConnectService` | ✅ | D09 |
| CO06 | 创建会话 | POST | `/connect/sessions` | `ConnectController` | `ConnectService` | ✅ | D09 |

### 1.11 调用记录模块 (Invocation)

| # | API | 方法 | 路径 | Controller | Service | Mapper | 状态 |
|---|-----|------|------|------------|---------|--------|------|
| I01 | 上报调用 | POST | `/v1/invocations` | `InvocationController` | `InvocationService` | `InvocationMapper` | ✅ |
| I02 | 调用详情 | GET | `/v1/invocations/{invocationId}` | `InvocationController` | `InvocationService` | `InvocationMapper` | ✅ |
| I03 | Solution 反馈统计 | GET | `/v1/invocations/solutions/{solutionId}/feedback-stats` | `InvocationController` | `InvocationService` | `InvocationMapper` | ✅ |

### 1.12 策略模块 (Policy)

| # | API | 方法 | 路径 | Controller | Service | 状态 |
|---|-----|------|------|------------|---------|------|
| P01 | 策略评估 | POST | `/policy/evaluate` | `PolicyController` | `PolicyEngineService` | ✅ |
| P02 | 强制评估 | POST | `/policy/enforce` | `PolicyController` | `PolicyEngineService` | ✅ |
| P03 | 权限检查 | GET | `/policy/check` | `PolicyController` | `PolicyEngineService` | ✅ |

### 1.13 贡献模块 (Contribution)

| # | API | 方法 | 路径 | Controller | Service | Mapper | 状态 |
|---|-----|------|------|------------|---------|--------|------|
| CG01 | 我的贡献 | GET | `/v1/contributions/me` | `ContributionController` | `ContributionService` | `ContributionLedgerMapper` | ✅ |
| CG02 | 贡献排行 | GET | `/v1/contributions/rankings` | `ContributionController` | `ContributionService` | `ContributionLedgerMapper` | ✅ |

### 1.14 种子模块 (Seed)

| # | API | 方法 | 路径 | Controller | Service | Mapper | PRD 文档 |
|---|-----|------|------|------------|---------|--------|----------|
| SD01 | 创建候选 Seed | POST | `/seeds` | `SeedController` | `CandidateSeedService` | `CandidateSeedMapper` | D12 |
| SD02 | Seed 列表 | GET | `/seeds` | `SeedController` | `CandidateSeedService` | `CandidateSeedMapper` | D12 |
| SD03 | Seed 详情 | GET | `/seeds/{seedId}` | `SeedController` | `CandidateSeedService` | `CandidateSeedMapper` | D12 |

### 1.15 工具模型模块 (ToolModel)

| # | API | 方法 | 路径 | Controller | Service | Mapper | 状态 |
|---|-----|------|------|------------|---------|--------|------|
| TM01 | 排行榜 | GET | `/v1/tool-models/leaderboard` | `ToolModelController` | `ToolModelService` | `ToolModelLeaderboardMapper` | ✅ |

---

## 2. 功能模块总览

| 模块 | 描述 | 核心 Service | 核心 Mapper | 核心 Entity | PRD 文档 |
|------|------|-------------|-------------|-------------|----------|
| Auth | 认证与授权 | `UserService`, `WorkspaceService` | `UserMapper`, `WorkspaceMapper` | `UserEntity` | D02, D13 |
| Search | 搜索服务 | `SearchService`, `VectorSearchService` | `SolutionMapper` | - | D12 |
| Trace | 轨迹管理 | `TraceService` | `EngineeringTraceMapper`, `TraceEvidenceRefMapper` | `EngineeringTraceEntity` | D06 |
| Solution | 方案管理 | `SolutionService` | `SolutionMapper`, `SolutionVersionMapper` | `SolutionEntity` | D08 |
| ProjectCase | 私有案例 | `ProjectCaseService` | `ProjectCaseMapper` | `ProjectCaseEntity` | D07 |
| PublicCase | 公开案例 | `PublicCaseService` | `PublicCaseMapper` | `PublicCaseEntity` | D07 |
| Workspace | 工作空间 | `WorkspaceService` | `WorkspaceMapper`, `MembershipMapper` | `WorkspaceEntity` | D13 |
| User | 用户管理 | `UserService` | `UserMapper` | `UserEntity` | D13 |
| Feedback | 反馈管理 | `FeedbackService` | `FeedbackMapper` | `FeedbackEntity` | D07 |
| Review | 审核治理 | `ReviewService`, `ReviewQueueService` | `ReviewMapper` | `ReviewEntity` | D14 |
| Connect | 连接管理 | `ConnectService`, `QuotaService`, `RateLimitService` | `ConnectSessionMapper` | `ConnectSessionEntity` | D09 |
| Invocation | 调用记录 | `InvocationService` | `InvocationMapper` | `InvocationEntity` | D09 |
| Policy | 策略引擎 | `PolicyEngineService` | - | - | D13 |
| Contribution | 贡献积分 | `ContributionService`, `ContributionLedgerService` | `ContributionLedgerMapper` | `ContributionLedgerEntity` | D15 |
| Seed | 候选种子 | `CandidateSeedService` | `CandidateSeedMapper` | `CandidateSeedEntity` | D12 |
| ToolModel | 工具模型 | `ToolModelService` | `ToolModelLeaderboardMapper`, `ToolModelAttributionMapper` | - | D09 |
| Nav | 导航菜单 | `NavService` | `MembershipMapper` | - | D03 |
| Rbac | 权限控制 | `RbacService` | `MembershipMapper` | - | D13 |
| Coverage | 覆盖率统计 | `CoverageStatsService` | - | - | D12 |
| AIReview | AI 审核 | `AIReviewService` | - | - | D14 |

---

## 3. 代码结构映射

### 3.1 模块目录结构

```
axiqra-project/axiqra-code/
├── axiqra-common/                    # 公共模块
│   └── src/main/java/com/axiqra/common/
│       ├── domain/
│       │   ├── dto/                   # 数据传输对象
│       │   ├── vo/                    # 视图对象
│       │   ├── entity/                # 实体类
│       │   └── enums/                 # 枚举
│       ├── exception/                 # 异常定义
│       └── response/                  # 响应封装
│
├── axiqra-core/                      # 核心业务模块
│   └── src/main/java/com/axiqra/core/
│       ├── service/                   # 业务服务 (27个)
│       ├── mapper/                    # 数据访问 (18个)
│       └── port/                      # 端口接口
│
├── axiqra-api/                       # API 模块
│   └── src/main/java/com/axiqra/api/
│       ├── controller/                # 控制器 (19个)
│       ├── filter/                    # 过滤器 (3个)
│       ├── annotation/                # 自定义注解
│       └── config/                    # 配置类
│
└── axiqra-start/                     # 启动模块
    └── src/main/resources/
        ├── application.yml            # 主配置
        ├── application-cloud.yml      # 云配置
        └── db/                        # 数据库脚本
```

### 3.2 核心文件路径

| 模块 | Service 文件 | Mapper 文件 |
|------|------------|------------|
| Auth | `axiqra-core/service/UserService.java` | `axiqra-core/mapper/UserMapper.java` |
| Search | `axiqra-core/service/SearchService.java`, `VectorSearchService.java` | `axiqra-core/mapper/SolutionMapper.java` |
| Trace | `axiqra-core/service/TraceService.java` | `axiqra-core/mapper/EngineeringTraceMapper.java` |
| Solution | `axiqra-core/service/SolutionService.java` | `axiqra-core/mapper/SolutionMapper.java` |
| Workspace | `axiqra-core/service/WorkspaceService.java` | `axiqra-core/mapper/WorkspaceMapper.java` |
| Review | `axiqra-core/service/ReviewService.java`, `ReviewQueueService.java` | `axiqra-core/mapper/ReviewMapper.java` |
| Connect | `axiqra-core/service/ConnectService.java` | `axiqra-core/mapper/ConnectSessionMapper.java` |

---

## 4. PRD 文档对照

| PRD 编号 | 文档名称 | 关联模块 | 关联 API |
|----------|----------|----------|----------|
| D01 | 文档总索引 | - | - |
| D02 | 产品核心定位 | Auth | A01-A09 |
| D03 | 页面体系与原型 | Nav | W01-W09 |
| D06 | 工程记忆对象模型 | Trace | T01-T06 |
| D07 | Case 内容规范 | ProjectCase, PublicCase, Feedback | C01-C08, F01-F03 |
| D08 | Solution 状态机 | Solution | SO01-SO06 |
| D09 | AI 工具接入协议 | Connect, Invocation, ToolModel | CO01-CO06, I01-I03, TM01 |
| D12 | 搜索索引体系 | Search, Seed | S01-S02, SD01-SD03 |
| D13 | 权限与空间方案 | Workspace, User, Policy | W01-W09, U01-U02, P01-P03 |
| D14 | 内容治理审核 | Review | R01-R06 |
| D15 | 贡献者激励 | Contribution | CG01-CG02 |

---

## 5. 同步机制

### 5.1 PR 同步检查清单

每次提交 PR 前，请确认以下项目：

- [ ] **API 变更**：如果修改了 Controller 或新增了端点，必须更新本文档第 1 节
- [ ] **Service 变更**：如果新增或重构了 Service，必须更新本文档第 2 节
- [ ] **Mapper 变更**：如果新增了 Mapper，必须添加到对应模块
- [ ] **PRD 映射**：如果变更影响产品行为，必须同步更新对应 PRD 文档
- [ ] **功能模块**：如果新增功能模块，必须在第 2 节添加完整条目

### 5.2 更新规则

1. **新增 API**：在对应模块表中添加新行，标注 `状态` 为 `🆕`
2. **修改 API**：更新路径、方法、Controller 名称，保留原行
3. **删除 API**：将 `状态` 改为 `❌`，保留行并标注废弃版本
4. **重构 Service**：更新 `Service` 列，保持 `Mapper` 列一致
5. **文档同步**：PR 描述中必须包含对本文档的更新说明

### 5.3 状态说明

| 状态 | 含义 |
|------|------|
| ✅ | 已实现，功能完整 |
| 🆕 | 新增，待完善 |
| ❌ | 已废弃 |
| ⚠️ | 部分实现，待补充 |

---

## 6. 统计摘要

| 类别 | 数量 |
|------|------|
| Controller | 19 |
| Service | 27 |
| Mapper | 18 |
| API 端点 | 59 |
| 功能模块 | 19 |

---

*本文档由 B4 任务自动生成，最后更新：2026-06-26*
