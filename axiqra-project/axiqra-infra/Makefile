.PHONY: help up down restart logs ps health clean

help:
	@echo "axiqra-infra: Axiqra 中间件管理"
	@echo "  make up       启动中间件（CockroachDB/PostgreSQL/Redis/MinIO）"
	@echo "  make down     停止中间件"
	@echo "  make restart  重启中间件"
	@echo "  make logs     实时查看日志"
	@echo "  make ps       查看容器状态"
	@echo "  make health   检查服务健康状态"
	@echo "  make clean    清理数据卷（数据将丢失）"

up:
	@if [ -f .env ]; then \
		echo ".env already exists, skipping copy."; \
	else \
		if [ ! -f .env.example ]; then \
			echo "ERROR: .env.example not found. Cannot create .env."; \
			exit 1; \
		fi; \
		cp .env.example .env; \
		echo ".env created from .env.example"; \
	fi
	docker compose up -d
	@echo "等待 healthcheck（最多 120s）..."
	@docker compose ps

down:
	docker compose down

restart:
	docker compose restart

logs:
	docker compose logs -f

ps:
	docker compose ps

health:
	@docker compose ps

clean:
	docker compose down -v
	@echo "已清理所有数据卷"
