# PR 测试计划模板

## 1. 模板说明
本模板用于在 Pull Request 中简洁表达测试范围、执行结果与确认项，便于评审人员快速判断变更风险。

## 2. 模板正文

```md
## Test plan
- [ ] Run `<test-command-1>`
- [ ] Run `<test-command-2>`
- [ ] Confirm result: `<expected-summary>`
- [ ] Confirm archived report exists at `<report-path>`
- [ ] Confirm no unintended impact on `<affected-area>`
```

## 3. 使用建议
- 仅保留与本次变更直接相关的测试项；
- 若为定向修复，优先写最小必要验证；
- 若涉及公共依赖、配置或启动流程，追加模块级或项目级验证项；
- 若没有正式归档报告，可删除归档确认项，但应保留结果确认项。
