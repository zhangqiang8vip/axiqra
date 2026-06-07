# R1/R2 Bug 复盘 — R3 编码 Checklist

> 本文档记录 R1（日志+安全+审计）和 R2（Common 层）开发阶段遇到的所有 bug、根因和教训，作为 R3 开发的记忆文档。
> **原则：不重蹈覆辙，一次写对。**

---

## 一、已修复的历史 Bug

| # | 问题 | 根因 | R3 写法要点 |
|---|------|------|------------|
| **B-001** | `DataMaskingUtil` JSON 正则把双引号也匹配进去了，导致脱敏后多了引号如 `"SECRET_REDACTED"` | 正则 `["']?` 匹配了值内的引号，边界判断错误 | JSON/正则脱敏后验证边界字符，用真实输入测试边界 |
| **B-002** | `TraceIdFilterTest` 中 MDC 在 `finally` 后才清理，`assertNull` 断言时 MDC 还没清 | 测试异步/拦截器顺序问题，`finally` 在 mock chain 完成后才执行 | MDC 测试要确保 filter chain 真正走完后再断言；用 `CountDownLatch` 或 `verify` 等待完成 |
| **B-003** | `ConstraintViolation` 匿名内部类在 JDK 17 下编译失败 | JDK 17 对内部类实现接口更严格 | 用 `Mockito.mock(ConstraintViolation.class)` 而非匿名内部类实现接口 |

---

## 二、基础设施坑（R3 写代码前要确认）

| # | 坑 | 解决方案 |
|---|------|----------|
| **I-001** | PostgreSQL Audit 端口 5432 与 Docker Desktop 冲突 | `application-dev.yml` 审计库端口配置为 `5433` |
| **I-002** | PostgreSQL bootstrap superuser 无法自降权限，RLS 绕过 | `init.sql` 里用独立 non-superuser `axiqra_audit_app` 作为应用连接角色 |
| **I-003** | 审计失败被吞掉不阻断业务，长期失联会静默丢数据 | S2 前接受现状，代码注释清楚；R3 阶段补告警 |
| **I-004** | 单元测试里 BCrypt 比对用 DUMMY_HASH 防止时序攻击 | 登录验证失败统一返回 DUMMY_BCRYPT_HASH 再比对，不提前 return |

---

## 三、TODO 项（S2 才做，R3 预留接口）

| # | TODO | 位置 | R3 怎么做 |
|---|------|------|----------|
| **T-001** | 密钥用环境变量是临时方案，安全性不足 | `ApiSignatureFilter.java:200` | 接口设计要考虑可注入 `KeyVaultPort`，不要硬编码 secret lookup 逻辑 |
| **T-002** | `BizException` 固定返回 409，不所有场景都语义正确 | `GlobalExceptionHandler.java:49` | 留 `statusCode` 字段或方法，当前先用 409 |

---

## 四、代码规范红线（R3 一次写好）

### 4.1 枚举 `of()` 方法
- **规则**：常量放左边 `s.code.equals(code)`，防 NPE
- **状态**：R2 做得对，R3 继续保持，所有枚举照此执行

### 4.2 Entity 的 equals/hashCode
- **规则**：`@EqualsAndHashCode(exclude = {"gmtCreate", "gmtModified"})`
- **状态**：`BaseEntity` 已正确实现，R3 新增 DO 照此继承

### 4.3 DTO/VO 序列化
- **规则**：Jackson 反序列化前确认有 `@NoArgsConstructor`；`@Builder` 类加 `@AllArgsConstructor` + `@NoArgsConstructor`
- **风险**：`LoginResponse` 目前 response-only 用法安全；后续如需反序列化注意 `@JsonPOJOBuilder`

### 4.4 null 安全
- 所有 `e.getMessage()` 前先判 null：`e.getMessage() != null ? e.getMessage().toLowerCase() : "unknown"`
- MDC 清理放 `finally`，确保 filter chain 完成后才断言

### 4.5 测试规范
- JDK 17+ 下接口 mock 用 `Mockito.mock()` 而非匿名内部类
- 测试用真实值边界验证，不只测 happy path
- 集成测试用 `@SpringBootTest` + `@ActiveProfiles("test")`

### 4.6 Port 接口模式
- 所有外部依赖（Cache、Quota、Audit、KeyVault 等）走 Port 接口
- 实现类加 `@Component` / `@Service`，用 `@RequiredArgsConstructor` 注入
- 不在 Service 里直接 `new` 依赖

---

## 五、R3 模块结构约束

- `axiqra-common/`：DO、DTO、VO、枚举、统一异常、统一响应（`ApiResponse`/`PageRequest`/`PageResponse`）
- `axiqra-core/`：业务逻辑，`@Service` + Port 接口，事务边界
- `axiqra-api/`：Controller 层，参数校验（`@Valid`），异常收敛到 `GlobalExceptionHandler`
- 每个 Controller 方法必须有 Knife4j `@ApiOperation` 注解
- 所有 HTTP 响应统一走 `ApiResponse<T>` 包装

---

*文档版本：v1.0 | 生成时间：2026-06-07 | 适用：R3 Auth + Workspace 阶段*
