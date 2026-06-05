# Axiqra AI 代码审查机器人配置指南

本文档说明如何为 Axiqra 项目配置和启用各种 AI 代码审查机器人。

---

## 目录

- [1. CodeRabbit](#1-coderabbit)
- [2. GitHub Copilot](#2-github-copilot)
- [3. Gemini](#3-gemini)
- [4. OpenRouter](#4-openrouter)
- [5. Codex](#5-codex)
- [6. Agnes AI](#6-agnes-ai)
- [7. Cursor BugBot](#7-cursor-bugbot)
- [8. 全部启用后的效果](#8-全部启用后的效果)

---

## 0. 前置准备

### 添加 GitHub Secrets

前往 **Settings → Secrets and variables → Actions** 添加以下 Secret：

| Secret | 用途 | 示例值 |
|--------|------|--------|
| `GEMINI_API_KEY` | Gemini 审查 | Google AI Studio 获取 |
| `OPENROUTER_API_KEY` | OpenRouter 审查 | OpenRouter.ai 注册获取 |
| `API_BASE_URL` | Codex 审查 API 地址 | `https://free.v36.cm` |
| `OPENAI_API_KEY` | Codex 审查 Key | 对应 API Key |
| `AGNES_API_BASE_URL` | Agnes AI API 地址 | `https://apihub.agnes-ai.com/v1` |
| `AGNES_API_KEY` | Agnes AI Key | `sk-N6X6vCFW2d4vgAo6REX6iyIQGywc12AGnGjViffJa7zXCoHu` |
| `AGNES_REVIEW_MODEL` | Agnes 审查模型 | `agnes-2.0-flash` |

---

## 1. CodeRabbit

**定位**：全自动 AI 审查机器人，无需配置 API Key，安装 App 即可使用。

### 启用步骤

1. 访问 [coderabbit.ai](https://coderabbit.ai/)
2. 点击 **"Login with GitHub"** 授权
3. 在 Dashboard 点击 **"Add Repository"**，选择 `axiqra` 仓库
4. 可选：自定义审查规则，仓库根目录已放置 `.github/coderabbit.yaml`

### 功能特性

- 自动审查 PR 中的代码变更
- 生成 PR 摘要和变更说明
- 识别潜在 bug 和安全风险
- 支持中文审查（已配置 `language: zh-CN`）
- 在 PR 评论中实时交互

### 自定义配置

已配置 `.github/coderabbit.yaml`：
- 审查文件类型：`.mjs`, `.js`, `.ts`, `.tsx`, `.md`, `.yml`, `.json`
- 排除目录：`node_modules`, `dist`, `宣传`, `fixtures/v2.0`
- 自动审查：已启用

### 验证

安装后在任意 PR 中查看 CodeRabbit 的评论即可。

---

## 2. GitHub Copilot

**定位**：基于项目规范的变更范围分析，无需 API Key，GitHub Enterprise 订阅即可用。

### 启用方式

GitHub Copilot 审查通过 `.github/copilot-instructions.md` 提供项目上下文。项目规范文件已创建：

`.github/copilot-instructions.md` 包含：
- 项目概述和技术栈说明
- 安全、MCP、错误处理、测试覆盖等审查维度
- Axiqra 特定的代码规范
- 审查阈值定义（Block Merge / Comment）

### 功能特性

- 分析 PR 变更范围
- 识别变更类型（安全/MCP/测试/API）
- 生成结构化审查摘要
- 在 PR 评论中发布审查结果
- 创建 Check Run 状态

### 验证

在 PR 中查看 Copilot Review 评论。

---

## 3. Gemini

**定位**：Google Gemini Flash 模型，提供快速代码审查，需要 `GEMINI_API_KEY`。

### 启用步骤

1. 获取 Gemini API Key：[Google AI Studio](https://makersuite.google.com/app/apikey)
2. 添加 Secret：`GEMINI_API_KEY`
3. 完成！workflow `.github/workflows/gemini-review.yml` 会自动触发

### 工作流

- 触发条件：PR 打开/同步/重新打开
- 调用 Gemini API 进行代码审查
- 在 PR 评论中发布审查结果

### 功能特性

- 识别安全漏洞（硬编码密钥、注入风险）
- 检查 MCP 协议合规性
- 评估错误处理质量
- 提供修复建议

### 费用

Gemini Flash 模型价格极低（免费额度充足），适合日常审查。

---

## 4. OpenRouter

**定位**：聚合多个 LLM 模型的统一网关，使用 `openrouter/free` 模型，无需指定具体 Key。

### 启用步骤

1. 注册 OpenRouter：[openrouter.ai](https://openrouter.ai/)
2. 添加 Secret：`OPENROUTER_API_KEY`
3. 完成！workflow `.github/workflows/openrouter-review.yml` 会自动触发

### 工作流

- 触发条件：PR 打开/同步/重新打开
- 调用 OpenRouter API（`openrouter/free` 模型）
- 在 PR 评论中发布审查结果

### 功能特性

- 自动路由到最优免费模型
- 支持多种 LLM 后端
- 与 GitHub Actions 无缝集成

---

## 5. Codex

**定位**：OpenAI-compatible API 提供代码审查，需要 `API_BASE_URL` + `OPENAI_API_KEY`。

### 启用步骤

1. 获取 API 代理地址和 Key（如 `https://free.v36.cm` + 对应 Key）
2. 添加 Secrets：
   - `API_BASE_URL` = `https://free.v36.cm`
   - `OPENAI_API_KEY` = 填入 API Key
3. 将 `.github/workflows/codex-review.yml.disabled` 重命名为 `.github/workflows/codex-review.yml` 即可启用
4. 模型默认为 `gpt-4o-mini`

### 工作流

- 触发条件：PR 打开/同步/重新打开
- 调用兼容 OpenAI 格式的 API（curl 直接调用）
- 在 PR 评论中发布审查结果

---

## 6. Agnes AI

**定位**：Agnes AI 平台提供的快速审查服务，需要 `AGNES_API_BASE_URL` + `AGNES_API_KEY` + `AGNES_REVIEW_MODEL`。

### 启用步骤

1. 添加 Secrets：
   - `AGNES_API_BASE_URL` = `https://apihub.agnes-ai.com/v1`
   - `AGNES_API_KEY` = `sk-N6X6vCFW2d4vgAo6REX6iyIQGywc12AGnGjViffJa7zXCoHu`
   - `AGNES_REVIEW_MODEL` = `agnes-2.0-flash`
2. 将 `.github/workflows/agnes-review.yml.disabled` 重命名为 `.github/workflows/agnes-review.yml` 即可启用

### 工作流

- 触发条件：PR 打开/同步/重新打开
- 调用 Agnes AI API（OpenAI 兼容格式）
- 在 PR 评论中发布审查结果

---

## 7. Cursor BugBot

**定位**：Cursor IDE 内置的 PR 审查工具，适合本地深度审查。

### 启用方式

1. 在 Cursor 中打开 PR（通过 Cursor Composer 或 Branch 面板）
2. BugBot 会自动分析代码变更
3. 在 Cursor 中直接查看审查意见

### 与 GitHub Actions 的区别

| 对比 | Cursor BugBot | GitHub Actions Workflows |
|------|--------------|------------------------|
| 运行环境 | 本地 IDE | 云端 CI |
| 审查深度 | 深，可实时交互 | 基于 diff 的静态分析 |
| 触发方式 | 手动打开 PR | PR 事件自动触发 |
| 交互性 | 高，可多轮对话 | 一次性评论 |

---

## 8. 全部启用后的效果

### PR 创建后的完整流水线

```
PR 打开
  │
  ├─→ GitHub Copilot Review (免费，自动)
  │      └─→ PR 评论：变更范围分析
  │
  ├─→ Gemini Review (需要 GEMINI_API_KEY)
  │      └─→ PR 评论：安全 + MCP + 错误处理审查
  │
  ├─→ OpenRouter Review (需要 OPENROUTER_API_KEY)
  │      └─→ PR 评论：免费模型代码审查
  │
  ├─→ Codex Review (需要 API_BASE_URL + OPENAI_API_KEY)
  │      └─→ PR 评论：深度代码审查 + 修复建议
  │
  ├─→ Agnes AI Review (需要 AGNES_* secrets)
  │      └─→ PR 评论：Agnes 平台代码审查
  │
  ├─→ CodeRabbit (免费，App 安装)
  │      └─→ PR 评论：AI 交互审查 + PR 摘要
  │
  ├─→ Axiqra Quality Gates (免费)
  │      ├─→ 测试门控
  │      ├─→ 安全扫描 (Trivy)
  │      └─→ 代码质量检查 (ESLint)
  │
  └─→ Cursor BugBot (本地 IDE)
         └─→ 实时深度审查（开发者本地）
```

### 评论示例

每个机器人都会在 PR 中留下独特视角的评论：

- **Copilot**：变更范围摘要，文件分类
- **Gemini**：安全风险、MCP 合规性评估
- **OpenRouter**：免费模型综合审查
- **Codex**：深度代码建议，修复示例
- **Agnes AI**：Agnes 平台审查
- **CodeRabbit**：交互式审查，PR 摘要
- **Quality Gates**：测试结果，安全扫描结果

### 推荐配置

| 团队规模 | 推荐组合 | 说明 |
|---------|---------|------|
| 个人/小团队 | CodeRabbit + OpenRouter + Codex | 最少配置，覆盖安全审查 |
| 中型团队 | CodeRabbit + OpenRouter + Codex + Gemini + Agnes AI | 全方位覆盖 |
| 大型团队 | 全套 + Cursor BugBot + 人工审查 | 多层审查保障 |

---

## 故障排除

### Gemini 审查未触发
- 检查 `GEMINI_API_KEY` Secret 是否正确设置

### OpenRouter 审查未触发
- 检查 `OPENROUTER_API_KEY` Secret 是否正确设置

### Codex 审查未触发
- 检查 `API_BASE_URL` 和 `OPENAI_API_KEY` Secrets 是否正确设置
- 确认 workflow 已启用（`.yml.disabled` 已改名）

### Agnes AI 审查未触发
- 检查 `AGNES_API_BASE_URL`、`AGNES_API_KEY`、`AGNES_REVIEW_MODEL` Secrets 是否正确设置
- 确认 workflow 已启用（`.yml.disabled` 已改名）

### CodeRabbit 未响应
- 确认已在 [coderabbit.ai](https://coderabbit.ai/) 中授权了仓库

### Copilot Review 失败
- Copilot Review 依赖 GitHub Actions，无需额外配置

---

## 相关文件

| 文件 | 说明 |
|------|------|
| `.github/coderabbit.yaml` | CodeRabbit 审查配置 |
| `.github/copilot-instructions.md` | Copilot 项目规范 |
| `.github/workflows/gemini-review.yml` | Gemini 审查 workflow |
| `.github/workflows/openrouter-review.yml` | OpenRouter 审查 workflow |
| `.github/workflows/copilot-review.yml` | Copilot 审查 workflow |
| `.github/workflows/codex-review.yml` | Codex 审查 workflow |
| `.github/workflows/agnes-review.yml` | Agnes AI 审查 workflow |
| `.github/workflows/axiqra-quality-gates.yml` | 质量门控 workflow |
