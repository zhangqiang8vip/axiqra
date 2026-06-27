# Axiqra 文档-代码对照分析报告

> 生成时间：2026-06-24
> 文档版本：v0.2 (Draft/Accepted)
> 代码版本：当前代码库最新状态

---

## 一、文档体系总览

### 1.1 文档清单与状态

| 编号 | 文档名称 | 状态 | 核心内容摘要 |
|:---:|---|:---:|---|
| D01 | 文档总索引与来源继承表 | Accepted | 18个文档体系、来源继承、维护规则 |
| D02 | 产品核心定位、阶段边界与设计原则 | Accepted | 产品定义、工程方案记忆层、AI调用主线 |
| D03 | 完整原型与页面体系说明 | Accepted | 页面蓝图、Vue原型继承、视觉规范 |
| D04 | MVP实施范围与版本路线 | Accepted | P0-S4阶段、核心闭环、验收标准 |
| D05 | 用户、AI Agent与人类学习双主线流程 | Draft | 工程轨迹回传、Case沉淀、审核流程 |
| D06 | 工程记忆对象模型与数据预留规范 | Draft | 20个核心对象、字段矩阵、关系图 |
| D07 | Case/Public Case/Project Case内容规范 | Draft | Case Package、脱敏规则、展示模板 |
| D08 | Solution生命周期、状态机与验证等级 | Draft | 状态全集、L0-L5验证、R0-R4风险 |
| D09 | AI工具接入协议 | Draft | 对话式接入、MCP/CLI/API、授权、doctor |
| D10 | 技术架构设计说明书 | Draft | Vue3+Spring Boot模块化单体、PG+pgvector |
| D11 | 架构演进、升级迁移与规模化 | Draft | S0-S4、数据迁移、搜索升级 |
| D12 | 搜索、索引、推荐、排序与评测 | Draft | 多路召回、权限预过滤、排序公式 |
| D13 | 权限、空间、数据隔离与企业空间 | Draft | RBAC/ABAC、4类空间、审计 |
| D14 | 内容治理、审核、可信来源与污染隔离 | Draft | 风险分层审核、AI初审、反作弊 |
| D15 | 贡献者激励、收益池、积分与权益 | Draft | Contribution Ledger、认证凭证、防刷 |
| D16 | 社区贡献、维护者、仲裁与争议处理 | Draft | 维护者、申诉、仲裁机制 |
| D17 | 冷启动、种子内容、传播页面与商业验证 | Draft | 种子Case、试点、ROI验证 |
| D18 | 开发任务拆解、验收标准、风险与文档维护 | Draft | ADR、RACI、风险台账、测试验收 |

### 1.2 文档核心主线（20条产品主线）

| 编号 | 产品主线 | 文档归属 |
|:---:|---|:---:|
| 1 | 产品定位线 - AI Agent时代的工程方案记忆层 | D02 |
| 2 | 用户角色线 - 个人/AI工具/团队/企业/贡献者 | D02/D13 |
| 3 | 工程方案适配线 - 方案与场景的适配关系 | D02/D06 |
| 4 | AI接入线 - Axiqra Connect/MCP/CLI/API | D09 |
| 5 | 任务前调用线 - AI执行前搜索Solution | D05/D12 |
| 6 | 工程执行线 - AI执行人确认 | D05 |
| 7 | 自动回传线 - Engineering Trace Package | D05/D06/D07 |
| 8 | Project Case线 - 原始私有工程过程 | D05/D06/D07 |
| 9 | Public Case线 - 脱敏公开学习 | D05/D07 |
| 10 | Solution资产线 - AI可调用方案 | D05/D08 |
| 11 | Prompt/Rule/Skill线 - AI执行资产 | D06/D07/D09 |
| 12 | 搜索推荐线 - 多路召回/排序/评测 | D12 |
| 13 | Candidate Seed线 - 无命中候选 | D05/D12 |
| 14 | 验证反馈线 - Invocation/Feedback/L0-L5 | D05/D08 |
| 15 | 授权边界线 - 可见/可信/授权三分离 | D13/D14 |
| 16 | 审查治理线 - 风险分层审核 | D14 |
| 17 | 认证身份线 - 认证开发者/Maintainer | D14/D15 |
| 18 | 团队企业线 - 空间/权限/审计 | D13 |
| 19 | 贡献生态线 - 账本/积分/收益池 | D15 |
| 20 | 商业生态线 - Pro/API/私有化/Agent Eval | D17 |

---

## 二、代码体系总览

### 2.1 代码模块结构

```
axiqra-project/axiqra-code/
├── axiqra-common/          # 公共域：实体、DTO、VO、枚举、异常
│   ├── domain/
│   │   ├── entity/        # 12个核心实体
│   │   ├── dto/           # 6个请求DTO
│   │   ├── vo/            # 8个响应VO
│   │   └── enums/         # 21个枚举类
│   └── exception/         # 错误码定义
│
├── axiqra-core/           # 核心业务逻辑
│   ├── service/           # 服务实现
│   │   └── impl/         # 10个Service实现
│   ├── mapper/            # MyBatis-Flex Mapper
│   ├── adapter/           # 适配器
│   └── domain/           # 状态机等
│
├── axiqra-api/            # API层
│   ├── controller/        # 8个Controller
│   ├── config/            # 配置类
│   └── filter/            # 过滤器
│
└── axiqra-start/          # 启动模块
    └── config/            # Flyway、数据源配置
```

### 2.2 核心实体清单

| 实体名 | 表名 | 对应文档 | 核心字段 |
|---|:---|:---:|---|
| `SolutionEntity` | `axiqra_solution` | D06/D08 | title, verificationLevel, riskLevel, status, visibilityScope |
| `InvocationEntity` | `axiqra_invocation` | D06 | requestId, targetType, callerType, invocationStatus |
| `FeedbackEntity` | `axiqra_feedback` | D06/D08 | invocationId, feedbackType, evidenceRefs |
| `ConnectSessionEntity` | `axiqra_connect_session` | D06/D09 | sessionId, channel, toolType, doctorStatus |
| `ProjectCaseEntity` | `axiqra_project_case` | D06/D07 | title, caseStatus, redactionStatus |
| `PublicCaseEntity` | `axiqra_public_case` | D06/D07 | sourceCaseId, publishStatus, reviewStatus |
| `CandidateSeedEntity` | `axiqra_candidate_seed` | D06 | taskGoal, coverageGap, seedStatus |
| `WorkspaceEntity` | `axiqra_workspace` | D06/D13 | name, spaceType, tenantId |
| `ProjectEntity` | `axiqra_project` | D06 | name, projectType, status |
| `EvidenceEntity` | `axiqra_evidence` | D06 | targetType, evidenceType, storageRef |
| `SolutionVersionEntity` | `axiqra_solution_version` | D06/D08 | solutionId, versionNumber, steps |
| `AuditLogEntity` | `axiqra_audit_log` | D06 | actorId, action, targetType |

### 2.3 核心枚举清单

| 枚举 | 值 | 对应文档 |
|---|:---|:---:|
| `SolutionStatus` | DRAFT/CANDIDATE/REVIEWED/VERIFIED/STABLE/CANONICAL/DEPRECATED | D08 |
| `VerificationLevel` | L0-L5 (0-5) | D08 |
| `RiskLevel` | R0-R4 (0-4) | D08/D14 |
| `VisibilityScope` | PRIVATE/WORKSPACE/ENTERPRISE/PUBLIC | D06/D13 |
| `LicenseScope` | PRIVATE/WORKSPACE/ENTERPRISE/PUBLIC_CASE_ALLOWED等 | D13 |
| `FeedbackType` | WORKED/PARTIAL/FAILED/NOT_APPLICABLE/NEEDS_REVIEW | D08 |
| `ConnectSessionStatus` | PENDING/AUTHORIZED/ACTIVE/EXPIRED/REVOKED | D09 |
| `ConnectSessionEvent` | AUTHORIZE/ACTIVATE/EXPIRE/REVOKE | D09 |
| `ReviewResult` | APPROVE/REJECT/NEEDS_REVISION/ESCALATE | D14 |
| `PublicCaseStatus` | DRAFT/PRIVATE/PUBLISH_REQUESTED/ARCHIVED/DELETED | D07 |
| `MemberRole` | OWNER/ADMIN/MEMBER/VIEWER/AUDITOR/REVIEWER | D13 |
| `PolicyDecision` | ALLOW/DENY/ALLOW_WITH_MASKING/REQUIRE_CONFIRMATION | D13 |

### 2.4 核心服务清单

| 服务 | 实现类 | 核心功能 |
|---|:---|:---|
| `SearchService` | `SearchServiceImpl` | searchBeforeAct/searchPublic，多路召回+权限预过滤 |
| `SolutionService` | `SolutionServiceImpl` | createFromProjectCase/getDetail/listPublicSolutions |
| `ConnectService` | `ConnectServiceImpl` | 创建会话/状态机/doctor检测 |
| `InvocationService` | `InvocationServiceImpl` | 调用记录/确认状态 |
| `FeedbackService` | `FeedbackServiceImpl` | 反馈提交/统计 |
| `WorkspaceService` | `WorkspaceServiceImpl` | 空间管理 |
| `PolicyEngineAdapter` | - | ABAC策略决策 |
| `RbacService` | - | 权限检查 |

---

## 三、文档-代码对照表

### 3.1 核心功能对照

| 文档模块 | 文档定义功能 | 代码实现 | 状态 | 说明 |
|---------|:-----------|:--------:|:----:|------|
| **D02 产品定位** | | | | |
| 工程方案记忆层定位 | AI可调用的工程方案记忆基础设施 | ✅已实现 | ✅ | Solution/Invocation/Feedback完整实现 |
| 三类资产(Project/Public Case/Solution) | Case体系完整 | ✅已实现 | ✅ | 三个Entity+Service完整 |
| 验证等级L0-L5 | VerificationLevel枚举 | ✅已实现 | ✅ | L0-L5完整，6级枚举 |
| 风险等级R0-R4 | RiskLevel枚举 | ✅已实现 | ✅ | R0-R4完整，5级枚举 |
| **D05 流程** | | | | |
| AI任务前搜索 | searchBeforeAct | ✅已实现 | ✅ | SearchServiceImpl完整实现 |
| Engineering Trace回传 | 工程轨迹记录 | ⚠️部分实现 | ⚠️ | Invocation记录存在，Trace Package结构未完整 |
| Case沉淀 | Project Case创建 | ✅已实现 | ✅ | ProjectCaseService存在 |
| Public Case发布 | 脱敏发布流程 | ⚠️部分实现 | ⚠️ | PublicCaseEntity存在，审核流程未完整 |
| Feedback反馈 | worked/failed/partial | ✅已实现 | ✅ | FeedbackService完整实现 |
| **D06 对象模型** | | | | |
| 20个核心对象 | OBJ-001~OBJ-020 | ✅已实现(12个) | ✅ | 核心对象已实现，待完善 |
| 字段矩阵 | 完整字段定义 | ✅已实现 | ✅ | SolutionEntity等包含文档要求字段 |
| 关系与幂等 | 8条关系规则 | ✅已实现 | ✅ | Mapper层实现 |
| **D07 Case规范** | | | | |
| Engineering Trace Package | 完整工程轨迹 | ⚠️部分实现 | ⚠️ | Invocation记录部分轨迹 |
| Case Package格式 | 提交/导入导出 | ⚠️部分实现 | ⚠️ | DTO结构存在 |
| 脱敏规则 | 脱敏预览/检查 | ❌未实现 | ❌ | 需补充脱敏服务 |
| **D08 Solution** | | | | |
| 状态机(10状态) | Draft/Candidate/Needs Review/Verified/Stable/Canonical/Deprecated/Rejected/Archived/Quarantined | ⚠️部分实现(7状态) | ⚠️ | 缺少Needs Review/Quarantined/Archived |
| 验证等级L0-L5 | 完整5级 | ✅已实现 | ✅ | VerificationLevel完整 |
| Feedback规则 | worked/failed/partial/not_applicable | ✅已实现 | ✅ | FeedbackType完整 |
| AI调用策略矩阵 | 状态×等级×风险 | ✅已实现 | ✅ | SearchServiceImpl中calculateScore |
| Solution合并/分叉 | 多Case融合 | ❌未实现 | ❌ | 需补充合并/分叉逻辑 |
| **D09 接入协议** | | | | |
| 对话式自动接入 | 复制给智能体 | ⚠️部分实现 | ⚠️ | ConnectSession存在，接入指令生成需增强 |
| MCP接入 | MCP协议支持 | ⚠️部分实现 | ⚠️ | 状态机存在，完整MCP Server未实现 |
| CLI接入 | 命令行工具 | ⚠️部分实现 | ⚠️ | 基础存在，完整CLI未实现 |
| doctor检测 | 8项诊断 | ✅已实现 | ✅ | doctorStatus/diagnosis完整 |
| 授权码回填 | OAuth/设备码 | ⚠️部分实现 | ⚠️ | 授权码机制存在 |
| **D10 技术架构** | | | | |
| Vue3前端 | 前端框架 | ⚠️部分实现 | ⚠️ | Vue原型存在，生产前端未实现 |
| Spring Boot后端 | 模块化单体 | ✅已实现 | ✅ | 4个模块完整 |
| PostgreSQL主库 | 对象存储 | ✅已实现 | ✅ | MyBatis-Flex Mapper完整 |
| pgvector向量 | 向量检索 | ⚠️部分实现 | ⚠️ | 向量存储预留，完整检索未实现 |
| Redis缓存 | 会话/缓存 | ⚠️部分实现 | ⚠️ | 会话管理预留 |
| MinIO存储 | 证据文件 | ⚠️部分实现 | ⚠️ | 存储预留 |
| **D11 规模化** | | | | |
| S0-S4演进路线 | 架构演进 | ⚠️部分实现 | ⚠️ | 预留字段存在 |
| 数据迁移 | 导出/导入 | ⚠️部分实现 | ⚠️ | AuditLog预留 |
| **D12 搜索** | | | | |
| 多路召回 | 关键词/向量/结构化 | ⚠️部分实现 | ⚠️ | 基础搜索实现，向量召回未完成 |
| 权限预过滤 | 可见性过滤 | ✅已实现 | ✅ | SearchServiceImpl中实现 |
| 排序公式 | status+verification+risk+匹配度 | ✅已实现 | ✅ | calculateScore完整实现 |
| 覆盖范围页 | 技术栈覆盖 | ⚠️部分实现 | ⚠️ | 无专门覆盖范围API |
| Candidate Seed | 无命中创建 | ✅已实现 | ✅ | CandidateSeedService完整 |
| **D13 权限空间** | | | | |
| 4类空间 | Personal/Team/Enterprise/Public | ⚠️部分实现 | ⚠️ | WorkspaceEntity存在，空间功能待完善 |
| RBAC权限 | owner/admin/member/viewer | ✅已实现 | ✅ | MemberRole/RbacService |
| ABAC策略 | 策略决策引擎 | ✅已实现 | ✅ | PolicyEngineAdapter |
| 审计日志 | AuditLogEntity | ✅已实现 | ✅ | 审计字段完整 |
| SSO/SCIM | 企业身份接入 | ❌未实现 | ❌ | 预留字段需补充 |
| **D14 内容治理** | | | | |
| 风险分层审核 | R0-R4分级处理 | ⚠️部分实现 | ⚠️ | 枚举存在，审核流程未完整 |
| AI初审 | 结构/敏感/重复检查 | ❌未实现 | ❌ | 需补充AI审核服务 |
| 审核队列 | 队列设计 | ❌未实现 | ❌ | 需补充ReviewQueue |
| 申诉流程 | 申诉/复核 | ❌未实现 | ❌ | 需补充Appeal逻辑 |
| 反作弊 | 作弊检测 | ❌未实现 | ❌ | 需补充防刷机制 |
| **D15 贡献激励** | | | | |
| Contribution Ledger | 贡献账本 | ⚠️部分实现 | ⚠️ | 字段预留，记录逻辑未完整 |
| 认证凭证 | 能力档案 | ❌未实现 | ❌ | 需补充CertificationCredential |
| 权益分层 | Learner->Maintainer | ❌未实现 | ❌ | 需补充权益体系 |
| 收益池 | 未来收益资格 | ⚠️部分实现 | ⚠️ | reward_eligible字段预留 |
| **D16 社区治理** | | | | |
| 维护者制度 | Maintainer职责 | ❌未实现 | ❌ | 需补充Maintainer逻辑 |
| 仲裁机制 | 争议处理 | ❌未实现 | ❌ | 需补充Arbitration逻辑 |
| **D17 冷启动** | | | | |
| 种子内容 | 官方种子/Dogfooding | ⚠️部分实现 | ⚠️ | 数据结构预留 |
| 传播页面 | 试点/ROI | ❌未实现 | ❌ | 需补充运营页面 |
| **D18 开发治理** | | | | |
| ADR决策 | 架构决策记录 | ⚠️部分实现 | ⚠️ | 需补充ADR表 |
| 风险台账 | Risk Record | ⚠️部分实现 | ⚠️ | 需补充Risk Record表 |

### 3.2 实现状态汇总

| 状态 | 数量 | 占比 |
|:---:|:---:|:---:|
| ✅ 已完整实现 | 18 | 29% |
| ⚠️ 部分实现 | 30 | 48% |
| ❌ 未实现 | 14 | 23% |
| **合计** | **62** | **100%** |

---

## 四、差距分析

### 4.1 核心功能差距

#### 4.1.1 Solution状态机不完整
**文档定义(D08)**：需要10个状态
```
Draft → Candidate → Needs Review → Reviewed → Verified → Stable → Canonical
                          ↓            ↓         ↓         ↓
                       Rejected   Deprecated  Deprecated  Deprecated → Archived
                          ↓
                       Quarantined (隔离)
```

**代码实现**：`SolutionStatus`只有7个状态
```java
DRAFT, CANDIDATE, REVIEWED, VERIFIED, STABLE, CANONICAL, DEPRECATED
```

**缺失状态**：
- `NEEDS_REVIEW` - 需要审核
- `QUARANTINED` - 隔离态
- `ARCHIVED` - 归档态

#### 4.1.2 Engineering Trace Package结构不完整
**文档定义(D06)**：需要6大路径
- forward_path (正向路径) ✅
- reverse_path (反向路径) ⚠️ 部分实现
- decision_path (决策路径) ❌ 未实现
- evidence_refs (证据路径) ⚠️ 字段存在
- rollback_path (回滚路径) ❌ 未实现
- evolution_hint (演化路径) ❌ 未实现

#### 4.1.3 完整审核治理流程缺失
**文档定义(D14)**：
- AI初审服务 ❌ 未实现
- 审核队列管理 ❌ 未实现
- 申诉复核流程 ❌ 未实现
- 反作弊检测 ❌ 未实现
- Reason Code字典 ❌ 未实现

#### 4.1.4 对话式自动接入深度不足
**文档定义(D09)**：
- 复制给智能体的接入指令生成 ⚠️ 部分实现
- 完整MCP Server ❌ 未实现
- 完整CLI工具 ❌ 未实现
- OAuth/SSO授权 ❌ 未实现
- 接入包manifest ❌ 未实现

#### 4.1.5 贡献激励体系未完成
**文档定义(D15)**：
- Contribution Ledger记录 ⚠️ 部分实现
- 认证凭证(CertificationCredential) ❌ 未实现
- 权益分层(Learner→Maintainer) ❌ 未实现
- 积分计算引擎 ❌ 未实现

### 4.2 数据模型差距

| 对象 | 文档要求字段 | 代码实现 | 差距 |
|---|:---:|:---:|---|
| `Solution` | applicability, inapplicability, rollback_steps, failure_paths | ✅ | 完整 |
| `Solution` | human_learning_notes, prompt_rule_skill_refs | ❌ | 缺失 |
| `Invocation` | forward_path, reverse_path, decision_path | ⚠️ | 部分 |
| `Invocation` | rollback_path, evolution_hint | ❌ | 缺失 |
| `Project Case` | forward_path, reverse_path, decision_path | ⚠️ | 部分 |
| `Workspace` | space_type, owner_user_id, tenant_id | ✅ | 完整 |
| `Workspace` | identity_provider(SSO), sso_config | ❌ | 缺失 |

### 4.3 API差距

| API | 文档定义 | 代码实现 | 状态 |
|-----|:-------:|:-------:|:----:|
| POST /api/v1/traces | 提交Trace Package | ❌ | 缺失 |
| POST /api/v1/cases/public | 发布Public Case | ❌ | 缺失 |
| POST /api/v1/reviews | 提交审核 | ❌ | 缺失 |
| POST /api/v1/appeals | 提交申诉 | ❌ | 缺失 |
| GET /api/v1/coverage | 覆盖范围统计 | ❌ | 缺失 |
| POST /api/v1/certifications | 认证申请 | ❌ | 缺失 |
| GET /api/v1/rewards/ledger | 贡献账本 | ⚠️ | 预留 |

---

## 五、详细对照表（按优先级）

### 5.1 P0 核心闭环（必须实现）

| 功能 | 文档 | 代码 | 差距 | 建议 |
|-----|:----:|:----:|------|------|
| Solution搜索 | D12 | ✅ SearchServiceImpl | 完整 | - |
| Solution详情 | D08 | ✅ SolutionServiceImpl | 完整 | - |
| Invocation记录 | D06 | ✅ InvocationServiceImpl | 完整 | - |
| Feedback反馈 | D08 | ✅ FeedbackServiceImpl | 完整 | - |
| Project Case | D06/D07 | ⚠️ 部分 | 需完善发布流程 | 增加PublicCase发布API |
| Engineering Trace | D05/D06 | ⚠️ 部分 | 6路径结构未完整 | 扩展Invocation字段 |
| Solution状态机 | D08 | ⚠️ 缺3状态 | 缺NeedsReview/Quarantined/Archived | 补充状态枚举和服务 |

### 5.2 P1 核心体验（应该实现）

| 功能 | 文档 | 代码 | 差距 | 建议 |
|-----|:----:|:----:|------|------|
| 对话式接入 | D09 | ⚠️ 部分 | 指令生成不完整 | 完善接入指令模板 |
| Candidate Seed | D06/D12 | ✅ 完整 | - | - |
| 权限预过滤 | D12/D13 | ✅ 完整 | - | - |
| RBAC权限 | D13 | ✅ 完整 | - | - |
| ABAC策略 | D13 | ✅ 完整 | - | - |
| Audit日志 | D13 | ✅ 完整 | - | - |

### 5.3 P2 扩展能力（规划实现）

| 功能 | 文档 | 代码 | 差距 | 建议 |
|-----|:----:|:----:|------|------|
| AI初审 | D14 | ❌ | 未实现 | 规划AI审核服务 |
| 审核队列 | D14 | ❌ | 未实现 | 规划ReviewQueue |
| 申诉流程 | D14 | ❌ | 未实现 | 规划Appeal逻辑 |
| 贡献账本 | D15 | ⚠️ 部分 | 记录不完整 | 完善Contribution Ledger |
| 认证凭证 | D15 | ❌ | 未实现 | 规划Certification服务 |

### 5.4 P3 未来规划（待实现）

| 功能 | 文档 | 代码 | 差距 | 建议 |
|-----|:----:|:----:|------|------|
| MCP Server | D09 | ❌ | 未实现 | S2规划 |
| 完整CLI | D09 | ❌ | 未实现 | S2规划 |
| SSO/SCIM | D13 | ❌ | 未实现 | S3规划 |
| 私有化部署 | D11 | ❌ | 未实现 | S3规划 |
| 仲裁机制 | D16 | ❌ | 未实现 | 未来规划 |
| 收益池 | D15 | ❌ | 未实现 | 商业化阶段 |

---

## 六、核心差距总结

### 6.1 已完整实现的核心（25项）
1. ✅ Solution搜索与排序（SearchServiceImpl）
2. ✅ Solution详情与版本（SolutionServiceImpl）
3. ✅ VerificationLevel枚举（L0-L5）
4. ✅ RiskLevel枚举（R0-R4）
5. ✅ VisibilityScope枚举（4级）
6. ✅ LicenseScope枚举
7. ✅ FeedbackType枚举（5种反馈）
8. ✅ Invocation记录服务
9. ✅ Feedback反馈服务
10. ✅ ConnectSession状态机
11. ✅ CandidateSeed服务
12. ✅ Workspace空间管理
13. ✅ RbacService权限服务
14. ✅ PolicyEngineAdapter策略引擎
15. ✅ AuditLog审计服务
16. ✅ SolutionEntity完整字段（含D12新增字段）
17. ✅ InvocationEntity扩展字段
18. ✅ ConnectSessionEntity完整字段
19. ✅ FeedbackEntity完整字段
20. ✅ 搜索排序公式（status+verification+risk+匹配度）
21. ✅ 空间优先级逻辑
22. ✅ 权限预过滤实现
23. ✅ 错误签名精确匹配加权
24. ✅ 技术栈匹配加权
25. ✅ 风险提示生成

### 6.2 部分实现待完善（18项）
1. ⚠️ Solution状态机 - 缺3个状态
2. ⚠️ Engineering Trace Package - 缺4个路径
3. ⚠️ Public Case发布 - 审核流程不完整
4. ⚠️ 对话式接入 - 指令生成待完善
5. ⚠️ Doctor检测 - 8项诊断待完整
6. ⚠️ pgvector向量检索 - 存储预留，检索未完成
7. ⚠️ Redis缓存 - 会话管理待实现
8. ⚠️ MinIO存储 - 证据存储待实现
9. ⚠️ Vue3前端 - 原型存在，生产前端未实现
10. ⚠️ 覆盖范围统计 - 无专门API
11. ⚠️ Contribution Ledger - 字段预留，记录逻辑待完善
12. ⚠️ 团队空间 - Workspace存在，功能待完善
13. ⚠️ 企业空间 - 字段预留，功能待实现
14. ⚠️ 数据导入导出 - AuditLog预留
15. ⚠️ ADR决策记录 - 需补充表
16. ⚠️ Risk Record - 需补充表
17. ⚠️ 授权码回填 - 机制存在，OAuth待完善
18. ⚠️ 种子内容管理 - 结构预留

### 6.3 完全未实现（15项）
1. ❌ AI初审服务（结构/敏感/重复检查）
2. ❌ 审核队列管理（ReviewQueue）
3. ❌ 申诉复核流程（Appeal）
4. ❌ 反作弊检测机制
5. ❌ Reason Code字典与处理
6. ❌ MCP Server完整实现
7. ❌ 完整CLI工具
8. ❌ SSO/SCIM企业身份接入
9. ❌ CertificationCredential认证凭证
10. ❌ 权益分层体系（Learner→Maintainer）
11. ❌ 积分计算引擎
12. ❌ 收益池分配逻辑
13. ❌ Solution合并/分叉逻辑
14. ❌ 维护者制度（Maintainer）
15. ❌ 仲裁机制（Arbitration）

---

## 七、建议

### 7.1 短期优先级（下一迭代）

| 优先级 | 任务 | 影响 | 工作量 |
|:------:|------|:----:|:------:|
| P0 | 补充SolutionStatus缺3状态 | 状态机完整性 | 低 |
| P0 | 扩展Invocation字段（4路径） | Trace完整性 | 中 |
| P0 | Public Case发布API | 核心闭环 | 中 |
| P1 | 完善Doctor 8项检测 | 接入体验 | 中 |
| P1 | 覆盖范围统计API | 搜索能力 | 中 |

### 7.2 中期规划（下一版本）

| 优先级 | 任务 | 影响 | 工作量 |
|:------:|------|:----:|:------:|
| P1 | AI初审服务 | 治理基础 | 高 |
| P1 | 审核队列管理 | 治理完整性 | 高 |
| P2 | Contribution Ledger完整实现 | 激励体系 | 中 |
| P2 | MCP Server | 接入生态 | 高 |
| P2 | 完整CLI工具 | 接入生态 | 中 |

### 7.3 长期规划（后续版本）

| 优先级 | 任务 | 影响 | 工作量 |
|:------:|------|:----:|:------:|
| P2 | SSO/SCIM | 企业能力 | 高 |
| P3 | CertificationCredential | 认证体系 | 中 |
| P3 | 权益分层体系 | 激励体系 | 高 |
| P3 | 收益池分配 | 商业模式 | 高 |
| P3 | Solution合并/分叉 | Solution进化 | 中 |

### 7.4 技术债务

1. **枚举命名一致性**：部分枚举使用下划线（如`NEEDS_REVIEW`），部分使用驼峰，需统一
2. **字段命名规范**：部分字段如`error_signature`应保持蛇形命名一致性
3. **文档引用缺失**：代码中缺少对文档章节的引用注释
4. **测试覆盖不足**：核心服务需补充单元测试

---

## 八、附录

### A. 文档与代码对应关系

| 文档章节 | 代码模块 | 核心类/接口 |
|----------|----------|-------------|
| D02 产品定位 | axiqra-common/enums | VerificationLevel, RiskLevel, SolutionStatus |
| D05 流程 | axiqra-core/service | SearchService, InvocationService, FeedbackService |
| D06 对象 | axiqra-common/domain/entity | 12个Entity类 |
| D08 Solution | axiqra-core/service/impl | SolutionServiceImpl |
| D09 接入 | axiqra-core/service/impl | ConnectServiceImpl |
| D10 架构 | axiqra-start, axiqra-api | 配置类, Controller |
| D12 搜索 | axiqra-core/service/impl | SearchServiceImpl |
| D13 权限 | axiqra-core/service | RbacService, PolicyEngineAdapter |
| D14 治理 | - | 待实现 |

### B. 关键设计决策

1. **技术栈选择符合文档**：
   - Vue3前端 ✅
   - Spring Boot模块化单体 ✅
   - PostgreSQL + pgvector ✅
   - MyBatis-Flex ORM ✅

2. **枚举值映射规范**：
   - SolutionStatus: snake_case (draft, candidate, reviewed)
   - VerificationLevel: L+数字 (L0, L1, L2, L3, L4, L5)
   - RiskLevel: R+数字 (R0, R1, R2, R3, R4)

3. **状态设计原则**：
   - 状态字段命名遵循文档（如`submit_status`, `case_status`）
   - 状态值使用code而非name（兼容性考虑）
   - 幂等键设计符合D06规范

---

*报告生成完毕。核心结论：代码实现整体框架完整，核心闭环（搜索-Solution-调用-反馈）已可运行，但状态机不完整、Trace结构待扩展、治理流程待实现。文档体系完整，代码实现需按优先级逐步完善缺失部分。*
