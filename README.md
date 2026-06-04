# Axiqra

面向 AI 编程工具和开源维护者的 Agent-ready engineering memory。

Agent-ready engineering memory for AI coding tools and open-source maintainers.

官网 / Website: https://www.axiqra.com/

## 中文简介

Axiqra 是一个早期基础设施项目，目标是把真实工程任务沉淀为可复用、可审核、可被 AI Agent 调用的工程记忆。

它希望让 Codex、Cursor、Claude Code、Gemini CLI、企业自研 Agent 等 AI 编程工具，在动手修改代码之前先检索已有工程方案，在任务完成之后再回写结构化工程轨迹。

一句话：

```text
不要让 AI 编程 Agent 每次都从零推理已经被真实验证过的工程路径。
```

## 为什么需要 Axiqra

开源维护者和工程团队经常反复处理同类问题：

- 重复 issue 和 bug 模式
- PR review 中反复出现的判断
- 迁移、发布、回滚和兼容性问题
- 项目特有的实现约定
- 已经失败、但后来又被重复尝试的方案
- 只存在于评论、聊天、本地笔记或维护者记忆里的排障路径

Axiqra 希望把这些知识沉淀为结构化的 Case、Solution、证据、适用边界、回滚说明和 worked/failed 反馈。

## 开源范围

Axiqra 目前不是一个成熟、广泛使用的传统 OSS 库。它仍处于产品、协议和生态组件设计阶段。

这个仓库首先开放面向生态的部分：

- Case、Public Case、Solution、Invocation、Feedback 等概念
- Engineering Trace Package 格式
- MCP/API/CLI 接入设计
- 面向 Codex 的维护者工作流
- 治理、审核、脱敏和可信来源规则
- 面向贡献者和审核者的公开文档

更完整说明见 [OPEN_SOURCE_SCOPE.md](OPEN_SOURCE_SCOPE.md)。

## 仓库结构

```text
docs/       产品、协议、架构、治理和实施规格
logo/       Axiqra logo 资产
宣传/       官网页面和宣传素材
```

## 当前状态

Axiqra 处于早期公开设计阶段。

当前仓库重点包括：

- 产品和协议规格
- 对象模型和生命周期设计
- 维护者和社区治理
- AI 工具接入流程
- 官网与公开传播素材

下一步计划推进开放协议组件、MCP 集成、CLI 工作流和参考样例。

## Axiqra 与 Codex

Axiqra 设计上适合接入 Codex 风格的工程流程：

1. 修改代码前，先检索历史 Solution 和 Public Case。
2. 根据证据、边界、风险说明和回滚路径判断方案是否适用。
3. 在目标仓库中执行工程任务。
4. 任务完成后回写 Engineering Trace Package，记录 worked、failed 和可复用部分。
5. 由维护者审核、改进并发布可复用工程记忆。

这可以服务 issue triage、PR review、release notes、迁移、排障、新贡献者 onboarding 和仓库自动化等 OSS 场景。

## 文档入口

建议从这里开始：

- [docs/README.md](docs/README.md)
- [docs/00-Axiqra产品介绍与全流程总览.md](docs/00-Axiqra产品介绍与全流程总览.md)
- [docs/09-AI工具接入、对话式自动接入、MCP、API、CLI与插件协议.md](docs/09-AI工具接入、对话式自动接入、MCP、API、CLI与插件协议.md)
- [docs/16-社区贡献、维护者、仲裁与争议处理机制.md](docs/16-社区贡献、维护者、仲裁与争议处理机制.md)

## 贡献

欢迎贡献：

- 开放 schema 和术语改进
- MCP/API/CLI 工作流设计评审
- 开源维护者工作流样例
- 脱敏、安全和授权规则改进
- 文档修订和翻译
- 不清楚概念或缺失 OSS 场景的 issue

请先阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 安全

Axiqra 处理的工程轨迹可能意外包含代码、日志、密钥、私有路径、客户名或内部基础设施信息。

报告敏感问题前请阅读 [SECURITY.md](SECURITY.md)。

## 许可证

本仓库采用 Apache License 2.0，见 [LICENSE](LICENSE)。

---

## English Overview

Axiqra is an early-stage infrastructure project for turning real engineering work into reusable, reviewable, and agent-callable memory.

It is designed to help AI coding tools such as Codex search previous engineering decisions before acting, and write back structured traces after tasks are completed.

The goal is simple:

```text
Do not make AI coding agents reason from zero when a real engineering path has already been verified.
```

## Why This Matters

Open-source maintainers often solve the same classes of problems repeatedly:

- recurring issues and bug patterns
- pull request review decisions
- migration, release, rollback, and compatibility problems
- project-specific implementation conventions
- failed approaches that should not be repeated
- debugging paths that live only in comments, chats, local notes, or maintainer memory

Axiqra aims to preserve that knowledge as structured Cases, Solutions, evidence, boundaries, rollback notes, and worked/failed feedback.

## Open-Source Scope

Axiqra is not presented as a mature, widely used OSS library today. The project is currently in an early product and protocol design phase.

This repository is being opened to publish the ecosystem-facing parts first:

- Case, Public Case, Solution, Invocation, and Feedback concepts
- Engineering Trace Package format
- MCP/API/CLI integration design
- Codex-oriented maintainer workflows
- governance, review, redaction, and trusted-source rules
- public documentation for contributors and reviewers

See [OPEN_SOURCE_SCOPE.md](OPEN_SOURCE_SCOPE.md) for the exact scope.

## How Codex Fits

Axiqra is designed to work with Codex-style engineering workflows:

1. Search previous Solutions and Public Cases before making a change.
2. Use evidence, boundaries, risk notes, and rollback paths to decide whether a Solution applies.
3. Execute the engineering task in the target repository.
4. Write back an Engineering Trace Package with what worked, what failed, and what should be reused.
5. Let maintainers review, improve, and publish reusable engineering memory.

This can support OSS workflows such as issue triage, PR review, release notes, migrations, debugging, onboarding, and repository automation.

