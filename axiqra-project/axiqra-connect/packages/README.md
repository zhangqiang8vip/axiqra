# Axiqra AI 工具接入包

本目录包含 AI 工具自动接入所需的配置文件和说明文档。

## 目录结构

```
.
├── cursor/                 # Cursor IDE 接入包
│   ├── .cursorrules        # Cursor Rules 配置
│   └── README.md           # Cursor 接入说明
├── claude-code/            # Claude Code 接入包
│   ├── CLAUDE.md          # Claude Code 指令
│   └── README.md           # Claude Code 接入说明
├── codex/                  # Codex 接入包
│   ├── instructions.md     # Codex 系统指令
│   └── README.md          # Codex 接入说明
└── common/                 # 通用接入包
    ├── prompt.md           # 通用提示词
    └── skill.md            # 技能描述
```

## 快速开始

### Cursor

1. 打开 Cursor 设置
2. 进入 Rules 页面
3. 导入 `.cursorrules` 文件内容

### Claude Code

1. 在项目根目录创建 `.claude` 目录
2. 将 `claude-code/CLAUDE.md` 复制到 `.claude/commands/axiqra.md`
3. 在 `.claude/settings.json` 中启用 Axiqra 命令

### CLI

```bash
# 安装 CLI
npm install -g axiqra-cli

# 登录
axiqra login

# 创建接入会话
axiqra sessions create --channel cli --tool-type custom
```

## 文档

详细接入说明请参考 [D09 文档](../../docs/_docs/09-AI工具接入、对话式自动接入、MCP、API、CLI与插件协议.md)。
