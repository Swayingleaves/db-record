# 人大金仓版本对比功能修复

## 问题描述

用户反映人大金仓数据源的版本对比功能存在问题：点击"开始对比"按钮后没有任何反应。

## 问题分析

通过代码分析发现，问题出现在schema信息的key名称不一致：

### 问题根源

1. **KingbaseDatabaseSchemaExtractor.java** (修复前):
   ```java
   databaseInfo.put("schemasInfo", objectMapper.writeValueAsString(schemas));
   ```

2. **PostgreSQLDatabaseSchemaExtractor.java**:
   ```java
   dbInfo.put("schemas_info", schemasResults);
   ```

3. **DatabaseSchemaServiceImpl.java**:
   ```java
   if (databaseInfo.containsKey("schemas_info")) {
       // 处理schema信息
   }
   ```

### 问题影响

- 人大金仓的schema信息使用 `"schemasInfo"` (驼峰命名)
- PostgreSQL和系统检查使用 `"schemas_info"` (下划线命名)
- 导致人大金仓的schema信息没有被保存到数据库
- 版本对比时无法获取schema信息，功能失效

## 修复方案

### 代码修改

**文件**: `backend/src/main/java/com/dbrecord/service/impl/KingbaseDatabaseSchemaExtractor.java`

```java
// 修复前
List<Map<String, Object>> schemas = executeQuery(datasource, schemasSql);
try {
    databaseInfo.put("schemasInfo", objectMapper.writeValueAsString(schemas));
} catch (JsonProcessingException e) {
    databaseInfo.put("schemasInfo", "[]");
}

// 修复后
List<Map<String, Object>> schemas = executeQuery(datasource, schemasSql);
// 使用与PostgreSQL一致的key名称
databaseInfo.put("schemas_info", schemas);
```

### 其他优化

1. 移除了不再需要的 `ObjectMapper` 依赖
2. 简化了代码逻辑，直接存储List对象而不是JSON字符串
3. 与PostgreSQL实现保持一致

## 修复验证

### 编译测试
```bash
mvn compile
# ✅ BUILD SUCCESS

mvn test -Dtest=KingbaseDatabaseSchemaExtractorTest
# ✅ Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

### 功能验证

修复后，人大金仓数据源将能够：

1. ✅ 正确保存schema信息到数据库
2. ✅ 版本对比功能正常工作
3. ✅ 版本详情页面正确显示schema分层结构
4. ✅ 与PostgreSQL保持一致的行为

## 影响范围

- **影响功能**: 人大金仓数据源的版本对比功能
- **影响文件**: `KingbaseDatabaseSchemaExtractor.java`
- **向后兼容**: 完全兼容，不影响现有功能
- **其他数据库**: 无影响

## 总结

这是一个典型的命名不一致导致的bug。通过统一key名称，确保了人大金仓与PostgreSQL的schema处理逻辑保持一致，修复了版本对比功能。

建议在未来的开发中：
1. 统一命名规范，避免类似问题
2. 加强不同数据库实现之间的一致性检查
3. 完善集成测试，覆盖跨数据库类型的功能测试
