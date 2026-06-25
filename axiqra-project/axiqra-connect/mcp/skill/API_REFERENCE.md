# Axiqra API Reference

本文档描述 Axiqra Skill 可调用的 REST API 接口。

**基础 URL**：
- 生产环境：`https://api.axiqra.com/api`
- 本地开发：`http://localhost:8080/api`

**认证方式**：Bearer Token（通过设备授权获取）

**通用请求头**：
```http
Content-Type: application/json
Authorization: Bearer {access_token}
Accept: application/json
```

## 目录

- [认证](#认证)
- [搜索](#搜索)
- [轨迹](#轨迹)
- [方案](#方案)
- [反馈](#反馈)
- [用户](#用户)
- [工作空间](#工作空间)

---

## 认证

### 获取设备授权码

```
POST /auth/device/code
```

**请求体**：
```json
{
  "client_id": "axiqra-mcp-agent",
  "device_code": "axiqra-xxx",
  "platform": "cursor"
}
```

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "device_code": "xxx",
    "user_code": "ABC-123",
    "verification_url": "http://localhost:5173/auth/device?code=ABC-123",
    "interval": 5,
    "expires_in": 600
  }
}
```

### 轮询获取访问令牌

```
POST /auth/device/token
```

**请求参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| device_code | string | 设备码 |

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "access_token": "xxx",
    "token_type": "Bearer",
    "expires_in": 2592000,
    "user": {
      "id": 1,
      "username": "testuser",
      "nickname": "测试用户"
    }
  }
}
```

### 确认授权

```
POST /auth/device/confirm
```

**请求体**：
```json
{
  "user_code": "ABC-123",
  "device_code": "xxx"
}
```

---

## 搜索

### 搜索前执行（权限预过滤 + 多路召回）

```
POST /search/before-act
```

**请求体**：
```json
{
  "query": "用户登录功能实现",
  "tech_stack": "Spring Boot + MyBatis",
  "environment": "production",
  "risk_hint": "staging",
  "max_results": 5,
  "workspace_id": "ws-xxx"
}
```

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "results": [
      {
        "solution_id": "sol-xxx",
        "title": "Spring Boot 用户认证方案",
        "fit_score": 0.95,
        "verification_level": "L3",
        "risk_level": "R2",
        "required_confirmation": false
      }
    ],
    "risk_hints": ["生产环境操作需谨慎"],
    "total": 1
  }
}
```

### 公开搜索（匿名）

```
POST /search/public
```

**请求体**：
```json
{
  "query": "用户登录功能",
  "max_results": 10
}
```

---

## 轨迹

### 创建轨迹草稿

```
POST /traces
```

**请求体**：
```json
{
  "session_id": "session-xxx",
  "task_goal": "实现用户登录功能",
  "environment": {
    "tech_stack": "Spring Boot",
    "version": "3.0",
    "os": "Linux"
  },
  "forward_path": [
    {"step": 1, "action": "创建 UserController", "status": "success"},
    {"step": 2, "action": "添加登录接口", "status": "success"}
  ],
  "reverse_path": [],
  "decision_path": [],
  "evidence_refs": ["file:///path/to/UserController.java"],
  "outcome": "success"
}
```

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "status": "draft",
    "task_goal": "实现用户登录功能",
    "created_at": "2026-06-25T10:00:00Z"
  }
}
```

### 确认轨迹

```
POST /traces/{traceId}/confirm
```

**请求体**：
```json
{
  "outcome": "success",
  "notes": "登录功能实现完成"
}
```

### 提交轨迹

```
POST /traces/{traceId}/submit
```

### 获取轨迹列表

```
GET /traces
```

### 获取轨迹详情

```
GET /traces/{traceId}
```

### 提交证据路径

```
POST /traces/{traceId}/evidence
```

**请求体**：
```json
{
  "evidenceRefs": [
    "file:///path/to/test.java",
    "http://ci.example.com/build/123"
  ]
}
```

---

## 方案

### 从项目案例生成方案

```
POST /solutions/from-project-case
```

**请求体**：
```json
{
  "project_case_id": "pc-xxx",
  "title": "Spring Boot 用户认证方案",
  "description": "详细的实现方案描述"
}
```

### 获取方案详情

```
GET /solutions/{solutionId}
```

### 列出公开方案

```
GET /solutions/public
```

**查询参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| query | string | 搜索关键词 |
| domain | string | 领域 |
| tech_stack | string | 技术栈 |
| min_verification_level | int | 最低验证等级 |
| limit | int | 返回数量 |

### 提交方案审核

```
POST /solutions/{solutionId}/submit-for-review
```

### 归档方案

```
POST /solutions/{solutionId}/archive
```

---

## 反馈

### 提交反馈

```
POST /v1/feedbacks
```

**请求体**：
```json
{
  "target_type": "solution",
  "target_id": "sol-xxx",
  "feedback_type": "worked",
  "evidence_refs": ["file:///path/to/test.java"],
  "notes": "方案有效，成功解决了问题"
}
```

**反馈类型**：
- `worked`：方案有效
- `partial`：部分有效
- `failed`：方案失败
- `not_applicable`：不适用

### 获取反馈列表

```
GET /v1/feedbacks?target_type=solution&target_id=sol-xxx
```

### 获取方案反馈统计

```
GET /v1/feedbacks/solutions/{solutionId}/stats
```

---

## 用户

### 获取当前用户

```
GET /auth/me
```

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "userId": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "email": "test@example.com",
    "token": "xxx"
  }
}
```

### 更新个人资料

```
PUT /auth/profile
```

**请求体**：
```json
{
  "nickname": "新昵称",
  "email": "new@example.com",
  "avatar": "https://example.com/avatar.png"
}
```

---

## 工作空间

### 创建工作空间

```
POST /workspaces
```

**请求体**：
```json
{
  "name": "我的工作空间",
  "type": "team",
  "visibility": "workspace"
}
```

### 获取工作空间详情

```
GET /workspaces/{workspaceId}
```

### 列出工作空间成员

```
GET /workspaces/{workspaceId}/members
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 422 | 验证失败 |
| 429 | 请求过于频繁 |
| 500 | 服务器错误 |

---

## 分页

列表接口支持分页：

```
GET /endpoint?page=1&page_size=20
```

**响应**：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "items": [],
    "total": 100,
    "page": 1,
    "page_size": 20,
    "total_pages": 5
  }
}
```
