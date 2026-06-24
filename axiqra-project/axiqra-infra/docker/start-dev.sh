#!/bin/bash
# Axiqra 开发环境启动脚本
# 同时运行 Flyway 迁移和应用程序

set -e

echo "=== Axiqra 开发环境启动 ==="

# 等待数据库就绪
echo "等待数据库就绪..."
sleep 5

# 启动应用（Flyway 迁移会在应用启动时自动执行）
echo "启动应用程序（Flyway 迁移将自动执行）..."
cd /app
java -jar axiqra-start.jar --spring.profiles.active=dev
