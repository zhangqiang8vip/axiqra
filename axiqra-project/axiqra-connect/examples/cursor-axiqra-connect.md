# Cursor + Axiqra 接入示例

## 方式 1：使用 Axiqra Connect CLI（推荐）

### 步骤 1：安装 CLI

```powershell
# Windows
powershell -ExecutionPolicy Bypass -Command "irm https://install.axiqra.com/connect.ps1 | iex"
```

或

```bash
# macOS/Linux
curl -fsSL https://install.axiqra.com/connect.sh | bash
```

### 步骤 2：登录

```bash
axiqra login
# 会打开浏览器进行授权
```

### 步骤 3：初始化

```bash
axiqra connect init
```

### 步骤 4：在 Cursor Agent 中使用

在 Cursor Agent 的 System Prompt 中添加：

```
在执行任何工程任务前，先执行 `axiqra search "<任务关键词>"` 搜索历史方案。

完成任务后，执行 `axiqra trace submit --file ./trace.json` 提交工程轨迹。

如果方案有效，执行 `axiqra feedback <invocation_id> worked`。
如果方案无效，执行 `axiqra feedback <invocation_id> failed --reason "<原因>"`。
```

## 方式 2：使用 MCP

### 步骤 1：安装 MCP Server

```bash
npm install -g @axiqra/mcp-server
```

### 步骤 2：配置 Cursor

在项目根目录创建 `.cursor/mcp.json`：

```json
{
  "mcpServers": {
    "axiqra": {
      "command": "npx",
      "args": ["-y", "@axiqra/mcp-server"],
      "env": {
        "AXIQRA_API_KEY": "your-api-key",
        "AXIQRA_WORKSPACE_ID": "ws-xxx"
      }
    }
  }
}
```

### 步骤 3：重启 Cursor

重启 Cursor 后，MCP 工具会自动加载。

## 方式 3：使用 Skill Pack

### 获取 API Key

1. 登录 Axiqra 网页
2. 进入 设置 → API Keys
3. 创建新的 API Key

### 配置环境变量

```bash
export AXIQRA_API_KEY="your-api-key"
export AXIQRA_API_URL="https://api.axiqra.com"
export AXIQRA_WORKSPACE_ID="ws-xxx"
```

### 在 Cursor Agent 中使用

在 Cursor Agent 的 System Prompt 中添加：

```
你可以通过 Axiqra 获取工程方案记忆。

1. 搜索历史方案：
   - 执行 MCP 工具 `axiqra.search_before_act`
   - 输入你的任务目标、技术栈、风险等级

2. 获取方案详情：
   - 执行 MCP 工具 `axiqra.get_solution`
   - 查看执行步骤、验证方式、风险提示

3. 提交工程轨迹：
   - 执行 MCP 工具 `axiqra.submit_trace`
   - 记录任务目标、执行步骤、决策理由、验证证据

4. 提交反馈：
   - 执行 MCP 工具 `axiqra.submit_feedback`
   - 报告 worked / partial / failed 结果
```

## 验证接入

```bash
# 检查 Doctor 状态
axiqra doctor

# 测试搜索
axiqra search "Redis 连接池配置"

# 查看配额
axiqra quota
```

## 常见问题

### Q: Doctor 检测失败

```bash
# 查看详细日志
axiqra doctor --verbose

# 检查网络
axiqra doctor --check network

# 检查认证
axiqra doctor --check auth
```

### Q: MCP 连接超时

检查 `AXIQRA_API_URL` 是否正确：

```bash
echo $AXIQRA_API_URL
# 应该输出: https://api.axiqra.com
```

### Q: 配额超限

```bash
# 查看配额使用
axiqra quota

# 每日配额 2000 次
# 配额在 UTC 00:00 重置
```
