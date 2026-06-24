# Claude Code 接入 Axiqra

本目录包含 Claude Code 接入 Axiqra 所需的配置和说明。

## 前置条件

1. 安装 Axiqra CLI: `npm install -g axiqra-cli`
2. 登录 Axiqra: `axiqra login`
3. Claude Code 版本支持 MCP

## 安装步骤

### 方式一：使用 Claude Code 原生 MCP

在 `~/.claude.json` 中配置：

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

### 方式二：使用 CLI 快速接入

```bash
axiqra connect claude-code
```

### 方式三：复制指令文件

1. 将 `CLAUDE.md` 复制到 `.claude/commands/axiqra.md`
2. 在 Claude Code 中通过 `/axiqra` 命令调用

## 验证安装

在 Claude Code 中执行以下命令验证：

```
/axiqra search 修复 Redis 连接池耗尽问题
```

应该能看到 Axiqra 返回的搜索结果。

## MCP 工具

Claude Code 通过 MCP 协议使用以下 Axiqra 工具：

| 工具 | 说明 |
|------|------|
| `axiqra.search_before_act` | 任务前搜索 |
| `axiqra.get_solution` | 获取方案详情 |
| `axiqra.get_public_case` | 获取公开案例 |
| `axiqra.submit_trace` | 提交轨迹 |
| `axiqra.submit_feedback` | 提交反馈 |
| `axiqra.create_seed` | 创建候选 |
| `axiqra.doctor` | 接入诊断 |

## 工作流程

1. **搜索** - 执行任务前先搜索相关方案
2. **评估** - 评估风险等级和验证等级
3. **执行** - 按方案步骤执行
4. **记录** - 提交工程轨迹
5. **反馈** - 用户评价后提交反馈

## 常见问题

### Q: MCP 连接失败

检查：
1. API Key 是否正确
2. 网络连接是否正常
3. 运行 `axiqra doctor` 诊断

### Q: 搜索结果不符合预期

调整搜索参数：
- 使用更精确的 `task_goal`
- 指定 `tech_stack`
- 添加 `error_signature`

### Q: 无法提交轨迹

检查：
1. Token 是否有效
2. 轨迹格式是否正确
3. 权限是否足够

## 更多信息

- [Axiqra CLI 文档](../cli/README.md)
- [Axiqra MCP Server 文档](../mcp/README.md)
- [D09 接入协议文档](../../../docs/_docs/09-AI工具接入、对话式自动接入、MCP、API、CLI与插件协议.md)
