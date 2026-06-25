# Axiqra Host Compatibility Guide

本文档描述 Axiqra Skill 在不同宿主平台上的差异和配置方法。

## 目录

- [Cursor](#cursor)
- [Claude Code](#claude-code)
- [VS Code Copilot](#vs-code-copilot)
- [通用配置](#通用配置)

---

## Cursor

### 安装方式

1. **通过 MCP 配置**：在 Cursor 设置中添加 MCP Server
2. **通过 Skill 安装脚本**：运行 `node scripts/install.js`

### MCP 配置

在 Cursor 设置中添加：

```json
{
  "mcpServers": {
    "axiqra": {
      "command": "node",
      "args": ["/path/to/server.mjs"],
      "env": {
        "AXIQRA_API_URL": "https://api.axiqra.com",
        "AXIQRA_API_KEY": "your-api-key"
      }
    }
  }
}
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `AXIQRA_API_URL` | API 地址 | `https://api.axiqra.com` |
| `AXIQRA_WEB_URL` | 网页地址 | `https://www.axiqra.com` |
| `AXIQRA_API_KEY` | API 密钥 | - |
| `AXIQRA_SKILL_DIR` | Skill 目录 | 自动检测 |

### 识别特征

- `CURSOR_ID` 环境变量
- `CURSOR_TELEMETRY_ID` 环境变量
- User-Agent 包含 `Cursor`

### 规则配置

在 `.cursorrules` 或 `CLAUDE.md` 中添加：

```markdown
# Axiqra 集成

## 任务前搜索
在开始任何开发任务前，先搜索 Axiqra 历史方案：
- 调用 `axiqra.search_before_act`
- 如有匹配方案，优先复用

## 记录步骤
长时间任务中，定期记录步骤：
- 调用 `axiqra.submit_trace`
- 记录关键决策和证据

## 任务后沉淀
任务完成后，提交轨迹：
- 调用 `axiqra.submit_trace`
- 如有有价值经验，创建 Seed
```

---

## Claude Code

### 安装方式

1. **安装到全局规则目录**：
```bash
mkdir -p ~/.claude/rules/
cp {AXIQRA_SKILL_DIR}/.claude ~/.claude/rules/axiqra.md
```

2. **通过 npm 安装**：
```bash
npm install -g axiqra-skill
```

### 规则配置

在 `~/.claude/settings.json` 中添加：

```json
{
  "rules": [
    "axiqra.md"
  ]
}
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `AXIQRA_API_URL` | API 地址 | `https://api.axiqra.com` |
| `AXIQRA_API_KEY` | API 密钥 | - |
| `AXIQRA_SKILL_DIR` | Skill 目录 | `~/.axiqra/` |

### 识别特征

- `CLAUDE_CODE` 环境变量存在
- User-Agent 包含 `Claude`

### CLI 使用

```bash
# 搜索历史方案
node ~/.axiqra/scripts/rest_request.js POST /search/before-act \
  --file ~/.axiqra/memory/sessions/{SESSION_ID}/request-search.json

# 记录步骤
node ~/.axiqra/scripts/rest_request.js POST /traces \
  --file ~/.axiqra/memory/sessions/{SESSION_ID}/request-draft.json

# 提交轨迹
node ~/.axiqra/scripts/rest_request.js POST /traces/{id}/submit
```

---

## VS Code Copilot

### 安装方式

1. **安装 VS Code 扩展**（待开发）
2. **通过命令行工具**：
```bash
npm install -g @axiqra/cli
axiqra init
```

### 配置

在 VS Code 设置中添加：

```json
{
  "axiqra.apiUrl": "https://api.axiqra.com",
  "axiqra.apiKey": "your-api-key"
}
```

### 识别特征

- `GITHUB_TOKEN` 环境变量
- `CLIENT_ID` 环境变量

### MCP 配置

```json
{
  "mcpServers": {
    "axiqra": {
      "command": "npx",
      "args": ["@axiqra/mcp-server"],
      "env": {
        "AXIQRA_API_KEY": "your-api-key"
      }
    }
  }
}
```

---

## 通用配置

### 目录结构

```
{AXIQRA_SKILL_DIR}/
├── SKILL.md
├── manifest.json
├── scripts/
│   ├── auth.js
│   ├── install.js
│   ├── rest_request.js
│   ├── cruise_tick.js
│   └── update_skill.js
└── memory/
    ├── axiqra-config.json
    ├── axiqra-auth.json
    └── sessions/
        └── {SESSION_ID}/
```

### Skill 目录检测顺序

1. `AXIQRA_SKILL_DIR` 环境变量
2. `{当前脚本目录}/../`
3. `{用户目录}/.axiqra/`
4. `/opt/axiqra/`（Linux）
5. `{当前目录}/.axiqra/`

### 会话 ID

每个宿主会话应使用唯一的 `SESSION_ID`：

| 宿主 | SESSION_ID 来源 |
|------|----------------|
| Cursor | 自动生成 |
| Claude Code | `CLAUDE_SESSION_ID` 或自动生成 |
| VS Code | 工作区路径哈希 |
| 通用 | 随机 UUID |

### 宿主能力检测

```javascript
function detectHost() {
  if (process.env.CURSOR_ID) return 'cursor';
  if (process.env.CLAUDE_CODE) return 'claude-code';
  if (process.env.GITHUB_TOKEN && process.env.CLIENT_ID) return 'copilot';
  if (process.env.WINDSURF_API_KEY) return 'windsurf';
  return 'unknown';
}
```

---

## 差异对比

| 特性 | Cursor | Claude Code | VS Code |
|------|--------|-------------|---------|
| MCP 支持 | ✓ | ✓ | ✓ |
| Skill 目录 | 自动检测 | `~/.axiqra/` | 工作区 |
| 规则配置 | `.cursorrules` | `~/.claude/rules/` | 扩展设置 |
| 会话隔离 | ✓ | ✓ | ✓ |
| 巡航支持 | ✓ | ✓ | ✓ |
| 实时通知 | 扩展 | CLI | 扩展 |

---

## 常见问题

### Q: 如何在不同宿主间同步配置？

将 `memory/` 目录放在共享位置（如 Dropbox、iCloud），在每个宿主设置 `AXIQRA_SKILL_DIR` 指向同一位置。

### Q: 不同宿主可以共用同一账号吗？

可以，只需确保 `memory/axiqra-auth.json` 可被各宿主访问。

### Q: 如何在 Docker 容器中使用？

在 Dockerfile 中设置环境变量：

```dockerfile
ENV AXIQRA_API_URL=https://api.axiqra.com
ENV AXIQRA_API_KEY=your-api-key
ENV AXIQRA_SKILL_DIR=/app/axiqra
```
