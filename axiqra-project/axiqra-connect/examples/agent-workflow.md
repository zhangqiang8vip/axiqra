# Axiqra Agent 工作流示例

本文档展示 Agent 如何集成 Axiqra 实现工程记忆闭环。

## 核心工作流

```
┌─────────────────────────────────────────────────────────────┐
│                      Axiqra Agent 工作流                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 接收任务                                              │
│     ↓                                                       │
│  2. axiqra search → 搜索历史方案                          │
│     ↓                                                       │
│  3. 评估适配性 → 高风险需用户确认                          │
│     ↓                                                       │
│  4. 执行任务                                              │
│     ↓                                                       │
│  5. axiqra trace submit → 提交工程轨迹                      │
│     ↓                                                       │
│  6. 用户确认 → 生成 Project Case                           │
│     ↓                                                       │
│  7. axiqra feedback → 提交反馈                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 场景示例

### 场景 1：Cursor Agent 修复 Bug

```
用户: "帮我修复这个 Redis 连接超时问题"

Agent:
1. axiqra search_before_act({
     task_goal: "修复 Redis 连接超时",
     tech_stack: "Spring Boot 3.x + Lettuce",
     risk_hint: "production"
   })
   → 返回 2 个 Solution

2. axiqra get_solution({ solution_id: "SOL-xxx" })
   → 返回配置参数和验证步骤

3. 检查当前配置
   → 发现连接超时配置过短

4. 修复配置
   → 应用 Solution 中的配置建议

5. 验证修复
   → 运行测试确认问题解决

6. axiqra submit_trace({
     trace_payload: {
       task_goal: "修复 Redis 连接超时",
       forward_path: [...],
       decision_path: [...],
       evidence_refs: ["test-output.log"]
     }
   })
   → 轨迹已提交

7. 等待用户确认

8. axiqra submit_feedback({
     invocation_id: "INV-xxx",
     feedback_type: "worked",
     evidence_refs: ["test-result.log"]
   })
   → 反馈已记录
```

### 场景 2：遇到无命中问题

```
用户: "我们想在 Kubernetes 上部署有状态服务，但找不到合适的方案"

Agent:
1. axiqra search_before_act({
     task_goal: "K8s 有状态服务部署",
     tech_stack: "K8s + PostgreSQL"
   })
   → 返回 1 个 Solution，但不完全匹配

2. 自己研究方案
   → 分析 K8s Operator vs StatefulSet
   → 决定使用 Operator 模式

3. 实施并验证

4. axiqra trace submit({
     trace_payload: {
       task_goal: "K8s 有状态服务部署",
       forward_path: [...],
       decision_path: [
         { decision: "选择 Operator 模式", reason: "需要自动故障恢复" }
       ]
     },
     idempotency_key: "k8s-stateful-xxx"
   })
   → 新轨迹已提交

5. axiqra create_seed({
     task_goal: "Kubernetes 有状态服务部署方案",
     coverage_gap: "缺少生产级 Operator 方案",
     evidence_hint: "需要 Operator 开发和 Helm 集成"
   })
   → 候选 Seed 已创建
```

### 场景 3：技术选型决策

```
用户: "我们要选型消息队列，Kafka vs RabbitMQ vs Redis Streams"

Agent:
1. axiqra search_before_act({
     task_goal: "消息队列选型",
     tech_stack: "微服务架构",
     context: "日活 10 万，延迟要求 < 100ms"
   })
   → 返回多个 Case 和 Solution

2. axiqra get_solution({ solution_id: "SOL-kafka" })
   → 获取 Kafka 适用场景

3. axiqra get_solution({ solution_id: "SOL-rabbitmq" })
   → 获取 RabbitMQ 适用场景

4. 分析对比
   → 根据团队规模、技术栈、运维能力推荐

5. 给出建议
   → 推荐 Redis Streams（团队小，简单场景）
   → 保留 Kafka 备选（未来扩展）

6. 提交选型决策轨迹

7. 标记这个场景需要更多 Case
   → 创建 Candidate Seed
```

## 轨迹回传质量要求

### 完整轨迹

```json
{
  "trace": {
    "session_id": "agent-xxx-001",
    "task_goal": "修复 Redis 连接池耗尽",
    "environment": {
      "tech_stack": "Spring Boot 3.2 + Lettuce",
      "version": "JDK 17",
      "os": "Linux",
      "infrastructure": "K8s + AWS EKS"
    },
    "forward_path": [
      {
        "step": 1,
        "action": "分析连接池配置",
        "reason": "连接数持续上涨可能是池配置问题",
        "result": "max-idle=10, max-total=50"
      },
      {
        "step": 2,
        "action": "检查连接释放逻辑",
        "reason": "可能是连接未正确释放",
        "result": "发现异步任务中未关闭连接"
      }
    ],
    "reverse_path": [
      {
        "attempted": "增加 max-total",
        "reason_failed": "只是延迟问题，未解决根本原因",
        "evidence": "连接数继续上涨"
      }
    ],
    "decision_path": [
      {
        "decision": "修复连接释放逻辑而非扩大池",
        "options_considered": ["扩大连接池", "添加连接监控"],
        "selected": "修复释放逻辑",
        "reason": "治本优于治标"
      }
    ],
    "evidence_refs": [
      "logs/redis-connection.log",
      "test/connection-pool-test.js"
    ],
    "outcome": "success"
  }
}
```

### 脱敏检查

❌ 禁止包含：
- `<API_KEY_REDACTED>`
- `<INTERNAL_IP>`
- `<CUSTOMER_NAME>`
- 真实 AWS 账号/密钥

✅ 必须包含：
- 技术栈和版本
- 配置参数（不含密钥）
- 决策理由
- 验证证据

## 反馈类型选择

| 场景 | 反馈类型 | 说明 |
|------|---------|------|
| 方案完全解决 | `worked` | 有效 |
| 方案部分有效 | `partial` | 需补充 |
| 方案不适用 | `failed` | 触发复核 |
| 场景不匹配 | `not_applicable` | 更新边界 |

### worked 反馈

```json
{
  "invocation_id": "INV-xxx",
  "feedback_type": "worked",
  "evidence_refs": [
    "test-output.log",
    "metrics-improvement.png"
  ],
  "notes": "配置生效，P95 延迟从 500ms 降到 50ms"
}
```

### failed 反馈

```json
{
  "invocation_id": "INV-yyy",
  "feedback_type": "failed",
  "failure_reason": "Spring Boot 2.7 不支持新配置语法",
  "environment_context": {
    "spring_boot_version": "2.7.18"
  },
  "alternative_found": "使用了兼容旧语法的配置"
}
```

### partial 反馈

```json
{
  "invocation_id": "INV-zzz",
  "feedback_type": "partial",
  "what_worked": "连接池配置生效",
  "what_failed": "超时设置仍不足",
  "suggestion": "需要同时调整 readTimeout"
}
```

## 与其他规则的交互

```
search-before-act → 执行 → trace-submit → feedback
       ↑                              ↓
       ← ← ← ← ← ← ← ← ← ← ← ← ← ←
```

- **search-before-act** 告诉你什么时候搜
- **trace-submit** 告诉你回传什么
- **feedback** 告诉平台结果如何影响方案质量

## CLI 快速参考

```bash
# 搜索
axiqra search "Spring Boot Redis 配置"

# 获取详情
axiqra solution get SOL-xxx

# 提交轨迹
axiqra trace submit ./trace.json

# 反馈
axiqra feedback INV-xxx worked
axiqra feedback INV-xxx failed --reason "版本不匹配"

# 诊断
axiqra doctor

# 配额
axiqra quota
```

## 最佳实践

1. **任务前必搜**：不要从零开始，先看历史方案
2. **完整记录**：轨迹比结论更重要
3. **诚实反馈**：worked/failed 都要报
4. **脱敏优先**：敏感信息用占位符
5. **用户确认**：高风险操作需要确认
