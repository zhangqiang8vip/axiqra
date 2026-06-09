# R4 — Quota + Connect + MCP/CLI

> Round 4 开发成果测试归档目录

## 交付摘要

- `ConnectController`：提供 quota、rate-limit、doctor、session 创建、列表、详情接口
- `QuotaServiceImpl` + `RedisQuotaAdapter`：按每用户每日 2000 次 quota 控制，第 2001 次返回 `QUOTA_EXCEEDED`
- `RateLimitServiceImpl`：按每用户每分钟 100 次窗口限流，返回 `retry_after`
- `ConnectServiceImpl`：提供会话创建、8 项 doctor 检测、READY/DEGRADED/BLOCKED 状态流转，并通过 `ConnectSessionPort` 持久化抽象存储
- `src/s1-core/index.mjs` + `src/s1-core/protocol.mjs`：MCP 7 工具 + CLI 7 命令的协议化最小可调用入口
- `tests/s1-core/s1_core_closed_loop.test.mjs`：原型闭环样例测试
