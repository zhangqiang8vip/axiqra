# Axiqra Connect

AI Agent 一键接入 Axiqra 工程方案记忆层。

## 一键安装命令

复制以下命令到终端执行即可完成安装：

### Windows PowerShell

```powershell
# 一行命令安装（推荐）
irm https://install.axiqra.com/connect.ps1 | iex

# 或下载脚本后执行
.\install.ps1
```

### macOS / Linux

```bash
# 一行命令安装（推荐）
curl -fsSL https://install.axiqra.com/connect.sh | bash

# 或下载脚本后执行
chmod +x install.sh && ./install.sh
```

### npm

```bash
npm install -g @axiqra/cli
```

## 快速使用

```bash
# 1. 设置 API Key
export AXIQRA_API_KEY=your-api-key

# 2. 登录
axiqra login --api-key $AXIQRA_API_KEY

# 3. 诊断
axiqra doctor

# 4. 搜索
axiqra search "Spring Boot Redis 配置"
```

## Agent 接入

在 Agent 的 System Prompt 中添加：

```
你接入了 Axiqra 工程方案记忆层。

1. 行动前搜索：在执行任何工程任务前，先执行 `axiqra search "<任务关键词>"` 搜索历史方案。
2. 获取详情：执行 `axiqra solution get <id>` 获取方案详情和执行步骤。
3. 轨迹回传：完成任务后执行 `axiqra trace submit --file ./trace.json` 提交工程轨迹。
4. 反馈闭环：执行 `axiqra feedback <invocation_id> worked|partial|failed` 报告结果。
```

## MCP 接入

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

## 目录结构

```
axiqra-connect/
├── README.md
├── install.sh                    # macOS/Linux 一键安装
├── install.ps1                   # Windows 一键安装
├── bin/
│   ├── axiqra-connect.sh        # Unix 安装脚本
│   └── axiqra-connect.ps1       # Windows 安装脚本
├── cli/
│   ├── README.md
│   ├── config.yaml
│   └── scripts/
│       └── axiqra-cli.mjs
├── mcp/
│   ├── README.md
│   ├── protocol.mjs
│   └── server.mjs
├── skill/
│   ├── README.md
│   ├── SKILL.md
│   ├── manifest.json
│   └── RULES/
│       ├── search-before-act.mdc
│       ├── trace-submit.mdc
│       └── feedback.mdc
└── examples/
    ├── cursor-axiqra-connect.md
    ├── claude-code-axiqra-connect.md
    └── agent-workflow.md
```

## 文档

- [CLI 文档](./cli/README.md)
- [MCP 文档](./mcp/README.md)
- [Skill 文档](./skill/README.md)
- [SKILL.md](./skill/SKILL.md) - Agent 接入指南

## 许可证

Apache License 2.0
