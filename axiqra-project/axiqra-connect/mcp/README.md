# Axiqra Connect MCP Server

MCP Server 实现，用于让 AI 工具（如 Cursor、Claude Code）通过 MCP 协议调用 Axiqra。

## 功能

| 工具名称 | 功能 | 权限 | 说明 |
|----------|------|------|------|
| `axiqra.search_before_act` | 任务前搜索 | search:read | AI 执行前搜索历史方案 |
| `axiqra.get_solution` | 获取 Solution | solution:read | 获取 AI 可执行视图 |
| `axiqra.submit_trace` | 提交轨迹 | trace:write | 回传工程轨迹包 |
| `axiqra.submit_feedback` | 提交反馈 | feedback:write | worked/failed/partial |
| `axiqra.create_seed` | 创建 Seed | search:admin | 无命中时创建候选 |
| `axiqra.doctor` | 接入诊断 | connect:write | 检测接入状态 |

## 安装

### npm 安装

```bash
npm install -g @axiqra/mcp-server
```

### npx 直接运行

```bash
npx -y @axiqra/mcp-server
```

### 手动安装

```bash
git clone https://github.com/axiqra/mcp-server.git
cd mcp-server
npm install
npm run build
npm link
```

## 配置

### 环境变量

```bash
# API 地址
AXIQRA_API_URL=https://api.axiqra.com

# API Key
AXIQRA_API_KEY=your-api-key

# 工作空间 ID
AXIQRA_WORKSPACE_ID=ws-xxx

# 日志级别
AXIQRA_LOG_LEVEL=info
```

### Cursor 配置

在 `~/.cursor/mcp.json` 或项目 `.mcp.json` 中添加：

```json
{
  "mcpServers": {
    "axiqra": {
      "command": "npx",
      "args": ["-y", "@axiqra/mcp-server"],
      "env": {
        "AXIQRA_API_KEY": "your-api-key",
        "AXIQRA_API_URL": "https://api.axiqra.com"
      }
    }
  }
}
```

### Claude Code 配置

在 `~/.claude.json` 中添加：

```json
{
  "mcpServers": {
    "axiqra": {
      "command": "npx",
      "args": ["-y", "@axiqra/mcp-server"],
      "env": {
        "AXIQRA_API_KEY": "your-api-key"
      }
    }
  }
}
```

### VS Code Copilot 配置

在 VS Code 设置中添加：

```json
{
  "mcp": {
    "servers": {
      "axiqra": {
        "command": "npx",
        "args": ["-y", "@axiqra/mcp-server"],
        "env": {
          "AXIQRA_API_KEY": "your-api-key"
        }
      }
    }
  }
}
```

## 使用示例

### search_before_act

```javascript
// MCP 工具调用
const result = await mcpServer.search_before_act({
  task_goal: "修复 Redis 连接池耗尽问题",
  tech_stack: "Spring Boot 3.x + Lettuce",
  environment: "K8s + AWS RDS",
  risk_hint: "production"
});
```

### get_solution

```javascript
const solution = await mcpServer.get_solution({
  solution_id: "SOL-xxx",
  view_mode: "execution"
});
```

### submit_trace

```javascript
const trace = await mcpServer.submit_trace({
  trace_payload: {
    task_goal: "...",
    forward_path: [...],
    decision_path: [...],
    evidence_refs: [...]
  },
  idempotency_key: "unique-key"
});
```

### submit_feedback

```javascript
const feedback = await mcpServer.submit_feedback({
  invocation_id: "INV-xxx",
  feedback_type: "worked",
  evidence_refs: ["test-output.log"]
});
```

## 开发

```bash
# 安装依赖
npm install

# 开发模式
npm run dev

# 构建
npm run build

# 测试
npm test

# 类型检查
npm run typecheck
```

## 协议定义

### 请求格式

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "axiqra.search_before_act",
    "arguments": {
      "task_goal": "...",
      "tech_stack": "..."
    }
  }
}
```

### 响应格式

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "..."
      }
    ]
  }
}
```

### 错误格式

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": {
    "code": -32600,
    "message": "Invalid Request",
    "data": {
      "request_id": "req-xxx",
      "error_code": "QUOTA_EXCEEDED"
    }
  }
}
```

## 错误处理

| 错误码 | HTTP 状态 | 说明 |
|--------|-----------|------|
| `-32600` | 400 | 请求格式错误 |
| `-32601` | 404 | 工具不存在 |
| `-32602` | 422 | 参数验证失败 |
| `-32603` | 500 | 内部错误 |
| `-32001` | 401 | 未认证 |
| `-32002` | 429 | 配额超限 |
| `-32003` | 429 | 限流中 |

## 许可

Apache License 2.0
