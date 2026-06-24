$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$codeDir = Join-Path $root "axiqra-project\axiqra-code"
$envFile = Join-Path $root ".env"

if (-not (Test-Path $envFile)) {
    throw "Missing $envFile. Copy .env.example to .env and fill in your cloud service credentials."
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith("#")) {
        return
    }

    $separator = $line.IndexOf("=")
    if ($separator -lt 1) {
        return
    }

    $name = $line.Substring(0, $separator).Trim()
    $value = $line.Substring($separator + 1).Trim()

    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}

if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = "cloud"
}

if ($env:LOGGING_FILE_PATH) {
    New-Item -ItemType Directory -Force -Path $env:LOGGING_FILE_PATH | Out-Null
}

$jdk17 = "D:\jdk-17"
if (Test-Path (Join-Path $jdk17 "bin\java.exe")) {
    $env:JAVA_HOME = $jdk17
    $env:PATH = (Join-Path $jdk17 "bin") + ";" + $env:PATH
}

$mvn = "mvn"
$mavenCandidates = @(
    "D:\apache-maven-3.9.9\bin\mvn.cmd",
    (Join-Path $root "tools\apache-maven-3.9.9\bin\mvn.cmd"),
    (Join-Path $root "tools\apache-maven-3.6.3\bin\mvn.cmd")
)

foreach ($candidate in $mavenCandidates) {
    if (Test-Path $candidate) {
        $mvn = $candidate
        $env:PATH = (Split-Path -Parent $candidate) + ";" + $env:PATH
        break
    }
}

& $mvn -f (Join-Path $codeDir "pom.xml") -pl axiqra-start -am -DskipTests install
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& $mvn -f (Join-Path $codeDir "axiqra-start\pom.xml") spring-boot:run
