# 人大金仓数据库Schema支持改进

## 改进概述

根据需求，对人大金仓数据库的数据源支持进行了以下改进：

1. **系统Schema过滤**：在创建版本时查询schema和表结构时，过滤掉系统schema
2. **Schema分层展示**：版本详情中先展示schema再展示表，与PostgreSQL保持一致

## 具体改进内容

### 1. 后端改进

#### 1.1 KingbaseDatabaseSchemaExtractor.java

**文件路径**: `backend/src/main/java/com/dbrecord/service/impl/KingbaseDatabaseSchemaExtractor.java`

**主要改进**:

1. **Schema信息获取时过滤系统schema**:
   ```sql
   WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast', 'pg_temp_1', 'pg_toast_temp_1', 
                            'sys', 'SYS_HM', 'sys_catalog', 'sysmac')
   ```

2. **表结构获取时过滤系统schema**:
   ```sql
   WHERE t.table_schema NOT IN ('information_schema', 'pg_catalog', 'pg_toast', 
                               'sys', 'SYS_HM', 'sys_catalog', 'sysmac')
   ```

3. **增强表结构信息**:
   - 添加了表行数统计 (`table_rows`)
   - 添加了数据大小统计 (`data_length`)
   - 设置引擎类型为 'kingbase'

#### 1.2 过滤的系统Schema列表

根据需求，过滤以下系统schema：
- `sys` - 系统schema
- `SYS_HM` - 系统高可用管理schema
- `sys_catalog` - 系统目录schema
- `sysmac` - 系统MAC相关schema

以及原有的PostgreSQL系统schema：
- `information_schema` - 信息schema
- `pg_catalog` - PostgreSQL系统目录
- `pg_toast` - PostgreSQL TOAST存储
- `pg_temp_1` - 临时schema
- `pg_toast_temp_1` - 临时TOAST schema

### 2. 前端改进

#### 2.1 VersionDetailPage.vue

**文件路径**: `frontend/src/views/VersionDetailPage.vue`

**主要改进**:

1. **扩展Schema展示支持**:
   - 将原本只支持PostgreSQL的Schema分层展示扩展到人大金仓
   - 更新注释从"PostgreSQL Schema信息"改为"PostgreSQL/KingbaseES Schema信息"

2. **分层显示逻辑**:
   - 将 `isPostgreSQL` 计算属性改为 `isSchemaBasedDatabase`
   - 支持 `datasourceType === 'kingbase'` 的分层显示

3. **Schema分组逻辑**:
   - `schemaGroups` 计算属性现在同时支持PostgreSQL和KingbaseES
   - 默认展开所有schema的逻辑也扩展到人大金仓

#### 2.2 展示效果

人大金仓数据源现在将：
1. 在版本详情页面顶部显示Schema信息列表
2. 在表结构部分按Schema分组显示表
3. 每个Schema可以展开/折叠查看其下的表
4. 表名显示格式：`schema_name.table_name`（非public schema时）

### 3. 测试支持

#### 3.1 单元测试

**文件路径**: `backend/src/test/java/com/dbrecord/service/impl/KingbaseDatabaseSchemaExtractorTest.java`

**测试内容**:
- 连接URL构建测试
- 各个方法的结构完整性测试
- Schema名称默认处理测试

## 技术实现细节

### 1. Schema过滤实现

人大金仓基于PostgreSQL架构，因此可以使用类似的系统表查询：
- 使用 `information_schema.schemata` 获取schema列表
- 使用 `information_schema.tables` 获取表列表
- 通过 `WHERE` 条件过滤系统schema

### 2. 前端展示逻辑

前端通过 `datasourceType` 字段判断数据库类型：
- `postgresql` 或 `kingbase` 使用Schema分层展示
- 其他数据库类型使用平铺展示

### 3. 兼容性考虑

- 保持与现有PostgreSQL功能的一致性
- 不影响其他数据库类型的展示
- 向后兼容现有的人大金仓数据源

## 使用说明

1. **创建人大金仓数据源**时，系统会自动过滤系统schema
2. **创建版本**时，只会扫描用户schema和表
3. **查看版本详情**时，会按Schema分组展示表结构
4. **Schema展开/折叠**功能与PostgreSQL保持一致

## 注意事项

1. 人大金仓的JDBC驱动需要正确配置
2. 确保数据库用户有足够权限访问系统表
3. 系统schema过滤列表可根据实际需要调整
4. 建议在生产环境中测试schema过滤的完整性

## 版本对比功能修复

### 问题描述
人大金仓数据源的版本对比功能点击"开始对比"后没有反应。

### 根本原因
人大金仓的schema信息key名称与PostgreSQL不一致：
- **KingbaseDatabaseSchemaExtractor**: 使用 `"schemasInfo"` (驼峰命名)
- **PostgreSQLDatabaseSchemaExtractor**: 使用 `"schemas_info"` (下划线命名)
- **DatabaseSchemaServiceImpl**: 检查的key是 `"schemas_info"`

这导致人大金仓的schema信息没有被正确保存到数据库中，版本对比时无法获取schema信息。

### 修复方案
将KingbaseDatabaseSchemaExtractor中的key名称统一为 `"schemas_info"`：

```java
// 修复前
databaseInfo.put("schemasInfo", objectMapper.writeValueAsString(schemas));

// 修复后
databaseInfo.put("schemas_info", schemas);
```

### 修复效果
- ✅ 人大金仓的schema信息能正确保存到数据库
- ✅ 版本对比功能正常工作
- ✅ 版本详情页面能正确显示schema分层结构

## 后续优化建议

1. 可以考虑将系统schema过滤列表配置化
2. 可以添加更多人大金仓特有的系统表信息获取
3. 可以优化大量schema和表的展示性能
4. 建议统一所有数据库提取器的key命名规范，避免类似问题
