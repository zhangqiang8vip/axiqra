# TASK 2 - Hot Reload / Local Development Options

目标：为 Axiqra 增加框架优先、配置优先的本地开发热部署方式。当前已落地最小实现：`axiqra-start` 引入 Spring Boot DevTools；前端 Vue/Vite 项目尚不存在，因此 Vite HMR 作为方案而非本轮代码变更。

## 一、官方资料核查

| 技术 | 官方资料 | 当前项目版本 | 是否适用 | 备注 |
| -- | -- | -- | -- | -- |
| FEAT-HOT-001 Spring Boot DevTools | Spring Boot Reference - Developer Tools, https://docs.spring.io/spring-boot/reference/using/devtools.html | Spring Boot 3.5.14 | 是 | 已在 `axiqra-start/pom.xml` 增加 `spring-boot-devtools` |
| FEAT-HOT-002 Spring Boot Restart / LiveReload | 同上 | 3.5.14 | 是 | DevTools 默认提供 restart 与 LiveReload server |
| FEAT-HOT-003 Docker Compose Watch | Docker Compose Watch, https://docs.docker.com/compose/how-tos/file-watch/ | Compose v2 | 部分适用 | 当前 `axiqra_core` 服务仍注释，暂不直接加 watch |
| FEAT-HOT-004 Docker bind mounts | Docker bind mounts, https://docs.docker.com/engine/storage/bind-mounts/ | Docker | 是 | 当前依赖服务已用 named volumes 保存数据 |
| FEAT-HOT-005 Vite HMR | Vite Guide - Features / HMR, https://vite.dev/guide/features.html | 当前无 Vue/Vite package.json | 当前不适用 | 未来前端可直接使用 Vite dev server |
| FEAT-HOT-006 Vite proxy/env | Vite server options / env, https://vite.dev/config/server-options.html, https://vite.dev/guide/env-and-mode.html | 当前无前端 | 规划项 | 未来代理 `/api` 到 8080 |
| FEAT-HOT-007 Maven | Maven Plugins, https://maven.apache.org/plugins/ | Maven 3.9.9 | 辅助适用 | 编译触发由 IDE 或 Maven 完成，重启由 DevTools 完成 |
| FEAT-HOT-008 JRebel | JRebel 官方文档, https://www.jrebel.com/products/jrebel | 未引入 | 可选商业方案 | 不默认引入 |

## 二、热部署方案对比

| 方案 | 后端热部署 | 前端热更新 | 依赖服务 | 优点 | 缺点 | 推荐场景 |
| -- | -- | -- | -- | -- | -- | -- |
| A：本机运行前后端，依赖服务走 Docker Compose | Spring Boot DevTools + IDE/Maven 编译触发 restart | 未来 Vue/Vite 使用 HMR | `axiqra-infra/docker-compose.yml` 启动 DB/Redis/RabbitMQ/MinIO | 启动快，调试方便，对当前代码侵入最低 | 需要本机 JDK/Maven；前端项目尚未存在 | 当前默认方案 |
| B：前后端容器化，Docker Compose watch / bind mount | `docker compose watch` rebuild 或 sync+restart | 容器内 Vite dev server | 全部容器化 | 环境一致性好 | 当前后端容器服务未启用，直接改动风险中等 | 团队统一环境后采用 |
| C：IDE 模式，DevTools + Vite HMR | IntelliJ/VS Code 自动编译 + DevTools | Vite HMR | Docker 或本地依赖 | 断点调试体验最好 | IDE 配置差异大 | 深度开发和调试 |

## 三、推荐方案

推荐方案名称：A 方案，本机后端 DevTools + Docker 依赖服务。

适用开发者：后端或全栈开发者，需要快速改 Controller/Service 并立即验证接口行为。

启动命令：

```powershell
cd E:\ProjectMyNew\axiqra\axiqra-project\axiqra-infra
docker compose up -d

cd E:\ProjectMyNew\axiqra\axiqra-project\axiqra-code
mvn spring-boot:run -pl axiqra-start -am -Dspring-boot.run.profiles=dev
```

必要配置文件：

| 文件 | 作用 |
| -- | -- |
| `axiqra-project/axiqra-code/axiqra-start/pom.xml` | TASK-HOT-001：新增 `spring-boot-devtools` |
| `axiqra-project/axiqra-code/axiqra-start/src/main/resources/application-dev.yml` | TASK-HOT-002：开发 profile 的管理端点、CORS、Sa-Token、依赖配置 |
| `axiqra-project/axiqra-infra/docker-compose.yml` | TASK-HOT-003：依赖服务容器化 |

不建议默认采用：

| 方案 | 原因 | 后续条件 |
| -- | -- | -- |
| JRebel | 商业方案，不符合默认开源/低成本路线 | 团队购买许可后再评估 |
| 直接容器化后端 watch | 当前 `axiqra_core` 在 compose 中注释，build context 与运行时配置需先整理 | 完成 TASK-DEV-008 后再实施 |
| 手写热部署脚本 / 文件监听器 | Spring Boot DevTools 与 Docker Compose Watch 已覆盖需求 | 仅在框架方案不可用时再讨论 |

## 四、需要修改的文件清单

| 文件 | 修改内容 | 是否框架配置 | 风险 |
| -- | -- | -- | -- |
| `axiqra-start/pom.xml` | 新增 `spring-boot-devtools` runtime optional 依赖 | 是 | 低，开发期依赖 |
| `application-dev.yml` | 保持 dev profile 的管理端点、CORS、Sa-Token 等开发配置 | 是 | 中，配置重复需后续收敛 |
| `docker-compose.yml` | 暂不直接修改 watch；仅记录评估 | 是 | 中，后端服务仍注释 |

## 五、验收标准

| 编号 | Given | When | Then |
| -- | -- | -- | -- |
| AC-HOT-001 | 开发者用 `spring-boot:run` 启动 dev profile | 修改 Controller 或 Service 并触发编译 | 服务由 DevTools 自动 restart |
| AC-HOT-002 | 未来 Vue/Vite dev server 已启动 | 修改 Vue 页面或组件 | 浏览器 HMR 更新 |
| AC-HOT-003 | Docker 依赖服务已启动 | 重启后端 | PostgreSQL、Redis、MinIO 数据不丢失 |
| AC-HOT-004 | 热部署失败 | 查看控制台日志 | 能看到 DevTools 或 Spring Boot 明确错误 |

## 六、测试用例

| 编号 | 前置 | 操作 | 期望 |
| -- | -- | -- | -- |
| TC-HOT-001 | 后端 dev profile 正常启动 | 修改 `ConnectController` 并编译 | 控制台出现 restart，接口返回新行为 |
| TC-HOT-002 | 后端 dev profile 正常启动 | 修改 `application-dev.yml` | Spring Boot restart 或提示配置错误 |
| TC-HOT-003 | 依赖服务容器已启动 | 重启后端 3 次 | 数据卷不丢失，健康检查可恢复 |
| TC-HOT-004 | 未来前端已存在 | 修改 Vue 组件 | Vite HMR 生效且不刷新全页 |
