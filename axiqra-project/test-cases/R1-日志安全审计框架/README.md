# R1 — 日志安全审计框架

> Round 1 开发成果测试归档目录

## 目录结构

```
R1-日志安全审计框架/
├── 01-审核报告/
│   └── R1-完成度审核报告.md     # 开发完成度审核（完整可用度 88%）
├── 02-单元测试/                   # 单元测试源码
│   └── （测试源码位于 axiqra-code 模块下）
└── 03-测试用例/
    ├── R1-单元测试用例.md       # 20 个单元测试用例
    └── R1-测试执行记录.md      # 测试执行记录（含结果待填）
```

## 交付物清单

| 交付物 | 路径 | 状态 |
|--------|------|------|
| logback-spring.xml | `axiqra-start/src/main/resources/` | ✅ |
| TraceIdFilter | `axiqra-api/.../filter/` | ✅ |
| GlobalExceptionHandler | `axiqra-api/.../handler/` | ✅ |
| ApiSignatureFilter | `axiqra-api/.../filter/` | ✅ |
| CachedBodyHttpServletRequest | `axiqra-api/.../filter/` | ✅ |
| AesEncryptUtil | `axiqra-common/.../util/` | ✅ |
| PasswordHashUtil | `axiqra-common/.../util/` | ✅ |
| DataMaskingUtil | `axiqra-common/.../util/` | ✅ |
| ErrorCode | `axiqra-common/.../exception/` | ✅ |
| BizException/SysException/ParamException | `axiqra-common/.../exception/` | ✅ |
| ApiResponse | `axiqra-common/.../response/` | ✅ |
| AuditPort | `axiqra-common/audit/` | ✅ |
| AuditAdapter | `axiqra-core/adapter/` | ✅ |
| application.yml 审计配置 | `axiqra-start/src/main/resources/` | ✅ |
| 审核报告 | `01-审核报告/R1-完成度审核报告.md` | ✅ |
| 单元测试用例文档 | `03-测试用例/R1-单元测试用例.md` | ✅ |
| 测试执行记录 | `03-测试用例/R1-测试执行记录.md` | ✅ |

## 执行命令

```bash
# 单元测试
cd axiqra-code
mvn test -pl axiqra-common,axiqra-api -am

# 集成测试
mvn test -pl axiqra-start -am -Dtest=InfraConnectionIT

# 覆盖率报告
mvn test jacoco:report
```
