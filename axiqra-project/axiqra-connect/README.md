# Axiqra Connect

AI Agent 一键接入 Axiqra 工程方案记忆层。

## 一键安装命令

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
# 1. 登录
axiqra login

# 2. 诊断
axiqra doctor

# 3. 搜索
axiqra search "Spring Boot Redis 配置"

# 4. 提交轨迹
axiqra trace submit ./trace.json
```

## 目录结构

```
axiqra-connect/
├── README.md
├── package.json
├── install.sh                    # macOS/Linux 一键安装
├── install.ps1                   # Windows 一键安装
├── bin/
│   ├── axiqra-connect.sh        # Unix 安装脚本
│   └── axiqra-connect.ps1       # Windows 安装脚本
├── cli/
│   ├── README.md                # CLI 完整文档
│   ├── config.yaml
│   └── scripts/
│       ├── axiqra-cli.mjs      # CLI 主程序
│       ├── draft-manager.mjs    # 草稿管理器
│       ├── queue.mjs            # 轨迹队列管理
│       └── .axiqra/
│           └── config.json      # CLI 配置
├── mcp/
│   ├── README.md                # MCP 文档
│   ├── protocol.mjs            # MCP 协议定义
│   ├── server.mjs              # MCP Server
│   ├── auth.js                  # 授权脚本
│   ├── skill/                   # Skill 包（发布用）
│   │   ├── SKILL.md            # Agent Skill 文档
│   │   ├── API_REFERENCE.md     # API 接口文档
│   │   ├── PLAYBOOKS.md         # 业务流程
│   │   ├── HOSTS.md            # 宿主兼容性
│   │   ├── SAFETY.md           # 安全规则
│   │   ├── TROUBLESHOOTING.md   # 排障指南
│   │   ├── manifest.json        # Skill 清单
│   │   └── scripts/
│   │       ├── auth.js
│   │       ├── rest_request.js
│   │       ├── install.js
│   │       ├── cruise_tick.js
│   │       ├── update_skill.js
│   │       └── validate_skill.js
│   └── memory/
│       └── axiqra-config.json   # MCP 配置
├── tests/
│   ├── integration.test.mjs
│   ├── protocol.test.mjs
│   └── server.test.mjs
└── .axiqra/
    └── config.json              # 全局配置
```

## CLI 功能

| 命令 | 说明 |
|------|------|
| `axiqra login` | 登录（设备授权） |
| `axiqra whoami` | 查看当前用户 |
| `axiqra doctor` | 接入诊断 |
| `axiqra search <query>` | 搜索历史方案 |
| `axiqra solution get <id>` | 获取方案详情 |
| `axiqra trace submit <file>` | 提交轨迹 |
| `axiqra trace queue` | 查看待提交队列 |
| `axiqra feedback <id> <type>` | 提交反馈 |
| `axiqra seed create <query>` | 创建候选 Seed |
| `axiqra draft trace <goal>` | 记录草稿步骤 |
| `axiqra draft flush <id>` | 提交草稿为轨迹 |
| `axiqra config get/set` | 配置管理 |

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

## Agent 接入

在 Agent 的 System Prompt 中添加：

```
你接入了 Axiqra 工程方案记忆层。

1. 行动前搜索：在执行任何工程任务前，先执行 `axiqra search "<任务关键词>"` 搜索历史方案。
2. 获取详情：执行 `axiqra solution get <id>` 获取方案详情和执行步骤。
3. 轨迹回传：完成任务后执行 `axiqra trace submit --file ./trace.json` 提交工程轨迹。
4. 反馈闭环：执行 `axiqra feedback <invocation_id> worked|partial|failed` 报告结果。
```

## MCP 接入配置

在 Cursor / Claude Code 的 MCP 配置中添加：

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

## 文档

- [CLI 文档](./cli/README.md)
- [MCP 文档](./mcp/README.md)
- [Skill 文档](./mcp/skill/SKILL.md)

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `AXIQRA_API_URL` | API 地址 | `https://api.axiqra.com` |
| `AXIQRA_WEB_URL` | 网页地址 | `https://www.axiqra.com` |
| `AXIQRA_API_KEY` | API Key | - |
| `AXIQRA_TOKEN` | 登录 Token | - |
| `AXIQRA_CONFIG_DIR` | 配置目录 | `~/.axiqra` |

## 许可证

Apache License 2.0
