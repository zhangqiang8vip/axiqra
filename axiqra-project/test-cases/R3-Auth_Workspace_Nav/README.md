# R3 — Auth + Workspace + Nav

> Round 3 开发成果测试归档目录

## 目录结构

```text
R3-Auth_Workspace_Nav/
├── 01-审核报告/
│   └── R3-完成度审核报告.md     # R3 完成度审核（代码完成 + 测试通过 + 验收归档）
└── 03-测试用例/
    ├── R3-测试执行记录.md      # 测试执行记录（含结果与边界说明）
    └── R3-覆盖率归档说明.md    # 覆盖率执行方法、路径与复核说明
```

## 交付物清单

| 交付物 | 路径 | 状态 |
|--------|------|------|
| AuthController（登录/注册/登出/当前用户/个人资料） | `axiqra-api/.../controller/AuthController.java` | ✅ |
| UserController（当前用户/公开用户） | `axiqra-api/.../controller/UserController.java` | ✅ |
| WorkspaceController（工作空间 CRUD + 成员管理） | `axiqra-api/.../controller/WorkspaceController.java` | ✅ |
| PolicyController（策略评估 / enforce / scope check） | `axiqra-api/.../controller/PolicyController.java` | ✅ |
| NavController + NavService | `axiqra-api` + `axiqra-core` | ✅ |
| WorkspaceRoleCheckInterceptor | `axiqra-api/.../interceptor/WorkspaceRoleCheckInterceptor.java` | ✅ |
| ScopeCheckInterceptor | `axiqra-api/.../interceptor/ScopeCheckInterceptor.java` | ✅ |
| RbacService / RbacAdapter | `axiqra-core` | ✅ |
| PolicyEngineService / PolicyEngineAdapter | `axiqra-core` | ✅ |
| WorkspaceServiceImpl | `axiqra-core/.../service/impl/WorkspaceServiceImpl.java` | ✅ |
| UserServiceImpl | `axiqra-core/.../service/impl/UserServiceImpl.java` | ✅ |
| 审核报告 | `01-审核报告/R3-完成度审核报告.md` | ✅ |
| 测试执行记录 | `03-测试用例/R3-测试执行记录.md` | ✅ |
| 覆盖率归档说明 | `03-测试用例/R3-覆盖率归档说明.md` | ✅ |

## 执行命令

```bash
cd D:\ai\axiqra\axiqra-project\axiqra-code
mvn test
```

## R3 范围说明

R3（Round 3）交付内容包括：

1. **Auth 模块**：`/auth/login`、`/auth/register`、`/auth/logout`、`/auth/me`、`/auth/profile`
2. **User 模块**：`/users/me`、`/users/{userId}`
3. **Workspace 模块**：personal/team/enterprise 空间 CRUD、成员管理、owner/admin 角色约束
4. **RBAC/ABAC 框架**：角色判定、Scope 校验、策略评估与 enforce
5. **累加式导航 API**：`/auth/nav`，返回五段导航，`governance/admin` 作为 S1 空列表预留

## 口径说明

- 当前实现的职责划分为：
  - `AuthController`：认证与个人资料接口（`login/register/logout/me/profile`）
  - `UserController`：用户信息查询接口（`/users/me`、`/users/{userId}`）
- 计划与归档口径已按现有代码结构统一，不再保留“认证接口归属 UserController”的旧表述。
