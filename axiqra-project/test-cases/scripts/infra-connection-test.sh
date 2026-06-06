#!/usr/bin/env bash
# infra-connection-test.sh
# 前置条件：docker compose up -d（启动 CockroachDB + PostgreSQL Audit + Redis + RabbitMQ + MinIO）

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CODE_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")/axiqra-code"

# Resolve JAVA_HOME: prefer env vars, then locate via PATH or common locations
if [[ -n "${JAVA17_HOME:-}" ]]; then
    JAVA_HOME="$JAVA17_HOME"
elif [[ -n "${JAVA_HOME:-}" ]]; then
    JAVA_HOME="$JAVA_HOME"
else
    JAVA_BIN=""
    if command -v java &>/dev/null; then
        JAVA_BIN="$(command -v java)"
    elif [[ -x "/usr/bin/java" ]]; then
        JAVA_BIN="/usr/bin/java"
    fi
    if [[ -n "$JAVA_BIN" ]]; then
        JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
    else
        echo "ERROR: Java not found. Set JAVA17_HOME or JAVA_HOME, or ensure java is in PATH." >&2
        exit 1
    fi
fi

echo "========================================"
echo " Axiqra Infrastructure Connection Test"
echo "========================================"
echo ""

# Docker 容器健康检查
check_container() {
    local name=$1
    local status
    status=$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null || echo "not-found")
    if [[ "$status" == "healthy" ]]; then
        echo "  [OK] $name is healthy"
        return 0
    else
        echo "  [FAIL] $name status: $status (expected: healthy)"
        return 1
    fi
}

ok=true
check_container "axiqra_cockroachdb_primary" || ok=false
check_container "axiqra_postgres_audit" || ok=false

if [[ "$ok" == "false" ]]; then
    echo ""
    echo "ERROR: Some containers are not healthy."
    exit 1
fi

export JAVA_HOME="$JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

cd "$CODE_DIR"

mvn clean test -pl axiqra-start -am \
    -Dtest=InfraConnectionIT \
    -DfailIfNoTests=false \
    -Dsurefire.failIfNoSpecifiedTests=false

echo ""
echo "========================================"
echo " [PASS] Infrastructure tests complete"
echo "========================================"
