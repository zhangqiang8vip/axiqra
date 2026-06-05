# Security Policy

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

Use placeholders:

```text
<SECRET_REDACTED>
<TOKEN_REDACTED>
<CUSTOMER_REDACTED>
<PRIVATE_PATH_REDACTED>
<PRIVATE_REPO_REDACTED>
<INTERNAL_HOST_REDACTED>
```

## Security Focus Areas

The project especially welcomes review of:

- trace redaction rules
- public/private Case boundaries
- authorization and tool scopes
- MCP/API/CLI writeback safety
- trusted-source and contamination controls
- dependency or supply-chain risks

---

[English](SECURITY.md) | [中文](../i18n/SECURITY_zh.md)
