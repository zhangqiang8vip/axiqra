# Security Policy / 安全政策

## 中文说明

Axiqra 围绕工程轨迹、日志、上下文和工程决策工作，这些内容可能意外包含敏感信息。

请不要在公开 issue 中发布敏感安全报告。

## 报告漏洞

如果你发现漏洞或敏感数据暴露风险，请先私下联系维护者。

当前维护者：

```text
Zhang Qiang
GitHub: zhangqiang8vipp
Website: https://www.axiqra.com/
```

如果暂时没有私密报告渠道，请只在公开 issue 中写高层描述，并请求私密联系方式。不要在 issue 中包含利用细节、secret、私有日志或客户信息。

## 敏感数据规则

请不要提交：

- API key、token、密码、私钥或 session cookie
- 真实客户数据
- 内部 hostname、私有 IP 或基础设施标识
- 私有仓库 URL
- 机密源代码
- 尚未披露的漏洞利用细节

请使用占位符：

```text
<SECRET_REDACTED>
<TOKEN_REDACTED>
<CUSTOMER_REDACTED>
<PRIVATE_PATH_REDACTED>
<PRIVATE_REPO_REDACTED>
<INTERNAL_HOST_REDACTED>
```

## 欢迎安全评审的方向

- trace 脱敏规则
- Public Case / Private Case 边界
- 授权与 tool scope
- MCP/API/CLI 回写安全
- 可信来源与污染隔离控制
- 依赖和供应链风险

---

## English

Axiqra is designed around engineering traces, which can accidentally contain sensitive information.

Please do not publish sensitive security reports as public issues.

## Reporting a Vulnerability

If you find a vulnerability or sensitive data exposure risk, please contact the maintainer privately first.

Current maintainer:

```text
Zhang Qiang
GitHub: zhangqiang8vipp
Website: https://www.axiqra.com/
```

If a private reporting channel is not yet available, open a public issue with only a high-level description and request a private contact path. Do not include exploit details, secrets, private logs, or customer information in the issue.

## Sensitive Data Rules

Do not submit API keys, tokens, passwords, private keys, customer data, internal hostnames, private IPs, private repository URLs, confidential source code, or unreleased vulnerability exploit details.

Use placeholders such as `<SECRET_REDACTED>`, `<TOKEN_REDACTED>`, `<CUSTOMER_REDACTED>`, `<PRIVATE_PATH_REDACTED>`, `<PRIVATE_REPO_REDACTED>`, and `<INTERNAL_HOST_REDACTED>`.

## Security Focus Areas

The project especially welcomes review of trace redaction rules, public/private Case boundaries, authorization and tool scopes, MCP/API/CLI writeback safety, trusted-source and contamination controls, and dependency or supply-chain risks.

