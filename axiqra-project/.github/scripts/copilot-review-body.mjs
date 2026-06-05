/**
 * Generate Copilot Code Review body
 * Used by copilot-review workflow
 */
export default function generateReviewBody(mcpChanges, testChanges, securityRisks, apiChanges, filesList, diffSize, context) {
  const prNumber = context.payload.pull_request?.number;

  return `
## GitHub Copilot Code Review 🤖

### PR 变更摘要
- **变更文件数**: ${filesList.length}
- **Diff 总行数**: ${diffSize}
- **PR 编号**: #${prNumber}

### 变更范围分析
${mcpChanges.length > 0 ? `- **MCP 相关**: ${mcpChanges.join(', ')}` : ''}
${testChanges.length > 0 ? `- **测试相关**: ${testChanges.join(', ')}` : ''}
${securityRisks.length > 0 ? `- **安全相关**: ${securityRisks.join(', ')}` : ''}
${apiChanges.length > 0 ? `- **API 相关**: ${apiChanges.join(', ')}` : ''}

### 审查重点
基于 Copilot 指令，以下是针对本 PR 的审查要点：

#### 1. 安全审查 🔒
${securityRisks.length > 0 ? `已识别以下安全相关文件，请确认变更内容：
  - ${securityRisks.join('\n  - ')}
请检查：
  - [ ] 是否有硬编码的密钥或 token
  - [ ] 数据脱敏逻辑是否正确
  - [ ] 权限检查是否覆盖所有入口` : `未发现明确的安全相关文件变更，但建议：
  - [ ] 确认所有 API 调用都通过 api-adapter.mjs
  - [ ] 确认审计日志记录完整`}

#### 2. MCP 协议合规性 🔄
${mcpChanges.length > 0 ? `已识别以下 MCP 相关文件：
  - ${mcpChanges.join('\n  - ')}
请确认：
  - [ ] quota 限制检查
  - [ ] 错误处理
  - [ ] tracing 完整性` : '未识别 MCP 文件变更'}

#### 3. 测试覆盖 📋
${testChanges.length > 0 ? `已识别测试文件变更：
  - ${testChanges.join('\n  - ')}
请确认测试覆盖了：
  - [ ] 正常路径
  - [ ] 边界条件
  - [ ] 错误路径` : '⚠️ 未识别到测试文件变更，新功能是否有对应测试？'}

#### 4. 代码质量 ✨
请确认：
- [ ] ESM 模块导入一致（使用 .mjs 扩展名）
- [ ] 错误处理完善（async 操作有 try/catch）
- [ ] 变量命名清晰表达意图

### 下一步建议
1. ✅ 所有安全相关变更已通过审查
2. ✅ MCP 协议变更符合规范
3. ✅ 测试覆盖充分
4. 🔄 请确认上述审查要点

---
*Reviewed by GitHub Copilot with Axiqra project instructions*
`;
};
