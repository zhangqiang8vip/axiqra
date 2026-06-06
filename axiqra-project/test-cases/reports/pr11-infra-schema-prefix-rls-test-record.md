# PR#11 Infrastructure Connection Test Record

## 1. 记录说明

本记录归档 PR #11 "refactor: unify table prefix and fix review risk_level default" 的基础设施集成验证结果。

## 2. 测试对象

- 模块：`axiqra-start`
- 测试类：`com.axiqra.start.InfraConnectionIT`（`*IT.java` 命名，符合规范）
- 相关文件：
  - `axiqra-start/src/test/java/com/axiqra/start/InfraConnectionIT.java`
  - `axiqra-start/src/main/java/com/axiqra/config/DataSourceConfig.java`
  - `axiqra-infra/docker-compose.yml`
  - `axiqra-infra/docker/postgres-audit/init.sql`
  - `axiqra-infra/docker/cockroachdb/init.sql`
  - `axiqra-code/axiqra-start/src/main/resources/application-dev.yml`
  - `axiqra-code/axiqra-start/src/test/resources/application-test.yml`
  - `axiqra-api/src/main/java/com/axiqra/api/config/HealthProbeProperties.java`

## 3. 测试目标

- 验证 CockroachDB（localhost:26257）连接可达
- 验证 CockroachDB 19 张表全部使用 `axiqra_` 前缀，旧 `axq_` 表已清除
- 验证 `axiqra_review.risk_level` 有 `DEFAULT 'R0'` 且 `NOT NULL`
- 验证插入 `axiqra_review` 时不指定 `risk_level` 时自动填充为 `R0`
- 验证 PostgreSQL Audit（localhost:5433）连接可达
- 验证 PostgreSQL Audit 8 张审计表全部使用 `axiqra_` 前缀
- 验证 PostgreSQL Audit 全 8 张表启用 `FORCE ROW LEVEL SECURITY`
- 验证 PostgreSQL Audit 全 8 张表各有 4 条 RLS 策略（SELECT/INSERT/UPDATE/DELETE）
- 验证 PostgreSQL Audit 的 append-only 特性：INSERT 成功，UPDATE/DELETE 被 RLS 策略拦截

## 4. 执行信息

- 执行时间：2026-06-06 18:59（UTC+8）
- 执行目录：`D:\ai\axiqra\axiqra-project\axiqra-code`
- 环境：本地 Windows + Docker Desktop
- Java 版本：OpenJDK 17.0.19（Eclipse Adoptium）
- 前置条件：`docker compose up -d`（CockroachDB + PostgreSQL Audit + Redis + RabbitMQ + MinIO 全部 healthy）

执行命令：

```bash
mvn clean test -pl axiqra-start -am \
  -Dtest=InfraConnectionIT \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false
```

## 5. 测试结果

| 测试用例 | 结果 | 说明 |
| --- | --- | --- |
| `primaryDataSourceReachable` | PASS | CockroachDB 响应 SELECT 1 |
| `cockroachDbTablesExist` | PASS | 19 张表均为 `axiqra_` 前缀 |
| `reviewRiskLevelHasDefault` | PASS | `risk_level DEFAULT 'R0' NOT NULL` |
| `insertReviewWithoutRiskLevel` | PASS | 插入时不指定 `risk_level` 自动填充为 `R0` |
| `noLegacyTablePrefixes` | PASS | 无 `axq_` 前缀旧表残留 |
| `auditDataSourceReachable` | PASS | PostgreSQL Audit 响应 SELECT 1 |
| `auditTablesExist` | PASS | 8 张审计表均为 `axiqra_` 前缀 |
| `auditTablesRlsEnabled` | PASS | 全 8 表 FORCE RLS 启用 |
| `auditTablesHaveFourPolicies` | PASS | 全 32 条策略（4/表） |
| `auditAppendOnly` | PASS | INSERT 成功，UPDATE/DELETE 被 RLS 策略正确拦截 |

- Tests run：10
- Failures：0
- Errors：0
- Skipped：0
- Time elapsed：5.485 s
- 最终结论：**PASSED**

## 6. 归档摘要

本次验证覆盖了 PR #11 的核心变更：表前缀从 `axq_` 统一为 `axiqra_`、`axiqra_review.risk_level` 默认值修复、以及 PostgreSQL Audit 的 append-only RLS 机制。所有 10 个测试用例均通过，覆盖了数据库连接、表结构、默认值、RLS 策略四个维度的验证。

关键发现：
- `axiqra_audit`（POSTGRES_USER）作为 bootstrap superuser 无法自行降权，init.sql 中的 `ALTER ROLE` 无效。因此通过创建独立的非超级用户 `axiqra_audit_app` 作为应用连接角色来规避 RLS 绕过问题。
- PostgreSQL Audit 端口从 5432 改为 5433，避免与 Docker Desktop backend 的端口冲突。
- Docker Desktop 环境下本地 127.0.0.1:5432 被占用，Java 测试使用纯 JDBC 直连（`DriverManager`）绕过 Spring 上下文，避免了复杂的数据源初始化链问题。

## 7. 原始结果来源

- `axiqra-start/target/surefire-reports/com.axiqra.start.InfraConnectionIT.txt`
- `axiqra-start/target/surefire-reports/TEST-com.axiqra.start.InfraConnectionIT.xml`

## 8. 后续动作

- [x] 所有测试通过，无需补充修复
- [x] 测试已归档到本报告
- [x] 已更新 `module-test-inventory.md`
