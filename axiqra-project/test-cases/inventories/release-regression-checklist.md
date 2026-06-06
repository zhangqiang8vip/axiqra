# 发布回归检查清单

## 1. 清单说明
本清单用于在关键提交、版本发布或合并前，执行最小必要的回归确认，降低基础能力回退风险。

## 2. 使用原则
- 仅勾选本次变更实际需要验证的项目；
- 若涉及安全、配置、启动、数据库或公共依赖调整，应适当扩大验证范围；
- 执行后应将结论同步到正式测试记录或 PR 测试计划中。

## 3. 最小回归检查项

```md
## Release regression checklist
- [ ] Confirm changed modules are identified
- [ ] Run targeted tests for changed behavior
- [ ] Run module-level tests when change scope exceeds one file
- [ ] Run project-level tests when shared config or dependencies are affected
- [ ] Confirm archived reports are updated when formal validation is required
- [ ] Confirm no temporary `target` artifacts are used as formal documentation
```

## 4. 分层验证建议
- [ ] 若本次仅影响单个明确场景，执行与该场景直接对应的定向测试
- [ ] 若本次影响同一模块内多个文件、接口或配置，执行模块级测试
- [ ] 若本次影响根构建配置、共享依赖、启动流程或跨模块行为，执行项目级测试
- [ ] 若形成正式验证结论，更新 `reports/` 中的归档报告
- [ ] 若新增测试类或测试场景，更新 `inventories/` 下对应清单文件

## 5. 结果记录建议
执行本清单后，建议至少补充以下信息：
- 本次验证范围；
- 实际执行命令；
- 结果摘要；
- 是否需要新增归档报告或补充测试。
