# Axiqra 宣传页一键启动脚本
# 用法: 右键 → 使用 PowerShell 运行（建议管理员权限）
# 注意：此文件需要以 UTF-8 with BOM 保存

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
$LogFile = Join-Path $ProjectRoot "start.log"

$PublicSiteUrl = if ($env:PUBLIC_SITE_URL) { $env:PUBLIC_SITE_URL } else { "https://www.axiqra.com" }

function Write-ProjectLog($Msg) {
    $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $Msg"
    $line | Tee-Object -FilePath $LogFile -Append
}

Write-ProjectLog "========== Axiqra 启动 =========="

# 1. 等待 Docker 就绪
Write-ProjectLog "等待 Docker Desktop..."
$retries = 0
while ($retries -lt 30) {
    try {
        docker info 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { break }
    } catch {}
    Start-Sleep 2
    $retries++
}
if ($retries -ge 30) {
    Write-ProjectLog "ERROR: Docker Desktop 未就绪，请手动启动后重试"
    Read-Host "按回车退出"
    exit 1
}
Write-ProjectLog "Docker 就绪"

# 2. 启动容器
Write-ProjectLog "启动 Docker 容器..."
$WebsiteDir = $ProjectRoot
if (-not (Test-Path (Join-Path $WebsiteDir "docker-compose.yml"))) {
    Write-ProjectLog "ERROR: 找不到 docker-compose.yml"
    Read-Host "按回车退出"
    exit 1
}
Push-Location $WebsiteDir
docker compose up -d 2>&1 | Tee-Object -FilePath $LogFile -Append
Pop-Location

if ($LASTEXITCODE -ne 0) {
    Write-ProjectLog "ERROR: 容器启动失败"
    Read-Host "按回车退出"
    exit 1
}
Write-ProjectLog "容器启动完成"

# 3. 启动 Cloudflare Tunnel
Write-ProjectLog "启动 Cloudflare Tunnel..."
$existing = Get-Process cloudflared -ErrorAction SilentlyContinue
if ($existing) {
    Stop-Process -Name cloudflared -Force -ErrorAction SilentlyContinue
    Start-Sleep 1
}

$cloudflaredPath = $null
if ($env:CLOUDFLARED_PATH) {
    $cloudflaredPath = $env:CLOUDFLARED_PATH
} else {
    $cmd = Get-Command cloudflared -ErrorAction SilentlyContinue
    if ($cmd -and $cmd.Source) {
        $cloudflaredPath = $cmd.Source
    }
    if (-not $cloudflaredPath) {
        $altPaths = @(
            "C:\Program Files (x86)\cloudflared\cloudflared.exe",
            "C:\Program Files\cloudflared\cloudflared.exe"
        )
        foreach ($p in $altPaths) {
            if (Test-Path $p) { $cloudflaredPath = $p; break }
        }
    }
}

if (-not $cloudflaredPath) {
    Write-ProjectLog "ERROR: 找不到 cloudflared.exe。请确保已安装 Cloudflare Tunnel。"
    Read-Host "按回车退出"
    exit 1
}

Start-Process -FilePath $cloudflaredPath `
    -ArgumentList "tunnel","run","--protocol","http2","axiqra" `
    -WindowStyle Hidden
Start-Sleep 5

# 4. 验证
Write-ProjectLog "验证服务..."
$health = curl.exe -s http://localhost:8080/api/health 2>&1
Write-ProjectLog "本地 API: $health"

Write-ProjectLog "========== 启动完成 =========="
Write-ProjectLog "本地访问: http://localhost:8080"
Write-ProjectLog "公网访问: $PublicSiteUrl"
Write-Host ""
Write-Host "启动完成！" -ForegroundColor Green
Write-Host "   本地: http://localhost:8080"
Write-Host "   公网: $PublicSiteUrl"
Write-Host ""
Read-Host "按回车关闭此窗口（服务继续后台运行）"
