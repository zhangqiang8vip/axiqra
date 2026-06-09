# Axiqra 测试资料目录说明

## 1. 目录定位
本目录用于集中存放 Axiqra 项目所有与测试相关的规范文档、正式归档报告、复用模板与资产清单。

建立本目录的目的包括：
- 统一项目内测试资料的存放位置；
- 规范测试记录、测试报告与测试说明的整理方式；
- 为提交说明与 Pull Request 描述提供可复用模板；
- 为后续回归验证、发布检查和代码评审提供可追溯依据。

## 2. 适用范围
本目录主要覆盖 `axiqra-code` 下的工程测试资料，重点面向项目内持续演进的测试流程与测试资产管理。

适用范围包括：
- `axiqra-project/axiqra-code`；
- `src/test/java` 下的测试类与相关测试场景；
- 归档在本目录内的正式测试报告；
- 根据实际执行结果整理出的测试记录、提交说明与评审材料。

## 3. 目录结构
本目录采用分层组织结构：

- `standards/`
  - 存放测试规范、测试流程、记录要求与归档原则；
- `reports/`
  - 存放正式归档的测试报告；
- `templates/`
  - 存放测试记录模板、PR 测试计划模板、提交测试说明模板等复用材料；
- `inventories/`
  - 存放模块测试资产清单、回归测试清单与发布验证清单；
- `README.md`
  - 本目录的用途说明、命名约定与维护原则。

## 4. 文件分类说明
本目录中的文件建议按用途划分为以下几类：

- 规范类文档
  - 例如：`standards/test-case-process.md`
  - 用于说明测试资料应该如何组织、如何记录、如何归档；
- 报告类文档
  - 例如：`reports/` 下的归档报告文件
  - 用于保留某一次或某一类正式测试执行结论；
- 模板类文档
  - 例如：`templates/test-execution-record-template.md`
  - 用于复用标准结构，降低新增测试资料时的整理成本；
- 清单类文档
  - 例如：`inventories/module-test-inventory.md`
  - 用于维护测试资产覆盖范围与回归检查项。

## 5. Round 测试资料

| Round | 主题 | 路径 | 状态 |
|-------|------|------|------|
| R1 | 日志 + 安全 + 审计框架 | `R1-日志安全审计框架/` | ✅ 完成 |
| R2 | 通用验证 + RBAC 框架 | `R2-通用验证_RBAC框架/` | ✅ 完成 |
| R3 | Auth + Workspace + Nav | `R3-Auth_Workspace_Nav/` | ✅ 已归档，100/100 闭环 |
| R4 | Quota + Connect + MCP/CLI | 由主计划进入准备阶段 | 🟡 Ready to start |

### 5.1 当前目录现状说明

当前测试资料目录以 Round 归档目录为主，`R1-日志安全审计框架/`、`R2-通用验证_RBAC框架/`、`R3-Auth_Workspace_Nav/` 为正式归档路径。

其中 R3 已完成代码、测试、覆盖率归档与验收文档闭环，可作为 R4 的直接前置完成依据；R4 当前处于准备启动状态，待形成首批测试资料后再新增对应归档目录。

以下 `standards/`、`templates/`、`inventories/`、`reports/` 结构属于目标规范形态，当前仓库尚未完整落地，因此引用这些路径时应先核对文件是否实际存在。

- `standards/test-case-process.md`
  - 测试流程规范主文档；
- `templates/test-execution-record-template.md`
  - 单次测试执行记录模板；
- `templates/pr-test-plan-template.md`
  - Pull Request 测试计划模板；
- `templates/commit-test-note-template.md`
  - 提交测试说明模板；
- `inventories/module-test-inventory.md`
  - 模块测试资产清单；
- `inventories/release-regression-checklist.md`
  - 发布前或合并前的回归检查清单；
- `reports/`
  - 用于存放按场景、按日期或按模块归档的正式测试报告。

## 6. 命名约定
为保证项目结构统一、跨团队协作清晰：
- 文件夹名称统一使用英文，并采用 kebab-case 风格；
- 文件名统一使用英文，便于跨平台、脚本处理与团队协作；
- R1/R2 等已归档历史目录可保留原中文名称，新增目录继续按英文 kebab-case；
- 文件内容统一使用中文，便于团队内部阅读与维护；
- 构建过程产生的 `target` 目录属于临时目录，不作为正式测试资料存放位置。

推荐命名示例：
- `test-case-process.md`
- `auth-login-test-report.md`
- `release-regression-checklist.md`

## 7. 维护原则
新增或更新本目录中的测试资料时，应遵循以下原则：
- 用语尽量正式、简洁、可审计；
- 明确记录执行范围、结果摘要与归档路径；
- 不在 Markdown 文档中粘贴大段原始日志或 XML 内容；
- 以本目录中的归档报告作为正式引用对象，而不是引用 `target` 下的临时产物；
- 测试规范类文档优先放入 `standards/`，正式测试结果优先放入 `reports/`；
- 通用复用文档优先放入 `templates/`，资产与检查类文档优先放入 `inventories/`。

## 8. 相关入口文档
为便于开发阶段就近查看，`axiqra-code` 目录下保留一份本地入口文档：
- `axiqra-code/test-case-guide.md`

该文件用于快捷访问正式测试资料，并指向本目录中的标准文档。
