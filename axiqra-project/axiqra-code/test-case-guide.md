# Axiqra 测试文档入口

本文件为 `axiqra-code` 目录下的就近访问入口，用于帮助开发同学快速找到当前项目的测试规范、正式测试报告、复用模板与测试清单。

## 1. 快速入口
- 目录总览：`../test-cases/README.md`
- 测试规范主文档：`../test-cases/standards/test-case-process.md`
- 正式测试报告：`../test-cases/reports/security-config-test-report.md`
- 测试记录模板：`../test-cases/templates/test-execution-record-template.md`
- PR 测试计划模板：`../test-cases/templates/pr-test-plan-template.md`
- 提交测试说明模板：`../test-cases/templates/commit-test-note-template.md`
- 模块测试资产清单：`../test-cases/inventories/module-test-inventory.md`
- 发布回归检查清单：`../test-cases/inventories/release-regression-checklist.md`

## 2. 推荐使用方式
### 2.1 当你需要新增一次测试记录时
- 优先复制模板：`../test-cases/templates/test-execution-record-template.md`
- 将整理后的正式记录归档到：`../test-cases/reports/`
- 不将 `target` 目录下的原始输出直接作为正式文档

### 2.2 当你需要编写 PR 测试计划时
- 优先参考：`../test-cases/templates/pr-test-plan-template.md`
- 只保留与本次变更直接相关的验证项
- 若形成正式验证结论，可同步补充归档报告

### 2.3 当你需要整理提交测试说明时
- 优先参考：`../test-cases/templates/commit-test-note-template.md`
- 重点写清楚为什么测、测了什么、结果如何
- 不直接粘贴大段原始日志

### 2.4 当你需要维护测试资产或回归清单时
- 模块测试资产清单：`../test-cases/inventories/module-test-inventory.md`
- 发布回归检查清单：`../test-cases/inventories/release-regression-checklist.md`
- 新增测试类、测试场景或发布检查项时应同步更新

## 3. 测试命令使用原则
### 3.1 定向验证
当本次变更只影响单个模块、单个测试类或单个明确场景时：
- 优先执行最小必要的定向测试命令；
- 命令应与本次变更范围直接对应；
- 执行结果应写入正式测试记录或 PR 测试计划。

### 3.2 模块级验证
当本次变更影响同一模块内多个文件、多个接口或同类配置时：
- 建议执行模块级测试；
- 以模块维度确认本次修改没有引入明显回归；
- 若模块已有固定验证命令，可在对应测试记录中引用。

### 3.3 项目级验证
当本次变更影响共享依赖、启动流程、根构建配置或跨模块行为时：
- 建议执行项目级测试；
- 必要时同步补充模块级或定向验证；
- 在正式记录中说明验证范围与风险判断。

## 4. 提交前测试资料检查
提交前至少确认以下事项：
- 本次变更对应的测试范围已经明确；
- 已选择与变更范围匹配的验证层级（定向 / 模块级 / 项目级）；
- 若形成正式测试结论，已归档到 `../test-cases/reports/`；
- 若新增测试类或测试场景，已同步更新相关清单；
- 不将 `target` 目录作为长期测试资料存放位置。

## 5. 使用建议
- 需要查看团队正式规范时，优先阅读 `../test-cases/standards/test-case-process.md`；
- 需要记录一次新的测试执行时，优先复制 `../test-cases/templates/test-execution-record-template.md`；
- 需要编写 PR 测试计划时，优先参考 `../test-cases/templates/pr-test-plan-template.md`；
- 需要整理提交说明时，优先参考 `../test-cases/templates/commit-test-note-template.md`；
- 需要维护模块覆盖范围或发布检查项时，优先更新 `../test-cases/inventories/` 下的清单文件；
- 不要将大段测试日志、XML 内容或临时排错输出直接保留在 `axiqra-code` 或 `target` 目录中作为正式文档。
