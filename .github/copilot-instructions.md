# GitHub Copilot Review Instructions for Axiqra Project
# This file provides project-specific context for Copilot code reviews

# ============================================================
# 项目概述
# ============================================================
## 项目名称：Axiqra - 工程记忆与知识增强系统
## 核心能力：MCP (Model Context Protocol) 适配、搜索服务、审计追踪、权限配额管理
## 技术栈：Node.js (ESM), JavaScript, GitHub Actions, MCP

# ============================================================
# 代码审查维度
# ============================================================

## 1. 安全与隐私（最高优先级）
- [ ] 敏感信息硬编码检测（API 密钥、token、密码、内部域名/IP）
- [ ] 数据脱敏（redaction.mjs）是否正确应用
- [ ] 用户输入校验与边界情况处理
- [ ] 权限检查（policy.mjs）是否覆盖所有入口
- [ ] 审计日志（audit.mjs）是否记录所有写操作

## 2. MCP 协议合规性
- [ ] MCP 资源访问是否符合 quota 限制
- [ ] 超出配额时的错误处理（mcp-adapter.mjs, mcp-service.mjs）
- [ ] MCP 请求的 tracing 是否完整（trace-service.mjs, trace-schema.mjs）
- [ ] API 适配层（api-adapter.mjs）是否正确转换协议

## 3. 搜索与数据质量
- [ ] 搜索服务的召回率和精度逻辑（search-service.mjs）
- [ ] JSON Store（json-store.mjs）的并发安全与数据一致性
- [ ] 公开内容 vs 私有内容的隔离是否正确实现
- [ ] 搜索结果的排序和相关性评分逻辑

## 4. 错误处理与容错
- [ ] 所有 async 操作是否有 try/catch
- [ ] 错误是否正确分类并返回适当的 HTTP 状态码
- [ ] 服务降级策略是否合理
- [ ] 超时和重试机制

## 5. 测试覆盖率
- [ ] 新功能是否有对应测试
- [ ] 测试是否覆盖边界条件（配额边界、权限边界）
- [ ] mock/fixture 是否与真实 API 行为一致
- [ ] E2E 测试是否覆盖真实用户场景

## 6. 代码风格与可维护性
- [ ] ESM 模块导入是否一致（.mjs 扩展名）
- [ ] 错误对象（errors.mjs）是否包含足够上下文
- [ ] 默认配置（defaults.mjs）是否合理且有文档
- [ ] 函数和变量命名是否清晰表达意图

# ============================================================
# 项目特定规范
# ============================================================

## 文件命名规范
- 核心服务：src/s1-core/*.mjs
- API 适配：src/s1-core/api-adapter.mjs
- MCP 适配：src/s1-core/mcp-adapter.mjs
- MCP 服务：src/s1-core/mcp-service.mjs
- 搜索服务：src/s1-core/search-service.mjs
- 审计服务：src/s1-core/audit.mjs
- 追踪服务：src/s1-core/trace-service.mjs
- 错误定义：src/s1-core/errors.mjs
- 策略控制：src/s1-core/policy.mjs
- 数据脱敏：src/s1-core/redaction.mjs
- 配额审查：src/s1-core/review-service.mjs
- 存储层：src/s1-core/json-store.mjs, src/s1-core/store.mjs

## 测试文件结构
- 单元测试：tests/s1-core/
- E2E 测试：tests/e2e/
- API 测试：tests/api/
- 鉴权测试：tests/authz/
- 审计测试：tests/audit/
- Fixture 数据：fixtures/v2.0/

## 审查输出格式
发现问题时，请按以下格式输出：
```
[严重程度] 文件:行号 - 问题描述
  → 建议修复方式
  → 相关上下文（可选）
```

## 审查阈值
- 任何安全漏洞 → 必须修复（Block Merge）
- 运行时错误（未捕获异常）→ 必须修复（Block Merge）
- 测试失败 → 必须修复（Block Merge）
- 代码风格问题 → 建议修复（Comment）
- 文档缺失 → 建议补充（Comment）
