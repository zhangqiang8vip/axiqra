# TASK 4 - P0 / S1 / S2 Staged Development Plan

目标：把 Swagger 接口盘点、热部署、框架配置优先改造拆成可执行开发任务，且不改变 Axiqra 的产品边界。

## 一、任务总览

| 阶段 | 任务编号 | 任务名称 | 目标 | 交付物 | 是否阻塞后续 |
| -- | -- | -- | -- | -- | -- |
| P0 | TASK-DEV-001 | 运行时 Swagger 接口盘点 | 启动项目并导出 `/api/v3/api-docs` | 运行时接口盘点表 | 是 |
| P0 | TASK-DEV-002 | Swagger 认证规则核对 | 对比 Sa-Token、scope、role 与 OpenAPI | 认证接口清单 | 是 |
| P0 | TASK-DEV-003 | 接口用途与模块归属表生成 | 给每个接口补用途、模块、风险 | 接口用途表 | 是 |
| P0 | TASK-DEV-004 | API 路径叠加异常修复 | 去除 Controller 内重复 `/api` 前缀 | 已修正 Controller | 是 |
| P0 | TASK-DEV-005 | OpenAPI Security Scheme 修正 | 声明 Sa-Token Bearer | `OpenApiConfig` | 是 |
| S1 | TASK-DEV-006 | Spring Boot DevTools 本地热部署 | 本机后端自动 restart | DevTools 配置与验收 | 否 |
| S1 | TASK-DEV-007 | Vite HMR 前端热更新 | 未来前端 HMR 方案 | Vite dev 配置草案 | 否 |
| S1 | TASK-DEV-008 | Docker Compose 本地依赖服务开发模式 | DB/Redis/MinIO/RabbitMQ 容器化 | compose 启动说明 | 否 |
| S1 | TASK-DEV-009 | Docker Compose watch / bind mount 方案评估 | 评估后端容器化热更新 | 评估记录 | 否 |
| S1 | TASK-DEV-010 | Candidate Seed Controller 暴露 | 补齐草案接口 | Controller + tests | 否 |
| S2 | TASK-DEV-011 | 统一错误结构与 Swagger schema 对齐 | 让错误响应可生成文档 | schema + tests | 否 |
| S2 | TASK-DEV-012 | 框架优先重构候选清单落实 | 分批处理健康检查、Flyway 等 | 重构 PR | 否 |

## 二、详细任务

任务编号：TASK-DEV-001  
任务名称：运行时 Swagger 接口盘点  
任务目的：把静态扫描结果升级为运行时事实。  
使用角色：后端工程师。  
触发入口：`GET /api/v3/api-docs`、`GET /api/doc.html`。  
前置条件：依赖服务可用，后端可启动。  
输入内容：OpenAPI JSON、Controller 源码。  
输入校验：JSON 可解析，paths 不为空。  
正常流程：启动后端、导出 OpenAPI、按 method+path 去重、与静态表对比。  
异常流程：启动失败时标注“静态扫描结果，未经过运行时 Swagger 验证”。  
权限控制：Swagger 文档端点按开发环境策略访问。  
数据保存：保存到 `REPORTS/task-1-swagger-openapi-inventory.md`。  
状态变化：无业务状态变化。  
API 需求：API-SWAGGER-001。  
前端显示：Knife4j UI。  
后端处理：Springdoc/Knife4j 扫描 Controller。  
日志 / 审计：记录启动失败原因。  
验收标准：AC-SWAGGER-001：OpenAPI JSON 可访问且接口数可复现。  
测试用例：TC-SWAGGER-001。  
优先级：P0。  
版本阶段：P0。  
风险：依赖服务不可用导致只能静态扫描。

任务编号：TASK-DEV-002  
任务名称：Swagger 认证规则核对  
任务目的：确认公开、登录、scope、role 的实际行为。  
使用角色：后端工程师、安全审查者。  
触发入口：Controller 注解、`SaTokenConfig`、`SecurityConfig`。  
前置条件：TASK-DEV-001 完成。  
输入内容：接口清单、认证配置。  
输入校验：每个接口都有认证分类。  
正常流程：未登录访问应得到 401；scope 不足应得到 403；公开接口应可访问。  
异常流程：无法确认时标为待确认。  
权限控制：Sa-Token、`@RequireScope`、`@RequireWorkspaceRole`。  
数据保存：认证清单。  
状态变化：无。  
API 需求：PERM-SWAGGER-001/002。  
前端显示：Swagger 锁图标。  
后端处理：拦截器与 Filter 链。  
日志 / 审计：安全异常记录 trace_id。  
验收标准：AC-AUTH-001：认证分类与运行时响应一致。  
测试用例：TC-AUTH-001。  
优先级：P0。  
版本阶段：P0。  
风险：OpenAPI 全局 security 不能表达健康检查例外。

任务编号：TASK-DEV-003  
任务名称：接口用途与模块归属表生成  
任务目的：让每个接口可追溯到业务模块。  
使用角色：产品、后端、测试。  
触发入口：Controller `@Operation`。  
前置条件：TASK-DEV-001 静态或运行时表。  
输入内容：接口列表、业务草案。  
输入校验：每行有用途说明。  
正常流程：按 Auth、Connect、Trace、Case、Review 等模块归组。  
异常流程：用途不明时标待产品确认。  
权限控制：继承接口认证分类。  
数据保存：接口用途表。  
状态变化：无。  
API 需求：API-DOCS-001。  
前端显示：无。  
后端处理：无。  
日志 / 审计：无。  
验收标准：AC-DOCS-001：所有接口有模块和用途。  
测试用例：TC-DOCS-001。  
优先级：P0。  
版本阶段：P0。  
风险：文档草案与代码 method/path 不一致。

任务编号：TASK-DEV-004  
任务名称：API 路径叠加异常修复  
任务目的：避免 `/api/api/v1/...`。  
使用角色：后端工程师。  
触发入口：5 个 Controller 的 `@RequestMapping`。  
前置条件：代码可编译。  
输入内容：`/api/v1/...` 注解。  
输入校验：只改类级 path，不改业务方法。  
正常流程：改为 `/v1/...`。  
异常流程：若外部客户端已依赖旧路径，增加兼容别名或迁移通知。  
权限控制：不变。  
数据保存：源码变更。  
状态变化：API 外部路径变为预期 `/api/v1/...`。  
API 需求：API-PATH-001。  
前端显示：Swagger paths 更新。  
后端处理：Spring MVC mapping。  
日志 / 审计：无。  
验收标准：AC-PATH-001：OpenAPI 不再出现 `/api/api/`。  
测试用例：TC-PATH-001。  
优先级：P0。  
版本阶段：P0。  
风险：旧客户端 404。

任务编号：TASK-DEV-005  
任务名称：OpenAPI Security Scheme 修正  
任务目的：让 Swagger/Knife4j 显示 Sa-Token Bearer 认证。  
使用角色：后端工程师。  
触发入口：`OpenApiConfig`。  
前置条件：Knife4j starter 可用。  
输入内容：Bearer scheme 名称和 header 策略。  
输入校验：`Authorization: Bearer <token>` 文档清晰。  
正常流程：添加 `@SecurityScheme` 与全局 `@SecurityRequirement`。  
异常流程：若健康检查被错误标锁，文档备注公开例外或改用 OpenApiCustomizer。  
权限控制：仅文档层，不改变运行时认证。  
数据保存：源码变更。  
状态变化：Swagger schema 变化。  
API 需求：API-SWAGGER-002。  
前端显示：Knife4j Authorize。  
后端处理：Springdoc 扫描注解。  
日志 / 审计：无。  
验收标准：AC-SWAGGER-002：UI 可输入 token。  
测试用例：TC-SWAGGER-002。  
优先级：P0。  
版本阶段：P0。  
风险：公开接口被全局 security 标记。

任务编号：TASK-DEV-006  
任务名称：Spring Boot DevTools 本地热部署  
任务目的：提升本机后端开发效率。  
使用角色：后端工程师。  
触发入口：`mvn spring-boot:run -pl axiqra-start -am`。  
前置条件：JDK 17、Maven、依赖服务。  
输入内容：Java 源码变更。  
输入校验：编译通过。  
正常流程：DevTools 自动 restart。  
异常流程：编译失败时控制台展示错误。  
权限控制：无业务权限变化。  
数据保存：无。  
状态变化：应用上下文重启。  
API 需求：TASK-HOT-001。  
前端显示：无。  
后端处理：DevTools restart classloader。  
日志 / 审计：控制台 restart 日志。  
验收标准：AC-HOT-001。  
测试用例：TC-HOT-001。  
优先级：S1。  
版本阶段：S1。  
风险：仅开发期生效，不替代生产发布。

任务编号：TASK-DEV-007  
任务名称：Vite HMR 前端热更新  
任务目的：未来 Vue 前端具备 HMR。  
使用角色：前端工程师。  
触发入口：`npm run dev` 或 `pnpm dev`。  
前置条件：创建 Vue 3 + Vite 项目。  
输入内容：Vue 组件变更。  
输入校验：TypeScript 编译通过。  
正常流程：Vite HMR 更新浏览器。  
异常流程：HMR 失败时全页 reload。  
权限控制：前端路由守卫。  
数据保存：无。  
状态变化：浏览器模块更新。  
API 需求：API-FE-001。  
前端显示：页面即时更新。  
后端处理：无。  
日志 / 审计：Vite 控制台。  
验收标准：AC-HOT-002。  
测试用例：TC-HOT-004。  
优先级：S1。  
版本阶段：S1。  
风险：当前无前端项目，不能本轮验证。

任务编号：TASK-DEV-008  
任务名称：Docker Compose 本地依赖服务开发模式  
任务目的：让 DB、Redis、RabbitMQ、MinIO 保持容器化且数据持久。  
使用角色：全栈工程师。  
触发入口：`docker compose up -d`。  
前置条件：Docker 可用，`.env` 完整。  
输入内容：compose 环境变量。  
输入校验：容器 healthcheck 通过。  
正常流程：启动依赖，后端本机连接。  
异常流程：healthcheck 失败时查看 compose logs。  
权限控制：本机端口绑定 127.0.0.1。  
数据保存：named volumes。  
状态变化：依赖服务启动/停止。  
API 需求：INFRA-DEV-001。  
前端显示：无。  
后端处理：连接外部依赖。  
日志 / 审计：Docker logs。  
验收标准：AC-HOT-003。  
测试用例：TC-HOT-003。  
优先级：S1。  
版本阶段：S1。  
风险：环境变量缺失导致容器失败。

任务编号：TASK-DEV-009  
任务名称：Docker Compose watch / bind mount 方案评估  
任务目的：评估全容器化开发的成本收益。  
使用角色：DevOps、后端工程师。  
触发入口：`docker compose watch`。  
前置条件：恢复并修正 `axiqra_core` 服务。  
输入内容：compose develop.watch 配置。  
输入校验：watch 配置符合 Docker Compose spec。  
正常流程：源码变更触发 rebuild 或 sync。  
异常流程：Windows 文件监听慢时回退 A 方案。  
权限控制：不改变业务权限。  
数据保存：无。  
状态变化：容器重建或重启。  
API 需求：INFRA-WATCH-001。  
前端显示：无。  
后端处理：容器内 Java 进程重启。  
日志 / 审计：Docker watch logs。  
验收标准：AC-HOT-004。  
测试用例：TC-WATCH-001。  
优先级：S1。  
版本阶段：S1。  
风险：文件监听风暴、Windows 性能问题。

任务编号：TASK-DEV-010  
任务名称：Candidate Seed Controller 暴露  
任务目的：补齐文档草案 `POST /api/candidate-seeds`。  
使用角色：后端工程师。  
触发入口：`CandidateSeedController`。  
前置条件：确认产品需要 REST 暴露。  
输入内容：CandidateSeed DTO。  
输入校验：Bean Validation。  
正常流程：Controller 调用 `CandidateSeedService`。  
异常流程：重复 seed、权限不足、参数错误。  
权限控制：需登录，scope 待定。  
数据保存：candidate seed 表。  
状态变化：新增 CandidateSeed。  
API 需求：API-SEED-001。  
前端显示：无。  
后端处理：Service 保存。  
日志 / 审计：创建事件审计。  
验收标准：AC-SEED-001。  
测试用例：TC-SEED-001。  
优先级：S1。  
版本阶段：S1。  
风险：Service 已有但 REST 契约未定。

## 三、最终提交物

| 编号 | 内容 | 文件 |
| -- | -- | -- |
| DELIVERABLE-001 | Swagger 接口盘点表 | `REPORTS/task-1-swagger-openapi-inventory.md` |
| DELIVERABLE-002 | 认证接口清单 | `REPORTS/task-1-swagger-openapi-inventory.md` |
| DELIVERABLE-003 | 接口用途表 | `REPORTS/task-1-swagger-openapi-inventory.md` |
| DELIVERABLE-004 | 热部署方案说明 | `REPORTS/task-2-hot-reload-options.md` |
| DELIVERABLE-005 | 需要修改的配置文件清单 | `REPORTS/task-2-hot-reload-options.md` |
| DELIVERABLE-006 | 阶梯式开发任务表 | `REPORTS/task-4-staged-development-plan.md` |
| DELIVERABLE-007 | 不能用框架解决、需要决策清单 | `REPORTS/task-3-framework-first-review.md` |
| DELIVERABLE-008 | 风险与回滚方案 | 本文件下节 |

## 四、风险与回滚方案

| 风险 | 等级 | 回滚方案 |
| -- | -- | -- |
| 路径从 `/api/api/v1` 修正为 `/api/v1` 影响旧客户端 | 高 | 临时添加旧路径兼容 Controller 或通知客户端迁移 |
| OpenAPI 全局 security 标记公开接口 | 中 | 改用 `OpenApiCustomizer` 对公开路径移除 security |
| DevTools 影响生产打包认知 | 低 | 保持 `runtime` + `optional=true`，生产以 fat jar 验证 |
| Docker Compose watch 造成文件监听风暴 | 中 | 回退 A 方案：本机运行后端，Docker 只跑依赖 |
| CandidateSeed REST 契约未定 | 中 | 先不实现 Controller，保留 Service 内部能力 |
