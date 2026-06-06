#!/usr/bin/env bash
set -euo pipefail

: "${POSTGRES_DB:=axiqra_audit_db}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${AUDIT_DB_USER:?AUDIT_DB_USER is required}"
: "${AUDIT_DB_PASSWORD:?AUDIT_DB_PASSWORD is required}"

AUDIT_APP_DB_USER="${AUDIT_APP_DB_USER:-axiqra_audit_app}"
SQL_TEMPLATE="/docker-entrypoint-initdb.d/init.sql.template"

psql \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set ON_ERROR_STOP=1 \
  --set AUDIT_DB_USER="$AUDIT_DB_USER" \
  --set AUDIT_APP_DB_USER="$AUDIT_APP_DB_USER" \
  --set AUDIT_DB_PASSWORD="$AUDIT_DB_PASSWORD" \
  --file "$SQL_TEMPLATE"
