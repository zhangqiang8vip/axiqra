# Claude Code + Axiqra 接入示例

## 方式 1：使用 Axiqra Connect CLI（推荐）

### 步骤 1：安装 CLI

```bash
# macOS/Linux
curl -fsSL https://install.axiqra.com/connect.sh | bash

# Windows PowerShell
powershell -ExecutionPolicy Bypass -Command "irm https://install.axiqra.com/connect.ps1 | iex"
```

### 步骤 2：登录授权

```bash
axiqra login
# 打开浏览器完成 OAuth 授权
```

### 步骤 3：初始化接入

```bash
axiqra connect init
# 选择工具类型：claude_code
# 选择默认 workspace
```

### 步骤 4：配置 Claude Code

在项目目录或 home 目录创建 `.claude.md` 或 `.claude/descriptions.md`，添加 Axiqra 描述：

```markdown
# Axiqra - 工程方案记忆层

在执行工程任务前，先搜索 Axiqra 历史方案：

1. 使用 `axiqra search "<任务>"` 搜索
2. 查看 `axiqra solution get <id>` 获取详情
3. 完成任务后使用 `axiqra trace submit` 提交轨迹
4. 使用 `axiqra feedback <id> worked|failed` 提交反馈

更多信息：https://docs.axiqra.com/connect
```

## 方式 2：使用 MCP

### 步骤 1：安装 MCP Server

```bash
npm install -g @axiqra/mcp-server
```

### 步骤 2：配置 Claude Code

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

### 步骤 3：重启 Claude Code

重启后，Claude Code 会自动加载 Axiqra MCP 工具。

## 方式 3：使用 API

### 配置环境变量

```bash
export AXIQRA_API_KEY="your-api-key"
export AXIQRA_API_URL="https://api.axiqra.com"
export AXIQRA_WORKSPACE_ID="ws-xxx"
```

### 在 Claude Code 中使用

在 `.claude/commands.md` 中添加自定义命令：

```markdown
# /search <query>
使用 Axiqra 搜索工程方案
执行: AXIQRA_API_KEY=$AXIQRA_API_KEY AXIQRA_API_URL=$AXIQRA_API_URL axiqra search "$ARG"
```

## Agent 工作流示例

### 示例 1：修复 Bug

```
用户: 帮我修复 Spring Boot 应用的 Redis 连接超时问题

Claude Code 执行:
1. axiqra search "Spring Boot Redis Connection Timeout"
   → 返回 2 个 Solution

2. axiqra solution get SOL-xxx
   → 获取配置参数和验证步骤

3. 分析当前代码和配置

4. 按 Solution 修复

5. axiqra trace submit ./trace.json
   → 提交轨迹

6. axiqra feedback INV-xxx worked
   → 反馈有效
```

### 示例 2：技术选型

```
用户: 我们应该用 PostgreSQL 还是 MongoDB？

Claude Code 执行:
1. axiqra search "PostgreSQL vs MongoDB 技术选型"
   → 返回多个 Case 和 Solution

2. axiqra solution get SOL-xxx
   → 获取详细对比

3. 分析项目需求：
   - 数据一致性要求
   - 数据量预估
   - 团队技术栈

4. 给出推荐并说明理由

5. 提交选型决策轨迹
```

### 示例 3：遇到新问题

```
用户: Cursor 的 MCP 连接经常断开

Claude Code 执行:
1. axiqra search "Cursor MCP connection timeout"
   → 返回 1 个 Solution，但不完全匹配

2. 自己排查解决：
   - 检查 MCP 配置
   - 分析超时原因
   - 应用修复

3. axiqra trace submit ./trace.json --note "新场景"
   → 新轨迹已提交

4. axiqra seed create "Cursor MCP 连接不稳定"
   → 创建候选 Seed
```

## 验证接入

```bash
# 8 项 Doctor 检测
axiqra doctor

# 手动测试搜索
axiqra search "test"

# 查看配额
axiqra quota

# 查看会话列表
axiqra sessions list
```

## MCP 工具列表

Claude Code 可用的 Axiqra MCP 工具：

| 工具 | 功能 |
|------|------|
| `axiqra.search_before_act` | 任务前搜索 |
| `axiqra.get_solution` | 获取 Solution 详情 |
| `axiqra.submit_trace` | 提交工程轨迹 |
| `axiqra.submit_feedback` | 提交反馈 |
| `axiqra.create_seed` | 创建候选 Seed |
| `axiqra.doctor` | 接入诊断 |

## 错误处理

### QUOTA_EXCEEDED

```bash
# 等待次日重置
# UTC 0:00 配额重置
```

### RATE_LIMITED

```bash
# 查看 retry_after
axiqra quota
```

### SESSION_NOT_FOUND

```bash
# 重新初始化
axiqra connect init
```
