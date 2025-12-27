#!/bin/bash

# MANTIS Project Startup Script
# Starts Infrastructure, Services, and Exporters

echo "========================================="
echo "   MANTIS: Industrial Predictive Maintenance"
echo "   Starting System..."
echo "========================================="

# 1. Stop any running containers to ensure clean state
echo "[1/3] Stopping previous instances..."
docker-compose \
    -f infrastructure/docker/docker-compose.infrastructure.yml \
    -f infrastructure/docker/docker-compose.services.yml \
    up -d --build

# 3. Validation
echo "[3/3] System Status:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "========================================="
echo "   MANTIS IS RUNNING"
echo "========================================="
echo "- Grafana: http://localhost:3000 (User/Pass: admin/admin)"
echo "- Dashboard Frontend: http://localhost:3001"
echo "- Kafka UI: http://localhost:8082"
echo "- Jaeger UI: http://localhost:16686"
echo "========================================="
