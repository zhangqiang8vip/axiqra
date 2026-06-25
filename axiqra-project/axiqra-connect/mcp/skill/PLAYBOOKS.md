# Axiqra Playbooks

本文档描述 Axiqra 的业务流程和操作指南。

## 目录

- [搜索历史方案](#搜索历史方案)
- [记录开发步骤](#记录开发步骤)
- [提交轨迹沉淀](#提交轨迹沉淀)
- [方案反馈](#方案反馈)
- [创建候选 Seed](#创建候选-seed)
- [工作候选巡航](#工作候选巡航)

---

## 搜索历史方案

### 场景

当用户提出一个新任务时，Agent 应该先搜索是否有相似的历史方案。

### 流程

```
1. 接收用户任务
2. 构造搜索请求
3. 调用 POST /search/before-act
4. 分析搜索结果
5. 如有匹配方案，展示摘要
6. 如无匹配方案，继续开发
```

### 请求示例

```bash
node {AXIQRA_SKILL_DIR}/scripts/rest_request.js POST /search/before-act \
  --file {AXIQRA_SKILL_DIR}/memory/sessions/{SESSION_ID}/request-search.json
```

**request-search.json**：
```json
{
  "query": "用户登录功能实现",
  "tech_stack": "Spring Boot + MyBatis",
  "environment": "production",
  "risk_hint": "staging",
  "max_results": 5
}
```

### 结果处理

**有匹配方案**：
- 展示方案摘要（标题、匹配度、验证等级）
- 询问用户是否复用
- 如复用，执行方案并提交反馈

**无匹配方案**：
- 继续开发
- 开发完成后创建 Seed 或提交轨迹

### 自检清单

- [ ] query 参数清晰描述任务目标
- [ ] tech_stack 正确填写技术栈
- [ ] risk_hint 标注环境风险
- [ ] 结果正确展示给用户

---

## 记录开发步骤

### 场景

在长时间开发任务中，需要记录中间步骤，以便后续沉淀或回滚。

### 流程

```
1. 开始开发任务
2. 每个关键步骤后调用 POST /traces
3. 记录步骤、决策、证据
4. 任务完成后提交轨迹
```

### 请求示例

```bash
node {AXIQRA_SKILL_DIR}/scripts/rest_request.js POST /traces \
  --file {AXIQRA_SKILL_DIR}/memory/sessions/{SESSION_ID}/request-draft.json
```

**request-draft.json**：
```json
{
  "session_id": "session-xxx",
  "task_goal": "实现用户登录功能",
  "environment": {
    "tech_stack": "Spring Boot",
    "version": "3.0",
    "os": "Linux"
  },
  "forward_path": [
    {
      "step": 1,
      "action": "创建 UserController",
      "status": "success",
      "timestamp": "2026-06-25T10:00:00Z"
    },
    {
      "step": 2,
      "action": "添加 JWT 认证",
      "status": "success",
      "timestamp": "2026-06-25T10:05:00Z"
    }
  ],
  "reverse_path": [],
  "decision_path": [
    {
      "step": 1,
      "decision": "选择 JWT 而非 Session",
      "reason": "适合微服务架构"
    }
  ],
  "evidence_refs": [
    "file:///path/to/UserController.java",
    "file:///path/to/AuthService.java"
  ],
  "outcome": "success"
}
```

### 证据引用规范

证据引用支持以下格式：

| 类型 | 格式 | 示例 |
|------|------|------|
| 本地文件 | `file://` | `file:///path/to/file.java` |
| URL | `http://` / `https://` | `https://ci.example.com/build/123` |
| Git | `git://` | `git://commit/abc123` |

### 自检清单

- [ ] forward_path 记录所有关键步骤
- [ ] decision_path 记录重要决策
- [ ] evidence_refs 包含可验证的证据
- [ ] outcome 准确反映任务结果

---

## 提交轨迹沉淀

### 场景

任务完成后，将开发过程沉淀为可复用的方案。

### 流程

```
1. 确认任务完成
2. 整理轨迹数据
3. 确认轨迹（POST /traces/{id}/confirm）
4. 提交轨迹（POST /traces/{id}/submit）
5. 可选：从轨迹创建 Seed
```

### 请求示例

**确认轨迹**：
```bash
node {AXIQRA_SKILL_DIR}/scripts/rest_request.js POST /traces/{traceId}/confirm \
  --file {AXIQRA_SKILL_DIR}/memory/sessions/{SESSION_ID}/request-confirm.json
```

**request-confirm.json**：
```json
{
  "outcome": "success",
  "notes": "登录功能实现完成，所有测试通过"
}
```

**提交轨迹**：
```bash
node {AXIQRA_SKILL_DIR}/scripts/rest_request.js POST /traces/{traceId}/submit
```

### 从轨迹创建方案

轨迹提交后，可以从轨迹生成正式的工程方案：

```bash
node {AXIQRA_SKILL_DIR}/scripts/rest_request.js POST /solutions/from-project-case \
  --file {AXIQRA_SKILL_DIR}/memory/sessions/{SESSION_ID}/request-solution.json
```

**request-solution.json**：
```json
{
  "project_case_id": "pc-xxx",
  "title": "Spring Boot 用户认证方案",
  "description": "基于轨迹沉淀的详细实现方案"
}
```

### 自检清单

- [ ] 轨迹状态为 confirmed
- [ ] 证据引用完整
- [ ] outcome 与实际结果一致
- [ ] 方案标题清晰描述问题解决

---

## 方案反馈

### 场景

使用历史方案后，需要反馈方案的有效性。

### 流程

```
1. 复用历史方案
2. 执行并验证
3. 提交反馈（POST /v1/feedbacks）
4. 如方案有效，提高其验证等级
5. 如方案无效，标记并说明原因
```

### 请求示例

**有效反馈**：
```bash
node {AXIQRA_SKILL_DIR}/scripts/rest_request.js POST /v1/feedbacks \
  --file {AXIQRA_SKILL_DIR}/memory/sessions/{SESSION_ID}/request-feedback.json
```

**request-feedback.json**：
```json
{
  "target_type": "solution",
  "target_id": "sol-xxx",
  "feedback_type": "worked",
  "evidence_refs": [
    "file:///path/to/test-result.java"
  ],
  "notes": "方案完全有效，登录功能正常工作"
}
```

**部分有效反馈**：
```json
{
  "target_type": "solution",
  "target_id": "sol-xxx",
  "feedback_type": "partial",
  "evidence_refs": [],
  "notes": "基础部分有效，但 token 刷新逻辑需要调整",
  "partial_details": {
    "valid_parts": ["用户认证", "密码验证"],
    "invalid_parts": ["token 刷新"],
    "suggestion": "需要添加 refresh_token 接口"
  },
  "failure_reason": "token 刷新逻辑与现有架构不兼容"
}
```

**无效反馈**：
```json
{
  "target_type": "solution",
  "target_id": "sol-xxx",
  "feedback_type": "failed",
  "evidence_refs": [],
  "notes": "方案无法使用",
  "failure_reason": "技术栈不匹配，该方案基于 Spring Boot 2.x，当前项目使用 3.x"
}
```

### 自检清单

- [ ] feedback_type 准确反映结果
- [ ] partial_details 提供有效的改进建议
- [ ] failure_reason 清晰说明失败原因
- [ ] evidence_refs 提供可验证的证据

---

## 创建候选 Seed

### 场景

当没有找到合适的历史方案时，可以创建候选 Seed 来标记这个需求，以便后续补充方案。

### 流程

```
1. 搜索历史方案
2. 无匹配结果
3. 创建 Seed（POST /seeds）
4. 开发完成后从 Seed 创建正式方案
```

### 请求示例

```bash
node {AXIQRA_SKILL_DIR}/scripts/rest_request.js POST /seeds \
  --file {AXIQRA_SKILL_DIR}/memory/sessions/{SESSION_ID}/request-seed.json
```

**request-seed.json**：
```json
{
  "task_goal": "实现微信小程序用户登录",
  "coverage_gap": "当前方案库缺少移动端小程序登录方案",
  "evidence_hint": "参考微信官方登录流程文档"
}
```

### Seed 生命周期

```
DRAFT → CANDIDATE → APPROVED → PUBLISHED
                 ↓
              REJECTED
```

### 自检清单

- [ ] task_goal 清晰描述需求
- [ ] coverage_gap 说明为什么需要这个 Seed
- [ ] evidence_hint 提供可能的实现线索

---

## 工作候选巡航

### 场景

定期检查用户的工作候选，包括待处理任务、方案更新等。

### 流程

```
1. 执行巡航脚本
2. 检查方案更新
3. 检查反馈统计
4. 生成推荐摘要
5. 通知用户
```

### 请求示例

```bash
node {AXIQRA_SKILL_DIR}/scripts/cruise_tick.js
```

### 巡航输出

巡航脚本会输出结构化的 JSON 供 Agent 解析：

```json
{
  "status": "ok",
  "workspace_id": "ws-xxx",
  "checks": {
    "solutions_updated": 2,
    "feedbacks_received": 5,
    "seeds_pending": 3
  },
  "recommendations": [
    {
      "type": "solution_update",
      "solution_id": "sol-xxx",
      "reason": "方案收到多个 partial 反馈"
    }
  ]
}
```

### 推荐类型

| 类型 | 说明 |
|------|------|
| solution_update | 方案有新的反馈或更新 |
| seed_reminder | Seed 长期未处理 |
| feedback_summary | 反馈统计变化 |
| new_capability | 发现新的可复用方案 |

### 自检清单

- [ ] 巡航脚本定期执行
- [ ] 推荐摘要正确展示
- [ ] 需要用户确认的动作正确提示

---

## 常见业务流程

### 完整任务流程

```
用户: "实现一个用户注册功能"
    ↓
Agent: 搜索历史方案
    ↓
┌─────────────────┬─────────────────┐
│ 有方案           │ 无方案          │
│ ↓               │ ↓               │
│ 复用并反馈       │ 开发并记录       │
│ ↓               │ ↓               │
│ 完成任务        │ 创建方案         │
│ ↓               │ ↓               │
│ 提交反馈        │ 提交轨迹         │
└─────────────────┴─────────────────┘
```

### 会话隔离文件命名规范

| 用途 | 文件名 |
|------|--------|
| 方案搜索 | `request-search.json` |
| 草稿记录 | `request-draft.json` |
| 轨迹确认 | `request-confirm.json` |
| 轨迹提交 | `request-trace.json` |
| 方案创建 | `request-solution.json` |
| 反馈提交 | `request-feedback.json` |
| Seed 创建 | `request-seed.json` |
