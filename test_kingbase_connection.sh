#!/bin/bash

# KingbaseES 连接测试脚本

TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc1MjgzODM1NiwiZXhwIjoxNzUyOTI0NzU2fQ.rJmuYzYl-O4x3N1qajNtTQvpWQ-qA0G8HdeyM2HJp34"

echo "=== 测试 KingbaseES 数据源连接 ==="

# 测试数据源连接
curl -X POST http://localhost:8081/api/datasource/test \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "KingbaseES测试",
    "type": "kingbase",
    "host": "172.22.37.139",
    "port": 54321,
    "databaseName": "experiment_user",
    "username": "kingbase",
    "password": "Kingbase@2506_16",
    "description": "KingbaseES连接测试"
  }'

echo -e "\n\n=== 测试完成 ==="
