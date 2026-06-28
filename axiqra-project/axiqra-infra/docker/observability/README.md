# Axiqra 可观测性栈 (Observability Stack)

Axiqra 后端的 Prometheus + Grafana 可观测性基础设施。
与后端基础依赖（数据库 / 缓存 / 对象存储）**完全解耦**，按需启动。

## 启动

### 1) 启动 Axiqra 后端（必需）
后端进程需要独立启动，并暴露 actuator 端口：

```powershell
$env:JAVA_HOME = "D:\jdk-17"
$env:PATH = "D:\jdk-17\bin;D:\apache-maven-3.9.9\bin;$env:PATH"
cd E:\ProjectMyNew\axiqra\axiqra-project\axiqra-code
mvn -B -pl axiqra-start -am spring-boot:run
```

启动后应可在 http://localhost:9090/actuator/health 看到 health=UP。

### 2) 启动 Prometheus + Grafana

```powershell
cd E:\ProjectMyNew\axiqra\axiqra-project\axiqra-infra
docker compose -f docker-compose.observability.yml up -d
```

| 服务 | 端口 | 凭据 |
|------|------|------|
| Prometheus UI | http://localhost:9091 | 无 |
| Grafana UI | http://localhost:3001 | admin / admin（可通过 .env 修改） |

### 3) 端到端验证

```powershell
powershell -ExecutionPolicy Bypass -File E:\ProjectMyNew\axiqra\axiqra-project\axiqra-infra\docker\observability\verify-metrics.ps1
```

应输出 `✓ 全部 16 个指标 + Prometheus + Grafana 验证通过`。

## 已暴露的 Axiqra 业务指标

| 指标名 | 类型 | 标签 | 描述 |
|--------|------|------|------|
| `axiqra_search_requests_total` | Counter | kind, result | Search Before Act 调用数 |
| `axiqra_search_duration_seconds` | Timer (Histogram) | kind | Search 耗时（含 p95/p99） |
| `axiqra_solution_invocations_total` | Counter | result | Solution 拉起数 |
| `axiqra_review_decisions_total` | Counter | decision | 审核决策数（approved/rejected/quarantined） |
| `axiqra_feedback_submissions_total` | Counter | type | 反馈提交数 |
| `axiqra_workspace_created_total` | Counter | type | Workspace 创建数 |
| `axiqra_trace_drafts_created_total` | Counter | risk_level | Trace 草稿创建数 |
| `axiqra_trace_submissions_total` | Counter | path, result | Trace 提交数 |
| `axiqra_project_case_created_total` | Counter | visibility | Project Case 创建数 |
| `axiqra_device_auth_codes_issued_total` | Counter | — | 设备授权码签发数 |
| `axiqra_device_auth_token_refreshes_total` | Counter | result | 设备 token 刷新数（issued/rotated/revoked/rejected） |

外加 Spring Boot Actuator 默认暴露的 JVM / HTTP / GC 等基础指标。

## Grafana Dashboard

启动 observability 栈后会自动加载 `axiqra-s1-business-metrics` dashboard，包含：

1. **Search Before Act** 调用速率（按 kind 拆 keyword / vector / hybrid）
2. **Search Latency P95/P99**（直方图）
3. **Solution Invocations**（success / failed / duplicate）
4. **Review Decisions**（approved / rejected / quarantined）
5. **Feedback Submissions**（按 type）
6. **Workspace Created**（personal / team / enterprise）
7. **Trace Submissions**（direct / needs_review）
8. **Project Case Created**（按 visibility_scope）
9. **Device Auth Token Refreshes**（issued / rotated / revoked / rejected）
10. **JVM Heap Used** + **HTTP P99 响应时间**

访问入口：http://localhost:3001/d/axiqra-s1-business-metrics/

## 文件结构

```
axiqra-infra/
├── docker-compose.observability.yml           # Prometheus + Grafana compose
├── docker/observability/
│   ├── prometheus/
│   │   └── prometheus.yml                     # 抓取 axiqra-core /actuator/prometheus
│   ├── grafana/
│   │   ├── provisioning/
│   │   │   ├── datasources/prometheus.yml     # 自动注册 Prometheus 数据源
│   │   │   └── dashboards/axiqra.yml          # dashboard 自动加载配置
│   │   └── dashboards/
│   │       └── axiqra-s1-business.json        # 11 panel 业务 dashboard
│   └── verify-metrics.ps1                     # 端到端验证脚本
└── .env.example                                # 含 GRAFANA_ADMIN_USER/PASSWORD
```

## 常见问题

**Q: Grafana 显示 "No data"？**
A: 等待 15 秒（scrape_interval）后刷新 dashboard；或检查 Prometheus
   http://localhost:9091/targets 看 axiqra-core 是否 health=up。

**Q: 容器内访问不到 host.docker.internal？**
A: Windows Docker Desktop 默认支持；Linux 需在 compose 中改用 `network_mode: host`
   或添加 `extra_hosts: ["host.docker.internal:host-gateway"]`（已包含）。

**Q: actuator 端口不是 9090？**
A: 修改 `application.yml` 中 `management.server.port`，同步修改 `prometheus.yml` 里的 target。
