# Open-Source Scope / 开源范围

## 中文说明

Axiqra 是一个面向 AI 编程 Agent 和维护者的早期基础设施项目。它目前不声称自己已经是成熟、广泛采用的传统开源库。

本文档说明本仓库优先开放什么，哪些部分可能仍属于产品化能力，以及为什么 Axiqra 仍然和开源维护者生态有关。

## 当前开放内容

当前公开范围包括：

- 产品和协议文档
- Case、Public Case、Project Case、Solution、Invocation、Feedback、Review、Authorization 等概念
- 工程记忆对象模型
- Engineering Trace Package 设计
- AI 编程工具接入流程
- MCP/API/CLI 接入规格
- 维护者治理、审核、申诉和仲裁模型
- 脱敏、可信来源、污染隔离和授权规则
- 官网和品牌资产

## 计划开放组件

下一步开源工作预计聚焦：

- 可机器读取的 Case/Solution schema
- Engineering Trace Package JSON 格式
- 用于工程记忆检索的 MCP server 原型
- 用于 trace 导入、脱敏、校验和回写的 CLI 原型
- 面向 Codex 的维护者自动化样例
- 面向 OSS 工作流的公开 Case 和 Solution 样例
- schema 校验和脱敏行为的参考测试

## 可能保持产品化的部分

Axiqra 未来也可能包含托管产品、企业空间、计费、私有数据隔离、商业验证和托管服务能力。

这些产品化部分与上面列出的开放生态组件分开。

## 为什么这对 OSS 仍然重要

许多开源维护者会反复处理同类 issue、review、迁移、排障、发布问题和项目特有约定。

这些知识经常消失在：

- AI 对话
- PR 评论
- issue 讨论
- 本地笔记
- 维护者个人记忆
- 一次性 AI 编程会话

Axiqra 的开放组件希望让这些知识变成结构化、可审核、可复用、可被 Codex 等工具调用的工程记忆。

## 与 Codex for OSS 的关系

Axiqra 更适合通过“对生态有重要作用”的路径申请，而不是声称自己已经是广泛使用的 OSS 项目。

Codex 可以帮助 Axiqra：

- 审核和改进 schema 与协议设计
- 更快构建 MCP/CLI 原型
- 生成和测试维护者工作流样例
- 支持 issue triage、PR review、release、migration 和 debugging 工作流
- 改进工程轨迹回写中的脱敏和安全审查

---

## English

Axiqra is an early-stage infrastructure project for AI coding agents and maintainers. It is not claiming to be a mature, widely adopted open-source library yet.

This document explains what this repository is opening first, what may remain product-specific, and why the project is relevant to the open-source maintainer ecosystem.

## What Is Open Here

The current public scope includes:

- product and protocol documentation
- Case, Public Case, Project Case, Solution, Invocation, Feedback, Review, and Authorization concepts
- engineering memory object model
- Engineering Trace Package design
- AI coding tool integration flows
- MCP/API/CLI integration specifications
- maintainer governance, review, appeal, and arbitration model
- redaction, trusted-source, contamination, and authorization rules
- public website and brand assets

## Planned Open Components

The next open-source work is expected to focus on:

- machine-readable Case/Solution schema
- Engineering Trace Package JSON format
- MCP server prototype for engineering memory retrieval
- CLI prototype for trace import, redaction, validation, and writeback
- Codex-oriented maintainer automation examples
- public sample Cases and Solutions for OSS workflows
- reference tests for schema validation and redaction behavior

## What May Stay Product-Specific

Axiqra may also include hosted product, enterprise workspace, billing, private data isolation, commercial validation, and managed service features.

Those product-specific parts are separate from the ecosystem-facing open components listed above.

## Why This Still Matters for OSS

Many open-source maintainers repeatedly handle the same kinds of issues, reviews, migrations, debugging paths, release problems, and project-specific conventions.

That knowledge often disappears into chat sessions, pull request comments, issue threads, local notes, private maintainer memory, and one-off AI coding sessions.

Axiqra's open components aim to make that knowledge structured, reviewable, reusable, and callable by tools such as Codex.

## Codex for OSS Relevance

Axiqra is applying under the ecosystem-importance path rather than claiming to be a widely used OSS project today.

Codex can help this project by reviewing protocol design, building MCP/CLI prototypes faster, generating maintainer workflow examples, and improving redaction and security review for engineering trace writeback.

