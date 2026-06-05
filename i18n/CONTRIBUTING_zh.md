# Axiqra 贡献指南

感谢你帮助 Axiqra 成为对维护者、贡献者和 AI 编程工具有用的工程记忆基础设施。

## 当前适合贡献的方向

- Case、Solution、Invocation、Feedback、Engineering Trace Package 等术语改进
- schema 和对象模型评审
- MCP/API/CLI 接入设计
- OSS 维护者工作流样例
- 安全、脱敏和授权规则评审
- 文档修订和翻译
- 公开工程 Case 和 Solution 样例

## 如何贡献

1. 先开 issue 描述改进点或问题。
2. Pull request 尽量聚焦一个主题。
3. 说明你的改动支持哪类维护者或 AI Agent 工作流。
4. 不要提交私有代码、日志、密钥、客户名、内部域名或私有仓库路径。
5. 文档改动请链接相关文档。

## Public Case 规则

Public Case 不应包含：

- 真实 secret、token、API key、密码或私钥
- 客户数据或私有业务数据
- 内部域名、私有 IP 或基础设施名称
- 私有仓库 URL
- 未经许可复制的版权内容
- 应先负责任披露的漏洞细节

请使用占位符：

```text
<TOKEN_REDACTED>
<CUSTOMER_REDACTED>
<PRIVATE_REPO_REDACTED>
<INTERNAL_HOST_REDACTED>
```

## PR 期望

好的 PR 应包含：

- 简短摘要
- 解决的问题或改进的工作流
- 受影响文档链接
- 安全或隐私注意事项
- schema、协议或工作流变化的示例

---

[English](../docs/CONTRIBUTING.md) | [中文](CONTRIBUTING_zh.md)
