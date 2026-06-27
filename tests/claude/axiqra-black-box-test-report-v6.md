# axiqra 黑盒真实用户测试报告 V6 - Claude

**测试日期**: 2026-06-25  
**测试版本**: V6（设备授权流程测试）  
**测试人**: Claude (黑盒真实用户)  
**测试环境**: Windows 11, Node.js v22.22.1, 后端服务 http://localhost:8080/api

---

## 1. 测试结论摘要

| 项目         | 结论              |
| ---------- | --------------- |
| 设备授权流程是否可用 | ✅ 可用 |
| 授权码获取是否成功  | ✅ 成功 |
| 授权页面是否可访问  | ⚠️ 方式一可用，方式二不可用 |
| Token 获取是否成功 | ✅ 成功 |
| CLI 登录是否成功  | ✅ 成功 |
| 搜索功能是否可用   | ✅ 可用（无数据） |
| 配额查询是否可用   | ✅ 可用 |
| 会话管理是否可用   | ✅ 可用 |
| 诊断功能是否可用   | ✅ 可用 |
| 草稿功能是否可用   | ❌ 不可用（bug） |
| 轨迹提交是否可用   | ❌ 不可用（500错误） |
| Seed 创建是否可用  | ❌ 不可用（权限不足） |
| 最终结论       | **部分通过**（基础功能可用，核心功能需修复） |

---

## 2. 设备授权流程测试

### 2.1 发起授权请求

**操作**: `POST http://localhost:8080/api/auth/device/code`

**结果**:
```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "user_code": "E7E-Y5A",
    "device_code": "gC0u0pXfGTmzz7tK",
    "interval": 2,
    "expires_in": 600,
    "verification_url": "http://localhost:8080/api/static/device-verify.html?code=E7E-Y5A&device=gC0u0pXfGTmzz7tK"
  }
}
```

**评估**: ✅ 成功获取授权码

---

### 2.2 授权页面访问测试

| 方式 | URL | 是否可用 | 说明 |
|------|-----|---------|------|
| 方式一 | http://localhost:5173/auth/device | ✅ 可用 | 用户在此页面输入授权码成功 |
| 方式二 | http://localhost:8080/api/static/device-verify.html?code=E7E-Y5A&device=gC0u0pXfGTmzz7tK | ❌ 不可用 | 返回 JSON 错误，非 HTML 页面 |

**正确链接**: http://localhost:5173/auth/devicel?code=E7E-Y5A&device=gC0u0pXfGTmzz7tK

> ⚠️ **文档问题**: API 返回的 `verification_url` 指向不可用的页面，实际应使用 `http://localhost:5173/auth/devicel`

---

### 2.3 获取 Token

**操作**: `POST http://localhost:8080/api/auth/device/token?deviceCode=gC0u0pXfGTmzz7tK`

**结果**:
```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "access_token": "4dfaf978-acc4-4c95-8cae-d8da9b901c80",
    "token_type": "Bearer",
    "expires_in": 2592000,
    "user": {
      "nickname": "testuser",
      "id": 427318654049394688,
      "username": "testuser"
    }
  }
}
```

**评估**: ✅ 成功获取 Token（有效期 30 天）

---

### 2.4 CLI 登录验证

**操作**: 保存 Token 到配置文件后执行 `whoami`

**结果**:
```
当前用户

════════════════════════════════════════
  ID:      427318654049394700
  用户名:   testuser
  邮箱:    test@example.com
════════════════════════════════════════
```

**评估**: ✅ CLI 登录成功

---

## 3. 问题清单

| 编号 | 问题现象 | 影响程度 | 疑似类型 | 说明 |
| -- | ---- | ---- | ---- | -- |
| 1 | API 返回的 verification_url 不可用 | 一般 | 文档问题 | 返回 `/api/static/device-verify.html`，实际应为 `/auth/devicel` |
| 2 | 方式二（备用链接）无法访问 | 一般 | 服务问题 | `/api/static/device-verify.html` 返回 JSON 错误而非 HTML |

---

## 4. 正确的授权流程

### 步骤 1：发起授权请求
```bash
curl -s -X POST http://localhost:8080/api/auth/device/code -H "Content-Type: application/json"
```

### 步骤 2：获取授权码
从响应中获取 `user_code`（如 `E7E-Y5A`）

### 步骤 3：用户输入授权码
打开浏览器访问：`http://localhost:5173/auth/device`

### 步骤 4：获取 Token
```bash
curl -s -X POST "http://localhost:8080/api/auth/device/token?deviceCode=<device_code>" -H "Content-Type: application/json"
```

### 步骤 5：保存 Token 到配置文件
```json
{
  "token": "<access_token>",
  "user": {
    "id": <user_id>,
    "username": "<username>",
    "nickname": "<nickname>"
  }
}
```

配置文件位置：`C:\Users\Administrator\.axiqra\config.json`

---

## 5. 测试数据

| 数据类型 | 数据值 | 用途 |
|---------|--------|------|
| user_code | E7E-Y5A | 用户输入的授权码 |
| device_code | gC0u0pXfGTmzz7tK | 设备标识，用于获取 Token |
| access_token | 4dfaf978-acc4-4c95-8cae-d8da9b901c80 | API 访问令牌 |
| user_id | 427318654049394688 | 用户 ID |
| username | testuser | 用户名 |

---

## 6. 最终结论

```text
V6 设备授权流程及接入后使用测试结论：部分通过

是否建议进入交付验收：部分建议（基础功能可交付，核心功能需修复）

是否需要继续修复：是

原因：
1. ✅ 设备授权流程完整可用：发起授权 → 获取授权码 → 用户确认 → 获取 Token → CLI 登录
2. ✅ 基础功能正常：搜索、配额、会话、诊断
3. ❌ 草稿功能不可用：draft trace/list 命令报错，API 返回 500
4. ❌ 轨迹提交不可用：API 返回 500
5. ❌ Seed 创建权限不足：需要 seed:write scope
6. ⚠️ 认证格式问题：不能使用 Bearer 前缀

建议修复：
1. 修复草稿功能（draft trace/list 命令及 API）
2. 修复轨迹提交 API（500 错误）
3. 配置 Seed 创建权限（seed:write scope）
4. 修正 API 返回的 verification_url
5. 更新文档说明正确的认证格式（不要加 Bearer）
```

---

## 7. 接入后使用测试

### 7.1 CLI 功能测试

| 功能 | 命令 | 结果 | 说明 |
|------|------|------|------|
| 搜索方案 | `search "Spring Boot"` | ✅ 正常 | 返回"未找到匹配的方案"（无数据） |
| 搜索方案 | `search "Docker"` | ✅ 正常 | 返回"未找到匹配的方案"（无数据） |
| 搜索方案 | `search "Redis"` | ✅ 正常 | 返回"未找到匹配的方案"（无数据） |
| 配额查询 | `quota` | ✅ 正常 | 剩余: 2000 |
| 会话列表 | `sessions list` | ✅ 正常 | 暂无会话 |
| 诊断检查 | `doctor` | ✅ 正常 | 6/6 通过 |
| 草稿创建 | `draft trace "测试"` | ❌ 失败 | "Cannot read properties of undefined" |
| 草稿列表 | `draft list` | ❌ 失败 | "this.getConfig is not a function" |
| Seed 创建 | `seed create "测试"` | ❌ 失败 | 权限不足，需要 scope: seed:write |
| 反馈提交 | `feedback INV-test worked` | ❌ 失败 | Internal server error |

### 7.2 API 直接调用测试

| API 端点 | 方法 | 结果 | 说明 |
|---------|------|------|------|
| `/api/search/before-act` | POST | ✅ 正常 | 返回空结果（无数据） |
| `/api/connect/quota` | GET | ✅ 正常 | 剩余: 2000 |
| `/api/connect/sessions` | GET | ✅ 正常 | 返回空数组 |
| `/api/v1/traces/draft` | POST | ❌ 失败 | Internal server error (500) |
| `/api/traces` | POST | ❌ 失败 | Internal server error (500) |
| `/api/seeds` | POST | ❌ 失败 | 权限不足 (20004) |

### 7.3 认证方式说明

**重要发现**: CLI 和 API 使用不同的认证格式

| 方式 | Header 格式 | 示例 |
|------|------------|------|
| CLI | `Authorization: <token>` | `Authorization: 7b9bfa8d-...` |
| API | `Authorization: <token>` | `Authorization: 7b9bfa8d-...`（不要加 Bearer） |

> ⚠️ **注意**: 使用 `Authorization: Bearer <token>` 会导致认证失败

---

## 8. 问题汇总

| 编号 | 问题现象 | 影响程度 | 疑似类型 | 状态 |
| -- | ---- | ---- | ---- | -- |
| 1 | API 返回的 verification_url 不可用 | 一般 | 文档问题 | 未修复 |
| 2 | 方式二（备用链接）无法访问 | 一般 | 服务问题 | 未修复 |
| 3 | draft trace 命令报错 | 严重 | 服务问题 | 未修复 |
| 4 | draft list 命令报错 | 严重 | 服务问题 | 未修复 |
| 5 | Seed 创建权限不足 | 严重 | 配置问题 | 未修复 |
| 6 | 轨迹提交 API 返回 500 | 严重 | 服务问题 | 未修复 |
| 7 | 草稿 API 返回 500 | 严重 | 服务问题 | 未修复 |

---

## 附录：测试环境信息

| 项目 | 值 |
|------|---|
| 操作系统 | Windows 11 Pro for Workstations |
| Node.js | v22.22.1 |
| CLI 版本 | 1.0.0 |
| 后端地址 | http://localhost:8080/api |
| 前端地址 | http://localhost:5173 |
| 配置文件 | C:\Users\Administrator\.axiqra\config.json |

---

**报告生成时间**: 2026-06-25 21:55  
**测试耗时**: 约 5 分钟
