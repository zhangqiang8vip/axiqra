# R6 — Trace + Project Case + Public Case

## 目录结构

```text
R6-Trace_ProjectCase_PublicCase/
├── 01-审核报告/
│   └── R6-完成度审核报告.md
└── 03-测试用例/
    └── R6-测试执行记录.md
```

## 交付物清单

| 交付物 | 路径 | 状态 |
|--------|------|------|
| Trace API / Service | `axiqra-api` + `axiqra-core` | ✅ |
| Project Case API / Service | `axiqra-api` + `axiqra-core` | ✅ |
| Public Case API / Service | `axiqra-api` + `axiqra-core` | ✅ |
| 审核报告 | `01-审核报告/R6-完成度审核报告.md` | ✅ |
| 测试执行记录 | `03-测试用例/R6-测试执行记录.md` | ✅ |

## 执行命令

```bash
cd ../../axiqra-code
mvn -pl axiqra-api,axiqra-core,axiqra-common -am test -Dtest=TraceServiceImplTest,TraceControllerTest,ProjectCaseServiceImplTest,ProjectCaseControllerTest,ProjectCaseControllerMockMvcTest,PublicCaseServiceImplTest,PublicCaseControllerTest,PublicCaseControllerMockMvcTest -Dsurefire.failIfNoSpecifiedTests=false
```
