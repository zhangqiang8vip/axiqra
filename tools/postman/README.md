# Axiqra Postman 业务全流程

导入以下文件：

- `axiqra-public-business-flow.postman_collection.json`
- `axiqra-mvp-aiagent-flow.postman_collection.json`
- `axiqra-public-business-flow.postman_environment.json`

选择 `Axiqra 本地 API` 环境。默认 `baseurl` 是：

```text
http://localhost:8080/api
```

## MVP：个人空间 AI Agent 接入闭环

优先运行 `Axiqra MVP：个人空间 AI Agent 接入到工程方案闭环`。这套流程验证当前 S1 MVP 主链路：

1. `00 初始化与连通`
   初始化本轮变量，检查健康接口和 OpenAPI 文档。
2. `01 个人用户与个人空间`
   注册个人用户，创建 `personal` 工作空间，再创建第二个 `team` 工作空间并列表确认同一用户可同时存在于多个空间；后续发布仍使用个人空间，团队空间不是前置条件。
3. `02 AI Agent 接入与任务前搜索`
   使用 `toolType=codex` 跑 Connect Doctor、创建接入会话，并执行 `search_before_act`。
4. `03 工程轨迹回传到 Project Case`
   AI Agent 提交 Trace，用户确认后提交，再从 Trace 生成 Project Case。
5. `04 发布个人工程方案 Solution`
   直接从个人 Project Case 生成 `workspace` 可见的 Solution，并用任务前搜索命中刚发布的方案。
6. `05 AI 调用与反馈`
   上报 Invocation，提交 Feedback，并验证 Solution 反馈统计。

命令行复测：

```powershell
npm config set proxy http://127.0.0.1:7897
npm config set https-proxy http://127.0.0.1:7897
npx --yes newman run tools\postman\axiqra-mvp-aiagent-flow.postman_collection.json -e tools\postman\axiqra-public-business-flow.postman_environment.json --env-var baseurl=http://localhost:8080/api --env-var baseUrl=http://localhost:8080/api
```

## 公开与团队业务全流程

需要覆盖公开接口、匿名边界、团队成员、审核/贡献边界时，再运行 `Axiqra Postman 业务全流程`。建议直接按 collection 顺序运行：

1. `00 基础连通与文档`
   初始化本轮变量，检查健康接口和 OpenAPI 文档。
2. `01 匿名公开业务流程（空库也应稳定）`
   检查公开案例、公开方案、排行榜和公开搜索。空库时列表允许为空，详情允许 `404`，但不能被登录拦截或返回 `500`。
3. `02 匿名保护边界校验`
   检查仍需登录的接口在匿名状态下返回 `401`。
4. `03 认证与用户资料`
   注册主用户、保存 `authToken`、验证用户资料、导航和默认 scope。
5. `04 工作空间与成员造数`
   创建团队空间、注册协作者、添加成员、更新角色。
6. `05 Trace 与 Project Case 主链路`
   创建 Trace、确认、提交，再从 Trace 创建 Project Case。
7. `06 Connect、Invocation 与 Feedback`
   跑 Connect doctor、创建会话、上报 Invocation、提交 Feedback、查统计。
8. `07 搜索、策略、审核与贡献边界`
   验证登录态搜索、策略评估，以及普通用户访问审核/贡献高权限接口会被拒绝。
9. `08 收尾与清理`
   移除协作者并退出登录。

命令行复测：

```powershell
npm config set proxy http://127.0.0.1:7897
npm config set https-proxy http://127.0.0.1:7897
npx --yes newman run tools\postman\axiqra-public-business-flow.postman_collection.json -e tools\postman\axiqra-public-business-flow.postman_environment.json --env-var baseurl=http://localhost:8080/api --env-var baseUrl=http://localhost:8080/api
```

空库限制：

- 当前 API 没有公开的 `Solution` 创建接口，所以公开 Solution 只能测空列表/404 边界，不能纯靠 Postman 造公开方案数据。
- 当前 API 没有公开的 `Authorization` 创建接口，所以 Public Case 发布申请默认会因为缺少有效 `authorizationId` 返回明确业务错误。以后有授权种子后，把环境变量 `authorizationId` 改成有效值即可继续验证发布链路。

Sa-Token 当前配置为 `token-name: Authorization`，没有 token 前缀，所以 collection 发送原始 token：

```text
Authorization: {{authToken}}
```
