# Cloud Environment

Use root `.env` as the only place for cloud service credentials. The committed
Spring profile reads environment variables only, so switching providers or
accounts is just replacing `.env`.

## Files

- `.env.example`: template for local credentials.
- `axiqra-project/axiqra-code/axiqra-start/src/main/resources/application-cloud.yml`: cloud profile.
- `tools/start-backend-cloud.ps1`: loads `.env` and starts the backend.

## Start

```powershell
Copy-Item .env.example .env
notepad .env
powershell -ExecutionPolicy Bypass -File .\tools\start-backend-cloud.ps1
```

## Required Services

- CockroachDB: set `COCKROACH_DB_URL`, `COCKROACH_DB_USER`, `COCKROACH_DB_PASSWORD`.
- PostgreSQL audit DB: set `AUDIT_DB_URL`, `AUDIT_APP_DB_USER`, `AUDIT_APP_DB_PASSWORD`.
- Redis: set `REDIS_HOST`, `REDIS_PORT`, `REDIS_USERNAME`, `REDIS_PASSWORD`, `REDIS_SSL_ENABLED`.
- MinIO/S3-compatible storage: optional while `AXIQRA_ENABLE_MINIO=false`.

Keep `.env` uncommitted. It is already ignored by `.gitignore`.
