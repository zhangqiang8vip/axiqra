# Axiqra MCP

AI 工程方案记忆基础设施。自动搜索历史方案，有就复用，没有就开发后沉淀。

## 一句话接入

让 AI 读取以下 SKILL.md：

```
https://oss.axiqra.com/skills/SKILL.md
```

然后对 AI 说：

> "请帮我接入 Axiqra"

AI 会自动完成一切。

## 工作原理

```
每次任务 → 搜索历史方案 → 复用 or 开发 → 沉淀新经验
```

## 核心能力

| 能力 | 说明 |
|------|------|
| 搜索历史方案 | 任务前自动搜索相似经验 |
| 草稿机制 | 长时间任务记录中间步骤 |
| 知识闭环 | 开发完成后沉淀到方案库 |

## MCP 工具

| 工具 | 说明 |
|------|------|
| `axiqra.search_before_act` | 任务前搜索历史方案 |
| `axiqra.get_solution` | 获取方案详情 |
| `axiqra.get_public_case` | 获取公开案例 |
| `axiqra.submit_trace` | 提交工程轨迹 |
| `axiqra.submit_feedback` | 提交调用反馈 |
| `axiqra.create_seed` | 创建候选 Seed |
| `axiqra.doctor` | 接入诊断 |

## 接入流程（AI 自动执行）

```
1. AI 从网上下载 SKILL.md 和 auth.js
2. AI 运行 auth.js --start，发起授权
3. AI 返回授权码给用户
4. 用户在 Axiqra 网页输入授权码
5. AI 运行 auth.js --wait 确认授权
6. AI 注册 MCP 工具
7. 完成！
```

## MCP 配置

### Cursor

在 Cursor 设置中添加：

```json
{
  "mcpServers": {
    "axiqra": {
      "command": "node",
      "args": ["/path/to/axiqra-connect/mcp/server.mjs"],
      "env": {
        "AXIQRA_API_URL": "https://api.axiqra.com",
        "AXIQRA_API_KEY": "your-api-key"
      }
    }
  }
}
```

### Claude Code

在 `~/.claude/settings.json` 中添加规则：

```json
{
  "rules": ["axiqra.md"]
}
```

## 本地开发模式

```bash
# Windows
set AXIQRA_API_URL=http://localhost:8080/api
set AXIQRA_WEB_URL=http://localhost:5173
node mcp/skill/scripts/auth.js --start

# Linux/Mac
export AXIQRA_API_URL=http://localhost:8080/api
export AXIQRA_WEB_URL=http://localhost:5173
node mcp/skill/scripts/auth.js --start
```

## 让其他 Agent 接入

### 方法一：一句话接入（推荐）

让其他 AI 说：

```
请帮我接入 Axiqra：https://oss.axiqra.com/skills/SKILL.md
```

### 方法二：手动配置 MCP Server

在 IDE 中配置：

```json
{
  "mcpServers": {
    "axiqra": {
      "command": "node",
      "args": ["/path/to/axiqra-connect/mcp/server.mjs"],
      "env": {
        "AXIQRA_API_URL": "http://localhost:8080/api",
        "AXIQRA_WEB_URL": "http://localhost:5173"
      }
    }
  }
}
```

## 文档

- [SKILL.md](https://oss.axiqra.com/skills/SKILL.md) - AI Agent 使用指南
- [API_REFERENCE.md](./skill/API_REFERENCE.md) - API 接口文档
- [PLAYBOOKS.md](./skill/PLAYBOOKS.md) - 业务流程
- [HOSTS.md](./skill/HOSTS.md) - 宿主兼容性
- [SAFETY.md](./skill/SAFETY.md) - 安全规则
- [TROUBLESHOOTING.md](./skill/TROUBLESHOOTING.md) - 排障指南
