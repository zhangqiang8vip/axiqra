# 提交测试说明模板

## 1. 模板说明
本模板用于编写提交说明中的测试部分，适用于 commit message 补充说明、变更记录或交付备注。

## 2. 英文提交说明模板

```text
test: <short-subject>

Add or update <test-scope> to verify <expected-behavior>. Verified with <execution-command> and archived report under <report-path>.
```

## 3. 中文补充说明模板

```text
补充或更新 <测试范围>，用于验证 <预期行为>。
已执行 <执行命令>，结果为 <结果摘要>，正式报告已归档至 <归档路径>。
```

## 4. 使用建议
- 第一行主题保持简洁；
- 正文重点说明为什么测、测了什么、结果如何；
- 不直接贴原始日志；
- 如无归档报告，可将最后一句替换为具体结果确认语句。
