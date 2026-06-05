# 开源范围

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

这些知识经常消失在：AI 对话、PR 评论、issue 讨论、本地笔记、维护者个人记忆和一次性 AI 编程会话。

Axiqra 的开放组件希望让这些知识变成结构化、可审核、可复用、可被 Codex 等工具调用的工程记忆。

## 与 Codex for OSS 的关系

Axiqra 更适合通过对生态有重要作用的路径申请。Codex 可以帮助 Axiqra 审核协议设计、构建 MCP/CLI 原型、生成维护者工作流样例，以及改进脱敏和安全审查。

---

[English](../docs/OPEN_SOURCE_SCOPE.md) | [中文](OPEN_SOURCE_SCOPE_zh.md)
