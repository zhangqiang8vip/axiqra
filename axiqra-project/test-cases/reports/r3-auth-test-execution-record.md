# R3 Auth 完善后全项目测试执行记录

## 1. 记录说明

本记录归档 R3 Auth 测试补全后的全项目测试执行结果（`codex/fix-module-ignores` 分支）。

## 2. 测试对象

- 模块：`axiqra-common`、`axiqra-core`、`axiqra-api`、`axiqra-start`、`axiqra-generator`（全项目）
- 测试范围：所有 `*Test.java`（单元测试）
- 执行目录：`D:\ai\axiqra\axiqra-project\axiqra-code`

## 3. 执行信息

- 执行时间：2026-06-07 16:04（UTC+8）
- 执行分支：`codex/fix-module-ignores`
- 执行命令：`mvn clean test`

## 4. 测试结果

### 4.1 汇总

| 模块 | 测试类 | Tests run | Failures | Errors | Skipped | 结果 |
|---|---|---|---|---|---|---|
| axiqra-common | AesEncryptUtilTest | 11 | 0 | 0 | 0 | PASSED |
| axiqra-common | DataMaskingUtilTest | 16 | 0 | 0 | 0 | PASSED |
| axiqra-common | PasswordHashUtilTest | 8 | 0 | 0 | 0 | PASSED |
| axiqra-core | WorkspaceServiceImplTest | 22 | 0 | 0 | 0 | PASSED |
| axiqra-core | RbacServiceImplTest | 9 | 0 | 0 | 0 | PASSED |
| axiqra-core | PolicyEngineServiceImplTest | 6 | 0 | 0 | 0 | PASSED |
| axiqra-api | TraceIdFilterTest | 5 | 0 | 0 | 0 | PASSED |
| axiqra-api | GlobalExceptionHandlerTest | 9 | 0 | 0 | 0 | PASSED |
| axiqra-api | WorkspaceControllerTest | 6 | 0 | 0 | 0 | PASSED |
| axiqra-api | PolicyControllerTest | 4 | 0 | 0 | 0 | PASSED |
| axiqra-start | ApplicationContextTest | 1 | 0 | 0 | 0 | PASSED |
| axiqra-generator | — | 0 | 0 | 0 | 0 | 无测试 |

**全项目汇总：Tests run: 97，Failures: 0，Errors: 0，Skipped: 0**

### 4.2 Build Summary

| 模块 | 结果 |
|---|---|
| Axiqra Code | SUCCESS |
| Axiqra Common | SUCCESS |
| Axiqra Core | SUCCESS |
| Axiqra API | SUCCESS |
| Axiqra Start | SUCCESS |
| Axiqra Generator | SUCCESS |

**最终结论：`BUILD SUCCESS`，总耗时 01:35 min**

## 5. 归档摘要

全项目单元测试 97 个全部通过，无失败无错误无跳过。

本次新增测试覆盖：
- **axiqra-core**：WorkspaceServiceImpl（22）、RbacServiceImpl（9）、PolicyEngineServiceImpl（6）
- **axiqra-api**：WorkspaceController（6）、PolicyController（4）
- 技术债修复：T-001（KeyVaultPort）、T-002（动态状态码）、T-004（workspaceId > 0 校验）、unchecked warning 修复

## 6. 原始结果来源

- `axiqra-project/axiqra-code/target/surefire-reports/TEST-*.xml`
- Terminal: `C:\Users\.cursor\projects\d-ai-axiqra/agent-tools/6880da24-c724-47c2-810a-b8d97d9b688d.txt`
