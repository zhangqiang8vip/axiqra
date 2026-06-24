# Axiqra Agent Skill - Agent 使用文档

## 概述

Axiqra Skill 包告诉 AI Agent 如何在任务前搜索历史方案、如何回传工程轨迹、如何提交反馈。

## 快速接入

### 1. 安装 CLI

```bash
# Windows
powershell -ExecutionPolicy Bypass -Command "irm https://install.axiqra.com/connect.ps1 | iex"

# macOS/Linux
curl -fsSL https://install.axiqra.com/connect.sh | bash
```

### 2. 登录授权

```bash
axiqra login
```

### 3. 在 Agent System Prompt 中添加

```
在执行工程任务前，先使用 Axiqra 搜索历史方案。

搜索命令: axiqra search "<任务关键词>"
获取详情: axiqra solution get <id>
提交轨迹: axiqra trace submit ./trace.json
反馈结果: axiqra feedback <id> worked|failed

更多信息: https://docs.axiqra.com/connect
```

## 核心能力

| 能力 | 命令 | 说明 |
|------|------|------|
| 任务前搜索 | `axiqra search` | AI 执行前先查历史方案 |
| 获取 Solution | `axiqra solution get` | 获取 AI 可执行视图 |
| 提交工程轨迹 | `axiqra trace submit` | 回传完整工程过程 |
| 提交反馈 | `axiqra feedback` | worked/failed/partial |
| Doctor 检测 | `axiqra doctor` | 检测接入状态 |

## 使用时机

### 必须搜索的场景

- 实现新功能前
- 遇到技术错误/异常
- 技术选型决策
- 使用新的库/框架/工具
- 排查生产问题
- 数据库迁移
- 架构变更

### 禁止行为

- ❌ 不搜索就决定技术选型
- ❌ 不搜索就开始排查问题
- ❌ 不搜索就实现新功能

## 规则文件

- `RULES/search-before-act.mdc` - 行动前搜索规则
- `RULES/trace-submit.mdc` - 轨迹提交规则
- `RULES/feedback.mdc` - 反馈规则

## MCP 工具

Agent 也可以通过 MCP 协议调用 Axiqra：

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

可用 MCP 工具：

| 工具 | 功能 |
|------|------|
| `axiqra.search_before_act` | 任务前搜索 |
| `axiqra.get_solution` | 获取详情 |
| `axiqra.submit_trace` | 提交轨迹 |
| `axiqra.submit_feedback` | 提交反馈 |
| `axiqra.create_seed` | 创建 Seed |
| `axiqra.doctor` | 接入诊断 |

## 相关文档

- [SKILL.md](./SKILL.md) - 完整 Skill 定义
- [manifest.json](./manifest.json) - Skill 清单
- [D09 - AI 工具接入协议](../../docs/_docs/09-AI工具接入、对话式自动接入、MCP、API、CLI与插件协议.md)
