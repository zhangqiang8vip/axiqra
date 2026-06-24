# TASK 3 - Framework First / Configuration First Review

目标：检查当前代码中是否存在本可由成熟框架或配置解决、却手写了大量底层逻辑的地方，并给出改造建议。

## 一、框架优先审查表

| 位置 | 当前做法 | 推荐框架 / 配置 | 官方依据 | 是否建议改 | 原因 | 风险 |
| -- | -- | -- | -- | -- | -- | -- |
| API 文档 | Knife4j 4.5.0 + OpenAPI 注解 | `@SecurityScheme` / `@OpenAPIDefinition` | Knife4j 官方文档；OpenAPI Specification | 已改 | 新增 `OpenApiConfig` 声明 Sa-Token Bearer | 低 |
| API 路径 | 部分 Controller 使用 `/api/v1/...` | 只在 context-path 放 `/api` | Spring MVC mapping 规则 | 已改 | 5 个 Controller 改为 `/v1/...`，避免 `/api/api/v1` | 中，客户端路径需同步 |
| CORS | `SecurityConfig` 中 `CorsConfigurationSource` Bean | Spring Security CORS 配置 | Spring Boot/Spring Security CORS 文档 | 已符合 | 由配置属性驱动，未手写底层跨域处理 | 低 |
| 登录认证 | Sa-Token + SaInterceptor | Sa-Token 官方方案 | Sa-Token 官方文档 | 暂不改 | 已使用成熟认证框架 | 低 |
| Scope 校验 | `@RequireScope` + Interceptor | Sa-Token 权限模型或自定义注解 | Sa-Token 权限认证文档 | 暂不改 | Sa-Token 无 Axiqra scope 语义，项目注解合理 | 低 |
| Workspace 角色 | `@RequireWorkspaceRole` + Interceptor | 项目注解 + RBAC 服务 | Spring MVC HandlerInterceptor | 暂不改 | 领域权限需要业务服务判断 | 中 |
| 请求签名 | `ApiSignatureFilter` HMAC + nonce | 现成 HMAC 实现 + Servlet Filter | Spring Security crypto 无等价防重放中间件 | 暂不改 | 防重放是领域安全需求，手写过滤器合理 | 中 |
| 请求体缓存 | `CachedBodyHttpServletRequest` | Servlet request wrapper | Servlet API | 不建议改 | 签名校验需要重复读取 body，框架无简单配置替代 | 低 |
| 参数校验 | Jakarta Bean Validation | `@Valid` / `@Validated` | Spring Validation 文档 | 已符合 | DTO 已使用成熟校验注解 | 低 |
| 统一错误 | `@RestControllerAdvice` | Spring MVC exception handling | Spring Boot MVC 文档 | 已符合 | 使用框架异常处理 | 低 |
| 数据访问 | MyBatis-Flex Mapper | MyBatis-Flex starter | MyBatis-Flex 官方文档 | 已符合 | 项目选择 MyBatis-Flex，不强推 JPA | 低 |
| 分页 | 自定义 `PageRequest` / `PageResponse` | Spring Data Pageable | Spring Data 文档 | 暂不改 | 未使用 Spring Data，迁移收益不高 | 低 |
| 审计字段 | Service/Mapper 手动处理 | JPA Auditing | Spring Data JPA Auditing | 不建议改 | 当前不是 JPA 技术栈 | 低 |
| 数据迁移 | 审计库 Flyway + 业务库 SQL | Flyway / Liquibase | Spring Boot Flyway 文档 | 建议评估 | 审计库已 Flyway，业务库仍外部 SQL | 中 |
| 健康检查 | 自定义 HealthController + Actuator | Actuator HealthIndicator | Spring Boot Actuator 文档 | 建议改 | 深度检查可接入 Actuator details | 中 |
| 前端 | 当前无 Vue/Vite app | Vue 3 + Vite + Pinia + Vue Router | Vite/Vue 官方文档 | 规划项 | 不能在无前端项目上强行改 HMR | - |

## 二、必须改

| 编号 | 问题 | 状态 | 动作 |
| -- | -- | -- | -- |
| MUST-001 | `/api/v1` Controller 与 context-path 叠加 | 已完成 | `Invocation`、`Feedback`、`Contribution`、`Review`、`ToolModel` 改为 `/v1/...` |
| MUST-002 | OpenAPI 未声明认证方案 | 已完成 | 新增 `OpenApiConfig` Bearer scheme |
| MUST-003 | Spring Boot DevTools 未落地 | 已完成 | `axiqra-start/pom.xml` 新增 `spring-boot-devtools` |
| MUST-004 | 运行时 Swagger 未验证 | 未完成 | 依赖服务启动后执行 TASK-DEV-001 |

## 三、建议改

| 编号 | 问题 | 建议 | 优先级 |
| -- | -- | -- | -- |
| SUGGEST-001 | `HealthController` 与 Actuator health 能力重叠 | 把 DB/Redis/Rabbit/MinIO 深度检测做成 `HealthIndicator` | S2 |
| SUGGEST-002 | 业务库迁移仍依赖外部 SQL | 评估 Flyway 管理 CockroachDB 迁移 | S2 |
| SUGGEST-003 | Auth/User 当前用户接口重叠 | 确认保留 `/auth/me` 还是 `/users/me` | S1 |
| SUGGEST-004 | `POST /api/invocations/{id}/feedback` 草案缺失 | 决定新增别名还是更新草案 | S1 |

## 四、不建议改

| 编号 | 场景 | 原因 |
| -- | -- | -- |
| NO-CHANGE-001 | `ApiSignatureFilter` | HMAC nonce 防重放没有 Spring Security 开箱中间件，Filter 层合理 |
| NO-CHANGE-002 | `CachedBodyHttpServletRequest` | 读取 body 做签名验证需要 wrapper，框架配置无法直接替代 |
| NO-CHANGE-003 | MyBatis-Flex 改 Spring Data JPA | 技术路线已选 MyBatis-Flex，强迁移会引入大范围风险 |
| NO-CHANGE-004 | 自定义 `PageResponse` | API 响应契约已稳定，非 Spring Data 项目不必引入 Pageable |

## 五、需要我决策的项

| 问题 | 方案 A：框架 | 方案 B：手写 / 保持 | 推荐 | 需要我决定什么 |
| -- | -- | -- | -- | -- |
| Candidate Seed Controller 缺失 | 新增标准 Spring MVC Controller | 保持 Service 内部使用 | A | 是否在 S1 暴露 `POST /api/candidate-seeds` |
| Feedback 路由差异 | 新增 `POST /api/v1/invocations/{id}/feedback` 别名 | 保持 `/api/v1/feedbacks` | B | API 文档草案是否接受资源式 feedback 设计 |
| ToolModel 排行榜公开性 | 放到 permitAll / public endpoint | 保持需登录 | 待确认 | 排行榜是否属于公开数据 |
| 业务库迁移 | Flyway 管理 CockroachDB | 保持外部 SQL | 待确认 | 是否接受引入业务库迁移治理 |
