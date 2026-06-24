---
name: axiqra-agent
description: "Axiqra — AI 工程方案记忆层。核心能力：①任务前搜索：在执行工程任务前搜索历史方案（类似 git pull before push）；②轨迹回传：完成任务后提交 Engineering Trace Package；③反馈闭环：报告方案 worked/failed/partial；④无命中创建 Seed：当搜索无结果时创建候选方案。适用于 Cursor、Claude Code、Codex、VS Code Copilot、Custom Agent 等 AI 编码工具。"
version: 1.0.0
user-invocable: true
homepage: https://www.axiqra.com
metadata: {"agent_skill":{"key":"axiqra-agent","aliases":["axiqra","axiqra-connect","axiqra连接","工程记忆","engineering memory"],"version":"1.0.0","priority":"first","fallback":true,"format":"manifest-first","distribution":{"preferred":"git","fallback":"zip"},"runtime":{"node":">=18","packages":[]},"permissions":["network:https://api.axiqra.com","fs:read-write:{AXIQRA_SKILL_DIR}/memory/","exec:node:{AXIQRA_SKILL_DIR}/scripts/*.js"],"entrypoints":["SKILL.md","RULES/search-before-act.mdc","RULES/trace-submit.mdc","RULES/feedback.mdc","manifest.json"],"output_contract":"machine: CLI/MCP emit JSON; human: summarize per Output Style","update_policy":"manual"}}
openclaw: {"emoji":"A","skillKey":"axiqra-agent","fallback":true,"requires":{"bins":["node"]}}
---

# Axiqra Agent Skill

Axiqra 是 AI 工程方案记忆层。当 AI Agent 执行工程任务前，先搜索历史方案；完成后回传轨迹；无命中时创建候选 Seed。

## 契约

- 当 AI Agent 在执行工程任务前，**必须**先调用 `axiqra search` 搜索历史方案。
- 当 AI Agent 完成任务后，**必须**整理并提交 Engineering Trace Package。
- 当 AI Agent 调用 Solution 后，**必须**提交反馈（worked / partial / failed）。
- 当 AI Agent 搜索无结果时，**应该**创建 Candidate Seed 帮助平台补全知识。
- Axiqra 同时服务两端：帮 AI Agent 快速获取历史方案（召回），与帮人类沉淀工程经验（沉淀）。

## 快速开始

### 一键安装

```bash
# Windows PowerShell
powershell -ExecutionPolicy Bypass -Command "irm https://install.axiqra.com/connect.ps1 | iex"

# macOS / Linux
curl -fsSL https://install.axiqra.com/connect.sh | bash

# npm
npm install -g @axiqra/cli
```

### 授权绑定

```bash
# API Key 方式（推荐）
axiqra login --api-key <your-api-key>

# OAuth 方式
axiqra login
# 会打开浏览器进行授权
```

### 初始化接入

```bash
axiqra connect init
axiqra doctor
```

### 在 Agent 中使用

复制以下内容到 Agent 的 System Prompt：

```
你接入了 Axiqra 工程方案记忆层。

1. 行动前搜索：在执行任何工程任务前，先执行 `axiqra search "<任务关键词>"` 搜索历史方案。
2. 获取详情：执行 `axiqra solution get <id>` 获取方案详情和执行步骤。
3. 轨迹回传：完成任务后执行 `axiqra trace submit --file ./trace.json` 提交工程轨迹。
4. 反馈闭环：执行 `axiqra feedback <invocation_id> worked|partial|failed` 报告结果。

更多信息：https://docs.axiqra.com/connect
```

## 核心工作流

```
┌─────────────────────────────────────────────────────────────┐
│                    Axiqra Agent 工作流                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 接收工程任务                                            │
│     ↓                                                       │
│  2. axiqra search → 搜索历史方案                            │
│     ↓                                                       │
│  3. 评估方案适配性 → R2+/R3+/R4 必须用户确认               │
│     ↓                                                       │
│  4. axiqra solution get → 获取执行步骤                      │
│     ↓                                                       │
│  5. 执行任务                                                │
│     ↓                                                       │
│  6. axiqra trace submit → 提交工程轨迹                      │
│     ↓                                                       │
│  7. 用户确认轨迹 → 生成 Project Case                        │
│     ↓                                                       │
│  8. axiqra feedback → 提交反馈                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## CLI 命令速查

### 搜索与方案

```bash
# 搜索历史方案
axiqra search "Spring Boot Redis 连接池配置"
axiqra search --tag java "微服务架构"
axiqra search --level L3 "K8s 部署"

# 获取方案详情
axiqra solution get SOL-xxx
axiqra solution get SOL-xxx --view execution

# 查看 Public Case
axiqra case get CASE-xxx
```

### 轨迹回传

```bash
# 提交工程轨迹
axiqra trace submit ./trace.json
axiqra trace submit ./trace.json --tag bug-fix

# 查看轨迹
axiqra trace list
axiqra trace get TRACE-xxx

# 删除轨迹
axiqra trace delete TRACE-xxx
```

### 反馈闭环

```bash
# 方案有效
axiqra feedback INV-xxx worked

# 部分有效
axiqra feedback INV-xxx partial --reason "版本不匹配"

# 无效
axiqra feedback INV-xxx failed --reason "场景不适用"

# 不适用
axiqra feedback INV-xxx not_applicable --reason "Go 项目不适用 Java 方案"
```

### 候选 Seed

```bash
# 创建候选 Seed
axiqra seed create "K8s 有状态服务部署方案"

# 查看 Seed
axiqra seed list
axiqra seed get SEED-xxx
```

### 诊断与状态

```bash
# 接入诊断
axiqra doctor
axiqra doctor --check network
axiqra doctor --check auth

# 配额状态
axiqra quota

# 会话管理
axiqra sessions list
axiqra sessions get SESSION-xxx
```

### 配置管理

```bash
# 查看配置
axiqra config list

# 设置配置
axiqra config set api-key <your-key>
axiqra config set api-url https://api.axiqra.com

# 查看用户
axiqra whoami
```

## Engineering Trace Package 格式

轨迹是 AI 完成一次工程任务的完整过程记录：

```json
{
  "trace": {
    "session_id": "agent-xxx-001",
    "task_goal": "修复 Redis 连接池耗尽问题",
    "environment": {
      "tech_stack": "Spring Boot 3.2 + Lettuce",
      "version": "JDK 17",
      "os": "Linux",
      "infrastructure": "K8s + AWS EKS"
    },
    "forward_path": [
      {
        "step": 1,
        "action": "分析连接池配置",
        "reason": "连接数持续上涨可能是池配置问题",
        "result": "max-idle=10, max-total=50"
      },
      {
        "step": 2,
        "action": "检查连接释放逻辑",
        "reason": "可能是连接未正确释放",
        "result": "发现异步任务中未关闭连接"
      }
    ],
    "reverse_path": [
      {
        "attempted": "增加 max-total",
        "reason_failed": "只是延迟问题，未解决根本原因",
        "evidence": "连接数继续上涨"
      }
    ],
    "decision_path": [
      {
        "decision": "修复连接释放逻辑而非扩大池",
        "options_considered": ["扩大连接池", "添加连接监控"],
        "selected": "修复释放逻辑",
        "reason": "治本优于治标"
      }
    ],
    "evidence_refs": [
      "logs/redis-connection.log",
      "test/connection-pool-test.js"
    ],
    "rollback_path": {
      "enabled": true,
      "steps": ["git revert HEAD", "kubectl rollout undo"]
    },
    "outcome": "success"
  }
}
```

### 脱敏规则

**禁止包含**：

- API Keys / Tokens / Passwords
- 客户数据
- 内网 IP / 域名
- 私有仓库 URL
- 真实姓名 / 邮箱

**使用占位符**：

```
<TOKEN_REDACTED>
<PASSWORD_REDACTED>
<CUSTOMER_REDACTED>
<INTERNAL_IP_REDACTED>
<PRIVATE_REPO_REDACTED>
```

## 验证等级与风险等级

### 验证等级（L0-L5）

| 等级 | 含义 | 使用建议 |
|------|------|---------|
| L0 | 草稿 | 仅参考 |
| L1 | 用户确认 | 需验证 |
| L2 | 有证据 | 可使用 |
| L3 | 可复现 | **优先使用** |
| L4 | 多上下文验证 | 高置信 |
| L5 | 跨上下文验证 | 最高置信 |

### 风险等级（R0-R4）

| 等级 | 含义 | 是否需要确认 |
|------|------|--------------|
| R0 | 无风险 | 不需要 |
| R1 | 低风险 | 建议确认 |
| R2 | 中风险 | **必须确认** |
| R3 | 高风险 | **必须人工确认** |
| R4 | 极高风险 | **禁止自动执行** |

## 反馈类型

| 类型 | 值 | 含义 | 影响 |
|------|-----|------|------|
| 好评 | `worked` | 方案有效解决问题 | +1 有效样本 |
| 一般 | `partial` | 部分有效，需要补充 | +0.5 样本 |
| 差评 | `failed` | 方案不适用或过时 | 触发复核 |
| 不适用 | `not_applicable` | 场景不匹配 | 更新边界 |

## 典型场景

### 场景 1：Cursor Agent 修复 Bug

```
用户: "帮我修复 Redis 连接超时问题"

Agent 执行:
1. axiqra search "Spring Boot Redis Connection Timeout"
   → 返回 2 个 Solution

2. axiqra solution get SOL-xxx
   → 获取配置参数和验证步骤

3. 按 Solution 执行修复
4. 测试验证

5. axiqra trace submit ./trace.json
   → 轨迹已提交

6. axiqra feedback INV-xxx worked
   → 反馈已记录
```

### 场景 2：遇到新问题

```
用户: "Cursor 的 MCP 连接经常断开"

Agent 执行:
1. axiqra search "Cursor MCP connection timeout"
   → 返回 1 个 Solution，但不完全匹配

2. 自己排查解决
3. axiqra trace submit ./trace.json --note "新场景，无现成方案"
   → 新轨迹已提交

4. axiqra seed create "Cursor MCP 连接不稳定"
   → 候选 Seed 已创建
```

### 场景 3：技术选型

```
用户: "我们要选型消息队列，Kafka vs RabbitMQ vs Redis Streams"

Agent 执行:
1. axiqra search "消息队列选型"
   → 返回多个 Case 和 Solution

2. 分析各方案适用场景
3. 结合当前项目上下文给出建议

4. axiqra solution get SOL-xxx
   → 获取详细对比

5. 向用户解释推荐理由
```

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

| MCP 工具 | 功能 |
|----------|------|
| `axiqra.search_before_act` | 任务前搜索 |
| `axiqra.get_solution` | 获取方案详情 |
| `axiqra.submit_trace` | 提交轨迹 |
| `axiqra.submit_feedback` | 提交反馈 |
| `axiqra.create_seed` | 创建候选 Seed |
| `axiqra.doctor` | 接入诊断 |

## 错误码

| 错误码 | 说明 | 处理 |
|--------|------|------|
| `QUOTA_EXCEEDED` | 每日配额用完 | 等待次日 UTC 0:00 重置 |
| `RATE_LIMITED` | 限流中 | 查看 `retry_after` 等待 |
| `UNAUTHORIZED` | 未登录 | 执行 `axiqra login` |
| `SESSION_NOT_FOUND` | 会话不存在 | 重新 `axiqra connect init` |
| `TRACE_MISSING_EVIDENCE` | 轨迹缺少证据 | 补充日志/截图/测试 |
| `VALIDATION_FAILED` | 验证失败 | 检查轨迹格式 |

## 配额与限制

| 项目 | 限制 |
|------|------|
| 每日配额 | 2000 次 |
| 限流窗口 | 100 次/分钟 |
| 配额重置 | UTC 0:00 |

## 关联文档

- `RULES/search-before-act.mdc` - 行动前搜索规则
- `RULES/trace-submit.mdc` - 轨迹提交规则
- `RULES/feedback.mdc` - 反馈规则
- `manifest.json` - Skill 清单
- [D09 - AI 工具接入协议](../../docs/_docs/09-AI工具接入、对话式自动接入、MCP、API、CLI与插件协议.md)

## 版本

当前版本：`1.0.0`

最后更新：2026-06-24
