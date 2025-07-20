# Docker 部署指南

## 项目结构
```
db-record/
├── backend/
│   ├── Dockerfile              # 后端Docker构建文件
│   └── .dockerignore          # 后端构建忽略文件
├── frontend/
│   ├── Dockerfile             # 前端Docker构建文件
│   ├── nginx.conf             # Nginx配置文件
│   └── .dockerignore          # 前端构建忽略文件
├── config/
│   └── application.yml        # 外部配置文件
└── docker-compose.yml         # 容器编排文件
```

## 部署步骤

### 1. 构建和启动所有服务
```bash
docker-compose up -d
```

### 2. 查看服务状态
```bash
docker-compose ps
```

### 3. 查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql
```

### 4. 停止服务
```bash
docker-compose down
```

### 5. 重新构建并启动
```bash
docker-compose up -d --build
```

## 服务访问

- **前端应用**: http://localhost:80
- **后端API**: http://localhost:8081
- **MySQL数据库**: localhost:3306

## 配置说明

### 外部配置文件
后端支持通过外部配置文件覆盖默认配置：
- 配置文件位置：`./config/application.yml`
- 该文件会挂载到容器内的 `/app/config/` 目录
- Spring Boot会自动加载外部配置文件

### 数据库配置
- 数据库数据持久化在Docker卷 `mysql_data` 中
- 初始化SQL脚本：`./backend/db/init_all.sql`
- 默认数据库：`db_record`
- 默认用户：`root/root`

### 网络配置
- 所有服务运行在 `db-record-network` 网络中
- 服务间通信使用容器名（如：mysql、backend、frontend）

## 健康检查

### 后端健康检查
后端服务包含健康检查端点：`/actuator/health`

### 数据库健康检查
MySQL服务会在启动后进行健康检查，确保数据库可用

## 自定义配置

### 修改端口映射
在 `docker-compose.yml` 中修改 `ports` 配置：
```yaml
services:
  frontend:
    ports:
      - "8080:80"  # 将前端映射到8080端口
  backend:
    ports:
      - "8082:8081"  # 将后端映射到8082端口
```

### 修改数据库配置
在 `./config/application.yml` 中修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/your_database
    username: your_username
    password: your_password
```

## 故障排除

### 1. 后端启动失败
- 检查数据库是否正常启动：`docker-compose logs mysql`
- 检查配置文件是否正确：`./config/application.yml`

### 2. 前端无法访问后端
- 检查nginx配置：`./frontend/nginx.conf`
- 确认后端服务正常运行：`docker-compose logs backend`

### 3. 数据库连接失败
- 确认MySQL容器正常运行
- 检查数据库初始化脚本是否执行成功
- 验证网络连接：`docker network ls`

## 生产环境注意事项

1. **安全配置**：修改默认密码和敏感信息
2. **资源限制**：为容器设置内存和CPU限制
3. **日志管理**：配置日志轮转和持久化
4. **备份策略**：定期备份数据库数据卷
5. **监控**：添加容器和应用监控