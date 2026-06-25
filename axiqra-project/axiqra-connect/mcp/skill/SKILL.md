---
name: axiqra-agent
description: "Axiqra — AI 工程方案记忆基础设施。核心能力：①搜索历史方案 ②草稿记录 ③轨迹沉淀 ④反馈闭环。"
version: 1.0.0
user-invocable: true
homepage: https://www.axiqra.com
---

# Axiqra Agent Skill

跨 Agent 宿主的可移植 Skill 包。安装后，Agent 自动具备工程方案记忆能力。

## 核心能力

| 能力 | 说明 |
|------|------|
| 搜索历史方案 | 任务前搜索相似经验，有则复用 |
| 草稿记录 | 随时记录开发步骤，不打断工作流 |
| 轨迹沉淀 | 完成后将经验沉淀到方案库 |
| 反馈闭环 | 评价方案有效性，持续优化 |

## 工作流程

```
任务开始 → search_before_act → 有历史?
                            ↓
              ┌─────────────┴─────────────┐
           有方案                      无方案
              ↓                           ↓
         复用方案                    自行开发
              ↓                           ↓
         draft_trace                 draft_trace
              ↓                           ↓
        用户说"好了"?               用户说"好了"?
              ↓                           ↓
         flush_trace                create_seed
              ↓                           ↓
         feedback                   flush_trace
```

## 快速开始

### 1. 安装 & 授权

```bash
node {AXIQRA_SKILL_DIR}/scripts/install.js
```

或单独授权：

```bash
# 获取授权码
node {AXIQRA_SKILL_DIR}/scripts/auth.js --start

# 等待授权（展示 user_code 给用户后执行）
node {AXIQRA_SKILL_DIR}/scripts/auth.js --wait <device_code>
```

### 2. 搜索历史方案

```bash
node {AXIQRA_SKILL_DIR}/scripts/rest_request.js POST /search/before-act \
  --file {AXIQRA_SKILL_DIR}/memory/sessions/{SESSION_ID}/request-search.json
```

### 3. 记录草稿

```bash
node {AXIQRA_SKILL_DIR}/scripts/rest_request.js POST /traces/draft \
  --file {AXIQRA_SKILL_DIR}/memory/sessions/{SESSION_ID}/request-draft.json
```

### 4. 提交轨迹

```bash
node {AXIQRA_SKILL_DIR}/scripts/rest_request.js POST /traces/flush \
  --file {AXIQRA_SKILL_DIR}/memory/sessions/{SESSION_ID}/request-trace.json
```

### 5. 提交反馈

```bash
node {AXIQRA_SKILL_DIR}/scripts/rest_request.js POST /v1/feedbacks \
  --file {AXIQRA_SKILL_DIR}/memory/sessions/{SESSION_ID}/request-feedback.json
```

## 用户请求路由

| 用户说法 | Agent 动作 |
|----------|------------|
| "搜索一下历史方案" | POST /search/before-act |
| "有没有类似的问题" | POST /search/before-act |
| "记录一下" | POST /traces/draft |
| "先记着" | POST /traces/draft |
| "好了，提交吧" | 询问 outcome → POST /traces/flush |
| "这个方案好用" | POST /v1/feedbacks (worked) |
| "方案有误" | POST /v1/feedbacks (failed) |
| "查看当前草稿" | GET /traces/draft/current |

## 面向用户输出规则

**禁止贴给用户：**
- 原始 JSON 响应
- API 返回结构
- 错误堆栈
- UUID 长列表

**必须告诉用户：**
- 使用了哪个 Axiqra 能力
- 简短结果（1-3 个关键点）
- 下一步建议

**输出模板：**
```
结果：<查到或完成了什么>
关键数据：<方案数量/状态/匹配度>
下一步：<一个建议动作>
```

## 会话隔离

- 每个会话创建 `{AXIQRA_SKILL_DIR}/memory/sessions/{SESSION_ID}/`
- 请求载荷写入会话文件
- 会话内复用同一 SESSION_ID

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `AXIQRA_API_URL` | API 地址 | `https://api.axiqra.com` |
| `AXIQRA_WEB_URL` | 网页地址 | `https://www.axiqra.com` |
| `AXIQRA_SKILL_DIR` | Skill 安装目录 | - |

## 本地开发模式

```bash
# Windows
set AXIQRA_API_URL=http://localhost:8080/api
set AXIQRA_WEB_URL=http://localhost:5173

# Linux/Mac
export AXIQRA_API_URL=http://localhost:8080/api
export AXIQRA_WEB_URL=http://localhost:5173
```

## 诊断

```bash
node {AXIQRA_SKILL_DIR}/scripts/auth.js --doctor
```

## 参考文档

- `API_REFERENCE.md` — API 接口
- `PLAYBOOKS.md` — 业务流程
- `HOSTS.md` — 宿主兼容性
- `SAFETY.md` — 安全规则
- `TROUBLESHOOTING.md` — 排障指南
- `rules/axiqra-agent-rules.mdc` — Agent 行为规范
- `.cursorrules` — Cursor Skill 文件
- `.claude` — Claude Code 规则文件
