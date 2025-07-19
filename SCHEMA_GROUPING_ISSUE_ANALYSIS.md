# Schema分组显示问题分析与解决方案

## 问题描述

在版本详情页面 `http://localhost:5173/dashboard/project/5/version/36` 中，Schema信息显示有2个Schema（public、test_schema），但表结构只显示了public schema的表，test_schema下的表没有显示。

## 问题分析

### 1. 数据调查结果

通过调试测试发现：

**版本36的实际数据**：
- 数据源类型：kingbase（人大金仓）
- Schema信息：包含 `public` 和 `test_schema` 两个schema
- 表数据：只有2个表，都在 `public` schema中
  - `oauth_client_details` (public)
  - `t_sys_access_log` (public)
- **test_schema中没有表数据**

### 2. 根本原因

问题不在前端的分组逻辑，而是在数据捕获阶段：

1. **数据库连接问题**：测试显示人大金仓数据库连接失败（Connection refused）
2. **历史数据问题**：版本36的数据是在test_schema中还没有表的时候捕获的
3. **部分捕获**：可能当时数据库连接有问题，只捕获了部分数据

### 3. 前端逻辑验证

通过模拟测试验证，前端的Schema分组逻辑是正确的：

```javascript
// 前端分组逻辑（VersionDetailPage.vue）
const schemaGroups = computed(() => {
  if (datasourceType.value !== 'postgresql' && datasourceType.value !== 'kingbase') {
    return null;
  }

  const groups = {};
  tables.value.forEach(table => {
    const schemaName = table.schemaName || 'public';
    if (!groups[schemaName]) {
      groups[schemaName] = [];
    }
    groups[schemaName].push(table);
  });

  return groups;
});
```

**模拟测试结果**：
- ✅ 能正确识别kingbase为基于Schema的数据库
- ✅ 能正确按schemaName分组表数据
- ✅ 能正确显示多个schema的表结构

## 解决方案

### 方案1：重新捕获数据库结构（推荐）

前端已经提供了"重新捕获结构"功能：

1. **操作步骤**：
   - 确保人大金仓数据库服务正常运行
   - 在test_schema中创建一些测试表
   - 在版本详情页面点击"重新捕获结构"按钮
   - 系统会重新扫描数据库并更新版本数据

2. **技术实现**：
   ```javascript
   // 前端调用
   async function captureSchema() {
     await request.post(`/api/project-version/capture-schema/${versionId}`);
     await loadVersionDetail(); // 重新加载数据
   }
   ```

3. **后端处理**：
   - 删除现有版本数据
   - 重新连接数据库
   - 扫描所有schema和表
   - 保存新的结构数据

### 方案2：数据库环境准备

为了验证功能，需要：

1. **启动人大金仓数据库**
2. **在test_schema中创建测试表**：
   ```sql
   -- 连接到experiment_user数据库
   \c experiment_user
   
   -- 在test_schema中创建测试表
   CREATE TABLE test_schema.test_table (
       id SERIAL PRIMARY KEY,
       name VARCHAR(100),
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   
   CREATE TABLE test_schema.user_info (
       user_id SERIAL PRIMARY KEY,
       username VARCHAR(50),
       email VARCHAR(100)
   );
   ```

### 方案3：手动验证（开发测试）

创建模拟数据来验证前端显示效果：

```java
// 在测试中验证分组逻辑
@Test
public void testSchemaGroupingWithTestData() {
    // 模拟包含test_schema表的数据
    List<Map<String, Object>> tables = Arrays.asList(
        createTable("oauth_client_details", "public"),
        createTable("t_sys_access_log", "public"),
        createTable("test_table", "test_schema"),
        createTable("user_info", "test_schema")
    );
    
    // 验证分组结果
    Map<String, List<Map<String, Object>>> groups = groupBySchema(tables);
    assert groups.get("public").size() == 2;
    assert groups.get("test_schema").size() == 2;
}
```

## 验证步骤

### 1. 环境检查
```bash
# 检查人大金仓数据库状态
lsof -i :54321

# 如果没有运行，启动数据库服务
# （具体命令取决于安装方式）
```

### 2. 数据库准备
```sql
-- 连接数据库并创建测试表
\c experiment_user
CREATE SCHEMA IF NOT EXISTS test_schema;
CREATE TABLE test_schema.test_table (id SERIAL, name VARCHAR(100));
INSERT INTO test_schema.test_table (name) VALUES ('测试数据');
```

### 3. 功能验证
1. 访问版本详情页面
2. 点击"重新捕获结构"按钮
3. 等待捕获完成
4. 验证是否显示test_schema及其表

### 4. 预期结果
```
📁 Schema: public (2个表)
  📄 oauth_client_details
  📄 t_sys_access_log

📁 Schema: test_schema (1个表)
  📄 test_table
```

## 技术细节

### 1. 数据流程
```
数据库 → KingbaseDatabaseSchemaExtractor → DatabaseSchemaService → API → 前端分组显示
```

### 2. 关键代码位置
- **后端提取器**：`KingbaseDatabaseSchemaExtractor.java`
- **API接口**：`ProjectVersionController.getVersionStructure()`
- **前端分组**：`VersionDetailPage.vue` 中的 `schemaGroups` 计算属性

### 3. 数据库表
- **版本表结构**：`version_table_structure` 表的 `schema_name` 字段
- **数据库结构**：`version_database_schema` 表的 `schemas_info` 字段

## 总结

**问题根源**：数据捕获时test_schema中没有表，或数据库连接问题导致部分捕获失败。

**解决方案**：使用前端的"重新捕获结构"功能，在确保数据库正常运行且test_schema中有表的情况下重新捕获数据。

**验证方法**：通过模拟测试已验证前端分组逻辑正确，问题确实在数据源而非显示逻辑。
