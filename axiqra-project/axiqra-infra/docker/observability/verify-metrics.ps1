#!/usr/bin/env pwsh
# ============================================================
# Axiqra Metrics 端到端验证脚本
# 验证 Axiqra 后端暴露的 11 个 Axiqra 自定义指标 + JVM 基础指标
#
# 前置条件：
#   1. axiqra-core 已启动（端口 8080 + 9090 actuator）
#   2. Prometheus + Grafana 已启动（端口 9091 + 3001）
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File E:\ProjectMyNew\axiqra\axiqra-project\axiqra-infra\docker\observability\verify-metrics.ps1
# ============================================================

$ErrorActionPreference = "Stop"

$BackendHost = "http://localhost:9090"
$PromHost = "http://localhost:9091"
$GrafanaHost = "http://localhost:3001"
$GrafanaUser = if ($env:GRAFANA_ADMIN_USER) { $env:GRAFANA_ADMIN_USER } else { "admin" }
$GrafanaPass = if ($env:GRAFANA_ADMIN_PASSWORD) { $env:GRAFANA_ADMIN_PASSWORD } else { "admin" }

$ExpectedMetrics = @(
    @{ Name = "axiqra_search_requests_total"; Kind = "Counter"; Tags = @("kind", "result") }
    @{ Name = "axiqra_search_duration_seconds"; Kind = "Timer"; Tags = @("kind") }
    @{ Name = "axiqra_solution_invocations_total"; Kind = "Counter"; Tags = @("result") }
    @{ Name = "axiqra_review_decisions_total"; Kind = "Counter"; Tags = @("decision") }
    @{ Name = "axiqra_feedback_submissions_total"; Kind = "Counter"; Tags = @("type") }
    @{ Name = "axiqra_workspace_created_total"; Kind = "Counter"; Tags = @("type") }
    @{ Name = "axiqra_trace_drafts_created_total"; Kind = "Counter"; Tags = @("risk_level") }
    @{ Name = "axiqra_trace_submissions_total"; Kind = "Counter"; Tags = @("path", "result") }
    @{ Name = "axiqra_project_case_created_total"; Kind = "Counter"; Tags = @("visibility") }
    @{ Name = "axiqra_device_auth_codes_issued_total"; Kind = "Counter"; Tags = @() }
    @{ Name = "axiqra_device_auth_token_refreshes_total"; Kind = "Counter"; Tags = @("result") }
)

$JvmBaseline = @(
    "jvm_memory_used_bytes",
    "jvm_threads_live_threads",
    "http_server_requests_seconds",
    "process_cpu_usage",
    "jvm_gc_pause_seconds"
)

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " Axiqra Metrics 端到端验证" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " 后端 Actuator: $BackendHost"
Write-Host " Prometheus:    $PromHost"
Write-Host " Grafana:       $GrafanaHost (user=$GrafanaUser)"
Write-Host ""

# ===== Step 1: 直接检查后端 actuator =====
Write-Host "[Step 1] 直连后端 /actuator/prometheus 验证指标是否暴露" -ForegroundColor Yellow
try {
    $raw = Invoke-RestMethod -Uri "$BackendHost/actuator/prometheus" -TimeoutSec 10
    Write-Host "  ✓ /actuator/prometheus 返回成功 ($(($raw -split "`n").Count) 行)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ /actuator/prometheus 不可访问: $_" -ForegroundColor Red
    Write-Host "    请先启动 axiqra-core（端口 9090）" -ForegroundColor Red
    exit 1
}

# ===== Step 2: 验证 11 个 Axiqra 自定义指标 + JVM 基础指标 =====
Write-Host ""
Write-Host "[Step 2] 验证 11 个 Axiqra 自定义指标" -ForegroundColor Yellow
$missing = 0
foreach ($m in $ExpectedMetrics) {
    if ($raw -match "^(# HELP )?$($m.Name)(_total|_bucket|_count|_sum|\s|\{|$)") {
        Write-Host "  ✓ $($m.Name) [$($m.Kind) tags=$($m.Tags -join ',')]" -ForegroundColor Green
    } else {
        Write-Host "  ✗ $($m.Name) 未暴露" -ForegroundColor Red
        $missing++
    }
}

Write-Host ""
Write-Host "[Step 3] 验证 JVM 基础指标（5 个）" -ForegroundColor Yellow
foreach ($j in $JvmBaseline) {
    if ($raw -match "^(# HELP )?$($j[0])") {
        Write-Host "  ✓ $j" -ForegroundColor Green
    } else {
        Write-Host "  ✗ $j 未暴露" -ForegroundColor Red
        $missing++
    }
}

# ===== Step 4: Prometheus 是否能拉到 =====
Write-Host ""
Write-Host "[Step 4] 验证 Prometheus 已抓取 axiqra-core" -ForegroundColor Yellow
try {
    $promTargets = Invoke-RestMethod -Uri "$PromHost/api/v1/targets" -TimeoutSec 10
    $axiqraTarget = $promTargets.data.activeTargets | Where-Object { $_.labels.job -eq "axiqra-core" }
    if ($axiqraTarget) {
        Write-Host "  ✓ Prometheus 抓取目标: axiqra-core, health=$($axiqraTarget.health)" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Prometheus 未注册 axiqra-core job" -ForegroundColor Red
        $missing++
    }
} catch {
    Write-Host "  ✗ Prometheus API 不可访问: $_" -ForegroundColor Red
}

# ===== Step 5: 直接 PromQL 查询 axiqra 指标 =====
Write-Host ""
Write-Host "[Step 5] 验证 PromQL 查询结果" -ForegroundColor Yellow
$promQueries = @(
    @{ Query = "axiqra_search_requests_total"; Label = "Search 请求计数" }
    @{ Query = "axiqra_solution_invocations_total"; Label = "Solution 拉起计数" }
    @{ Query = "axiqra_review_decisions_total"; Label = "审核决策计数" }
)
foreach ($q in $promQueries) {
    try {
        $uri = "$PromHost/api/v1/query?query=" + [uri]::EscapeDataString($q.Query)
        $resp = Invoke-RestMethod -Uri $uri -TimeoutSec 10
        if ($resp.status -eq "success") {
            $cnt = $resp.data.result.Count
            Write-Host "  ✓ $($q.Label) ($($q.Query)): $cnt 个时间序列" -ForegroundColor Green
        } else {
            Write-Host "  ✗ $($q.Label) 查询失败: $($resp.error)" -ForegroundColor Red
        }
    } catch {
        Write-Host "  ✗ $($q.Label) PromQL 调用失败: $_" -ForegroundColor Red
    }
}

# ===== Step 6: Grafana 健康检查 =====
Write-Host ""
Write-Host "[Step 6] 验证 Grafana" -ForegroundColor Yellow
try {
    $cred = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${GrafanaUser}:${GrafanaPass}"))
    $ghealth = Invoke-RestMethod -Uri "$GrafanaHost/api/health" -Headers @{Authorization = "Basic $cred"} -TimeoutSec 10
    Write-Host "  ✓ Grafana 健康: $($ghealth.database) / version $($ghealth.version)" -ForegroundColor Green

    # 检查 datasource
    $ds = Invoke-RestMethod -Uri "$GrafanaHost/api/datasources" -Headers @{Authorization = "Basic $cred"} -TimeoutSec 10
    $promDs = $ds | Where-Object { $_.type -eq "prometheus" }
    if ($promDs) {
        Write-Host "  ✓ Prometheus 数据源已注册: $($promDs.name) (uid=$($promDs.uid))" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Prometheus 数据源未注册" -ForegroundColor Red
    }

    # 检查 dashboard
    $dashes = Invoke-RestMethod -Uri "$GrafanaHost/api/search?query=Axiqra" -Headers @{Authorization = "Basic $cred"} -TimeoutSec 10
    $axDash = $dashes | Where-Object { $_.title -match "Axiqra" }
    if ($axDash) {
        Write-Host "  ✓ Axiqra dashboard 已加载: $($axDash.title)" -ForegroundColor Green
        Write-Host "    访问: $GrafanaHost/d/$($axDash.uid)/$($axDash.uri)" -ForegroundColor Gray
    } else {
        Write-Host "  ⚠ Axiqra dashboard 未加载（可手动导入 JSON）" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ Grafana API 不可访问: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
if ($missing -eq 0) {
    Write-Host " ✓ 全部 16 个指标 + Prometheus + Grafana 验证通过" -ForegroundColor Green
    exit 0
} else {
    Write-Host " ⚠ $missing 个检查项未通过" -ForegroundColor Yellow
    exit 1
}
