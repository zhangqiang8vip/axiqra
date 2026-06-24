$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = (Resolve-Path (Join-Path $scriptDir "..\..")).Path
$serviceName = "AxiqraMinIO"
$minioDir = Join-Path $root "tools\minio"
$nssmDir = Join-Path $root "tools\nssm"
$dataDir = Join-Path $root "data\minio"
$logDir = Join-Path $root "logs\minio"
$minioExe = Join-Path $minioDir "minio.exe"
$nssmExe = Join-Path $nssmDir "nssm.exe"
$nssmZip = Join-Path $nssmDir "nssm.zip"
$nssmExtract = Join-Path $nssmDir "extract"

$minioUrl = "https://dl.min.io/server/minio/release/windows-amd64/minio.exe"
$nssmUrl = "https://nssm.cc/release/nssm-2.24.zip"
$minioRootUser = if ($env:MINIO_ROOT_USER) { $env:MINIO_ROOT_USER } else { "axiqraadmin" }
$minioRootPassword = if ($env:MINIO_ROOT_PASSWORD) { $env:MINIO_ROOT_PASSWORD } else { "change_me_local_minio_password" }

New-Item -ItemType Directory -Force -Path $minioDir, $nssmDir, $dataDir, $logDir | Out-Null

if (-not (Test-Path $minioExe)) {
    Write-Host "Downloading MinIO..."
    Invoke-WebRequest -Uri $minioUrl -OutFile $minioExe
}

if (-not (Test-Path $nssmExe)) {
    Write-Host "Downloading NSSM..."
    Invoke-WebRequest -Uri $nssmUrl -OutFile $nssmZip
    if (Test-Path $nssmExtract) {
        Remove-Item -LiteralPath $nssmExtract -Recurse -Force
    }
    Expand-Archive -LiteralPath $nssmZip -DestinationPath $nssmExtract -Force
    $downloadedNssm = Get-ChildItem -Path $nssmExtract -Recurse -Filter "nssm.exe" |
        Where-Object { $_.FullName -match "\\win64\\nssm\.exe$" } |
        Select-Object -First 1
    if (-not $downloadedNssm) {
        throw "Downloaded NSSM archive did not contain win64\nssm.exe."
    }
    Copy-Item -LiteralPath $downloadedNssm.FullName -Destination $nssmExe -Force
}

& $nssmExe status $serviceName *> $null
if ($LASTEXITCODE -eq 0) {
    & $nssmExe stop $serviceName *> $null
    & $nssmExe remove $serviceName confirm
}

& $nssmExe install $serviceName $minioExe
& $nssmExe set $serviceName AppDirectory $root
& $nssmExe set $serviceName AppParameters "server `"$dataDir`" --address 127.0.0.1:9000 --console-address 127.0.0.1:9001"
& $nssmExe set $serviceName AppEnvironmentExtra "MINIO_ROOT_USER=$minioRootUser" "MINIO_ROOT_PASSWORD=$minioRootPassword"
& $nssmExe set $serviceName AppStdout (Join-Path $logDir "service-stdout.log")
& $nssmExe set $serviceName AppStderr (Join-Path $logDir "service-stderr.log")
& $nssmExe set $serviceName AppRotateFiles 1
& $nssmExe set $serviceName AppRotateOnline 1
& $nssmExe set $serviceName AppRotateSeconds 86400
& $nssmExe set $serviceName Start SERVICE_DEMAND_START
& $nssmExe set $serviceName DisplayName "Axiqra MinIO"
& $nssmExe set $serviceName Description "Local MinIO object storage for Axiqra development."

Write-Host "Installed $serviceName as a manual Windows service."
Write-Host "Start from services.msc or run: net start $serviceName"
