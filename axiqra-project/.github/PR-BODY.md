## Summary

配置多个 AI 代码审查机器人，为 Axiqra 项目提供自动化的 PR 代码审查能力。

- **GitHub Copilot Review**: PR 变更范围分析，自动评论
- **Gemini Review**: Google Gemini Flash 模型审查（需要 GEMINI_API_KEY）
- **Codex Review**: OpenAI GPT-4o 深度审查（需要 OPENAI_API_KEY）
- **CodeRabbit**: AI 交互式审查，安装 App 即可使用

## Changes

| 文件 | 说明 |
|------|------|
| `.github/workflows/copilot-review.yml` | Copilot 审查 workflow |
| `.github/workflows/gemini-review.yml` | Gemini 审查 workflow |
| `.github/workflows/codex-review.yml` | Codex 审查 workflow |
| `.github/coderabbit.yaml` | CodeRabbit 配置文件 |
| `.github/copilot-instructions.md` | Copilot 项目审查规范 |
| `.github/scripts/codex-review.mjs` | Codex 审查脚本 |
| `.github/REVIEW-BOTS-SETUP.md` | 配置指南文档 |

## Test plan

- [ ] PR 创建后检查 Copilot Review 是否自动评论
- [ ] 安装 CodeRabbit App 后验证审查评论
- [ ] 设置 GEMINI_API_KEY 后验证 Gemini 审查
- [ ] 设置 OPENAI_API_KEY 后验证 Codex 审查
