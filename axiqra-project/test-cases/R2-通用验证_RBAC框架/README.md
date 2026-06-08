# R2 — 通用验证 + RBAC 框架

> Round 2 开发成果测试归档目录

## 目录结构

```text
R2-通用验证_RBAC框架/
├── 01-审核报告/
│   └── R2-完成度审核报告.md     # R2 完成度审核（关联 review fixes fc88005a）
├── 02-单元测试/                   # 单元测试源码
│   └── （测试源码位于 axiqra-code 模块下）
└── 03-测试用例/
    ├── R2-单元测试用例.md       # 本文档：R2 全部单元测试用例说明
    └── R2-测试执行记录.md      # 测试执行记录（含结果）
```

## 交付物清单

| 交付物 | 路径 | 状态 |
|--------|------|------|
| NoHtmlValidator | `axiqra-common/.../validation/NoHtmlValidator.java` | ✅ |
| NoHtmlValidatorTest | `axiqra-common/.../validation/NoHtmlValidatorTest.java` | ✅ |
| UserEntity / UserService / UserServiceImpl | `axiqra-common` + `axiqra-core` | ✅ |
| WorkspaceEntity / WorkspaceService / WorkspaceServiceImpl | `axiqra-common` + `axiqra-core` | ✅ |
| MembershipMapper（软删除修复） | `axiqra-core/.../mapper/MembershipMapper.java` | ✅ |
| RbacService / RbacAdapter | `axiqra-core` | ✅ |
| PolicyEngineAdapter | `axiqra-core/.../adapter/PolicyEngineAdapter.java` | ✅ |
| AuthController（登录/注册/登出） | `axiqra-api/.../controller/AuthController.java` | ✅ |
| UserController（用户信息） | `axiqra-api/.../controller/UserController.java` | ✅ |
| WorkspaceController（工作空间 CRUD） | `axiqra-api/.../controller/WorkspaceController.java` | ✅ |
| PolicyController（策略评估） | `axiqra-api/.../controller/PolicyController.java` | ✅ |
| NavController + NavService | `axiqra-api` + `axiqra-core` | ✅ |
| ApiResponse（requestId 字段） | `axiqra-common/.../response/ApiResponse.java` | ✅ |
| TraceIdResponseAdvice（requestId 注入） | `axiqra-api/.../advice/TraceIdResponseAdvice.java` | ✅ |
| ApiSignatureFilter（登录白名单修复） | `axiqra-api/.../filter/ApiSignatureFilter.java` | ✅ |
| SecurityConfig（鉴权链路修复） | `axiqra-api/.../config/SecurityConfig.java` | ✅ |
| 审核报告 | `01-审核报告/R2-完成度审核报告.md` | ✅ |
| 单元测试用例文档 | `03-测试用例/R2-单元测试用例.md` | ✅ |
| 测试执行记录 | `03-测试用例/R2-测试执行记录.md` | ✅ |

## 执行命令

```bash
# 单元测试（全模块）
cd axiqra-code
mvn test -q

# 指定模块单元测试
mvn test -pl axiqra-common -am
mvn test -pl axiqra-core -am
mvn test -pl axiqra-api -am

# 集成测试
mvn test -pl axiqra-start -am -Dtest=InfraConnectionIT -DfailIfNoTests=false

# 覆盖率报告
mvn test jacoco:report
```

## R2 范围说明

R2（Round 2）交付内容包括：

1. **通用验证框架**：`NoHtmlValidator`（防 XSS）+ DTO 参数校验
2. **认证模块**：登录（/auth/login）、注册（/auth/register）、登出（/auth/logout）、个人资料更新
3. **用户模块**：当前用户信息（/users/me）、指定用户公开信息（/users/{userId}）
4. **工作空间模块**：CRUD、成员管理（添加/移除/角色变更）
5. **RBAC/ABAC 框架**：角色校验、Scope 校验、策略评估
6. **导航模块**：累加式菜单
7. **关键修复**（fc88005a）：
   - 登录/注册被签名过滤器拦截（P0）
   - Spring Security 与 Sa-Token 鉴权链路冲突（P0）
   - 接口路径重复 /api/api/...（P1）
   - request_id 响应契约（P1）
   - membership 软删除语义（P1）
   - N+1 查询（P2）
   - email 隐私泄露（P2）
   - logout 吞异常（P2）
   - 未使用字段（P3）
