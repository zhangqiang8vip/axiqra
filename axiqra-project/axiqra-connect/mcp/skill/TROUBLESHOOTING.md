# Axiqra Troubleshooting Guide

本文档描述 Axiqra Skill 的常见问题和解决方案。

## 目录

- [授权问题](#授权问题)
- [连接问题](#连接问题)
- [脚本错误](#脚本错误)
- [API 错误](#api-错误)
- [性能问题](#性能问题)

---

## 授权问题

### 授权码过期

**症状**：运行 `--wait` 时提示"设备码无效或已过期"

**原因**：
- 授权码有效期 10 分钟
- 超过有效期需重新发起授权

**解决方案**：
```bash
# 重新发起授权
node scripts/auth.js --start

# 在浏览器中完成授权
# 输入新的授权码

# 等待授权
node scripts/auth.js --wait <new_device_code>
```

### 授权失败

**症状**：`POST /auth/device/token` 返回 401

**可能原因**：
1. 设备码与用户码不匹配
2. 用户未在网页确认
3. 授权已被拒绝

**解决方案**：
1. 确认网页显示的授权码与本地一致
2. 点击确认按钮
3. 重新运行 `--wait`

### 凭证文件损坏

**症状**：读取 `memory/axiqra-auth.json` 时报错

**解决方案**：
```bash
# 删除损坏的凭证
rm memory/axiqra-auth.json

# 重新授权
node scripts/auth.js --start
```

---

## 连接问题

### API 无法访问

**症状**：`fetch` 请求超时或失败

**诊断步骤**：
```bash
# 运行诊断
node scripts/auth.js --doctor

# 手动测试连接
curl -v https://api.axiqra.com/api/health
```

**常见原因**：
1. 网络问题
2. API 地址配置错误
3. 防火墙阻止

**解决方案**：

| 原因 | 解决方案 |
|------|---------|
| 网络问题 | 检查网络连接 |
| API 地址错误 | 重新设置 `AXIQRA_API_URL` |
| 防火墙 | 添加防火墙规则 |

### 本地模式不生效

**症状**：设置了 `AXIQRA_API_URL=http://localhost:8080` 但仍然连接生产环境

**检查**：
```bash
# 检查环境变量
echo $AXIQRA_API_URL

# 检查配置文件
cat memory/axiqra-config.json
```

**解决方案**：
1. 确保环境变量已正确设置
2. 重新运行脚本
3. 检查 `memory/axiqra-config.json` 是否被覆盖

---

## 脚本错误

### 模块未找到

**症状**：`Error: Cannot find module '...'`

**解决方案**：
```bash
# 初始化 npm
npm init -y

# 安装依赖
npm install

# 或使用安装脚本
node scripts/install.js
```

### ES Module 错误

**症状**：`SyntaxError: Cannot use import statement outside a module`

**解决方案**：
在 `package.json` 中添加：
```json
{
  "type": "module"
}
```

或在脚本中：
```javascript
#!/usr/bin/env node
```

### Windows 路径问题

**症状**：路径解析错误（反斜杠问题）

**解决方案**：
```javascript
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
```

---

## API 错误

### 400 参数错误

**症状**：`Validation failed` 错误

**检查**：
1. 必填字段是否完整
2. 字段类型是否正确
3. JSON 格式是否有效

**示例**：
```json
// 错误：缺少必填字段
{ "query": "test" }

// 正确：包含所有必填字段
{
  "query": "test",
  "tech_stack": "Spring Boot",
  "max_results": 5
}
```

### 401 未授权

**症状**：`Unauthorized` 错误

**原因**：
1. 凭证过期
2. Token 无效
3. 缺少 Authorization 头

**解决方案**：
```bash
# 检查凭证
node scripts/auth.js --check

# 如需刷新，重新授权
node scripts/auth.js --start
```

### 403 禁止访问

**症状**：`Forbidden` 错误

**原因**：
1. 权限不足
2. 操作被限制

**解决方案**：
1. 检查账号权限
2. 联系管理员

### 422 验证失败

**症状**：`Validation failed` 错误

**检查**：
```javascript
// 检查错误详情
const result = await response.json();
console.log(result.detail); // 详细的验证错误信息
```

**常见问题**：
1. 字段值超出范围
2. 格式不正确
3. 枚举值错误

### 429 请求过于频繁

**症状**：`Rate limited` 错误

**解决方案**：
1. 等待后重试
2. 减少请求频率
3. 使用缓存

---

## 性能问题

### 搜索响应慢

**可能原因**：
1. 网络延迟
2. 查询数据量大
3. 服务器负载高

**解决方案**：
1. 减少 `max_results` 参数
2. 添加查询条件过滤
3. 使用缓存

### 脚本执行慢

**诊断**：
```bash
# 测量执行时间
time node scripts/rest_request.js GET /search/before-act
```

**优化**：
1. 减少不必要的 API 调用
2. 使用会话缓存
3. 并行请求（适当场景）

---

## 诊断工具

### Doctor 脚本

运行完整诊断：
```bash
node scripts/auth.js --doctor
```

输出示例：
```
══════════════════════════════════════
 Axiqra MCP 诊断
══════════════════════════════════════

  模式检测              [PRODUCTION]
    API: https://api.axiqra.com
    WEB: https://www.axiqra.com

  API 连接              ✓ 可达

  授权文件              ✓ 已授权
    工作区: ws-xxx

  配置文件              ✓ 存在
    版本: 1.0.0

══════════════════════════════════════
  通过: 4 / 4
  状态良好
```

### 网络测试

```bash
# 测试 API 可达性
curl -I https://api.axiqra.com/api/health

# 测试认证端点
curl -X POST https://api.axiqra.com/api/auth/device/code

# 测试授权流程
curl -X POST https://api.axiqra.com/api/auth/device/token \
  -d "device_code=xxx"
```

---

## 获取帮助

### 自助解决

1. 查阅本文档
2. 运行 `node scripts/auth.js --doctor`
3. 检查错误日志

### 联系支持

如问题无法解决：

1. 收集诊断信息
2. 准备错误日志
3. 联系 Axiqra 支持

### 反馈问题

发现 Bug 或功能建议：
1. 在 GitHub 提交 Issue
2. 提供诊断信息
3. 描述复现步骤
