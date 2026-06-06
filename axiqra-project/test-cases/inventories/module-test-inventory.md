# 模块测试资产清单

## 1. 清单说明
本清单用于维护各模块当前已具备的测试资产，帮助团队快速判断覆盖范围、缺口与后续补充方向。

## 2. 维护规则
- 每新增测试类或测试场景，应同步更新本清单；
- 按模块维度维护，保持路径可追溯；
- “当前状态”用于标记是否已执行、是否已归档、是否需要补充。

## 3. 当前资产清单

| 模块 | 测试类/场景 | 文件路径 | 覆盖重点 | 当前状态 | 归档报告 |
| --- | --- | --- | --- | --- | --- |
| axiqra-api | SecurityConfigTest | `axiqra-code/axiqra-api/src/test/java/com/axiqra/api/config/SecurityConfigTest.java` | 健康检查白名单、受保护接口认证、错误凭证拒绝 | 已执行，已归档 | `reports/security-config-test-report.md` |
| axiqra-start | InfraConnectionIT | `axiqra-start/src/test/java/com/axiqra/start/InfraConnectionIT.java` | CockroachDB/PG Audit 连接、表前缀、risk_level 默认值、RLS append-only 策略 | 已执行，已归档 | `reports/pr11-infra-schema-prefix-rls-test-record.md` |

## 4. 待补充建议
- [x] `InfraConnectionIT` 已补充正式执行记录并归档
- [x] `ApplicationContextTest` 已更新为 `InfraConnectionIT`（替换为有意义的集成测试）
- 后续若新增 `*IT.java` 或 `*E2ETest.java`，应按测试类型补充分组说明；
- 后续若新增 `*IT.java` 或 `*E2ETest.java`，应按测试类型补充分组说明；
- 若某模块已有多组测试，可拆分为更细粒度的模块清单文件。
