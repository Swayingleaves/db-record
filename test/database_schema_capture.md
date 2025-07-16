# 数据库结构捕获功能测试文档

## 功能概述

本功能实现了项目版本的数据库结构自动捕获和存储，将原本存储在 `project_version.schema_snapshot` 字段中的JSON数据，拆分到专门的关联表中，便于版本对比和结构分析。

## 数据库表结构

### 1. 版本数据库结构表 (`version_database_schema`)
- 存储数据库级别的基本信息
- 包含字符集、排序规则等信息

### 2. 版本表结构表 (`version_table_structure`)
- 存储每个表的基本信息
- 包含表名、注释、存储引擎、字符集等

### 3. 版本表字段结构表 (`version_table_column`)
- 存储每个表的字段详细信息
- 包含字段名、数据类型、长度、是否可空等

### 4. 版本表索引表 (`version_table_index`)
- 存储每个表的索引信息
- 包含索引名、类型、字段列表等

## 核心功能

### 1. 自动捕获数据库结构
- **触发时机**: 创建项目版本时自动触发
- **前提条件**: 项目必须关联数据源
- **捕获内容**: 数据库基本信息、所有表结构、字段信息、索引信息

### 2. 手动捕获数据库结构
- **接口**: `POST /api/project-version/capture-schema/{id}`
- **用途**: 手动重新捕获指定版本的数据库结构

### 3. 版本对比功能
- **接口**: `GET /api/project-version/compare/{fromVersionId}/{toVersionId}`
- **功能**: 基于新的表结构数据进行版本对比
- **对比内容**: 新增表、删除表、修改表等

## API 接口

### 创建版本（自动捕获结构）
```http
POST /api/project-version/create
Content-Type: application/json

{
  "projectId": 1,
  "versionName": "v1.0.0",
  "description": "初始版本"
}
```

**响应示例**:
```json
{
  "status": true,
  "code": 200,
  "msg": "版本创建成功，数据库结构已捕获",
  "data": {
    "id": 1,
    "projectId": 1,
    "versionName": "v1.0.0",
    "description": "初始版本",
    "userId": 1,
    "createTime": "2025-07-16T07:18:09"
  }
}
```

### 手动捕获数据库结构
```http
POST /api/project-version/capture-schema/1
```

**响应示例**:
```json
{
  "status": true,
  "code": 200,
  "msg": "数据库结构捕获成功",
  "data": null
}
```

### 版本对比
```http
GET /api/project-version/compare/1/2
```

**响应示例**:
```json
{
  "status": true,
  "code": 200,
  "msg": "成功",
  "data": {
    "fromVersion": "v1.0.0",
    "toVersion": "v1.1.0",
    "addedTables": ["new_table"],
    "removedTables": ["old_table"],
    "modifiedTables": [
      {
        "tableName": "user",
        "changeType": "COMMENT_CHANGED",
        "oldComment": "用户表",
        "newComment": "用户信息表"
      }
    ]
  }
}
```

## 测试步骤

### 1. 准备测试数据
1. 创建数据源（MySQL数据库）
2. 创建项目并关联数据源
3. 确保数据库中有一些测试表

### 2. 测试自动捕获
1. 创建项目版本
2. 检查是否自动捕获了数据库结构
3. 验证各个结构表中的数据

### 3. 测试手动捕获
1. 修改数据库结构（添加表、字段等）
2. 调用手动捕获接口
3. 验证新的结构是否被正确捕获

### 4. 测试版本对比
1. 创建两个不同的版本
2. 在两个版本之间修改数据库结构
3. 调用版本对比接口
4. 验证差异是否正确识别

## 数据库初始化

使用以下脚本初始化数据库：

```bash
# 在MySQL中执行
mysql -u root -p < backend/db/init_all.sql
```

或者单独执行版本结构相关的表：

```bash
mysql -u root -p < backend/db/version_database_schema.sql
```

## 注意事项

1. **数据库连接**: 确保数据源配置正确，能够正常连接
2. **权限检查**: 所有操作都会检查用户权限
3. **事务处理**: 结构捕获过程使用事务，确保数据一致性
4. **异常处理**: 捕获过程中的异常不会影响版本创建
5. **性能考虑**: 大型数据库的结构捕获可能需要较长时间

## 扩展功能

后续可以基于这个结构实现：

1. **详细的版本对比**: 字段级别的对比
2. **SQL脚本生成**: 基于结构差异生成迁移脚本
3. **结构可视化**: 图形化展示数据库结构
4. **历史追踪**: 表结构变更历史记录

## 状态

- ✅ 数据库表结构设计完成
- ✅ 实体类和Mapper创建完成
- ✅ 核心服务逻辑实现完成
- ✅ 控制器集成完成
- ✅ 编译测试通过
- 🔄 功能测试进行中 