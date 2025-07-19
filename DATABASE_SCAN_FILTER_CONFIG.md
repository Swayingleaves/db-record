# 数据库扫描过滤配置说明

## 概述

本系统支持通过配置文件灵活配置数据库扫描时需要过滤的schema和表，支持MySQL、PostgreSQL、KingbaseES（人大金仓）三种数据库类型的不同过滤规则。

## 配置文件位置

配置文件：`backend/src/main/resources/application.yml`

## 配置结构

```yaml
database:
  scan-filters:
    mysql:
      excluded-schemas:
        - information_schema
        - performance_schema
        - mysql
        - sys
      excluded-tables:
        # MySQL不使用schema概念，这里可以配置特定数据库下需要排除的表
        # 格式: database_name:
        #   - table1
        #   - table2
    postgresql:
      excluded-schemas:
        - information_schema
        - pg_catalog
        - pg_toast
        - pg_temp_1
        - pg_toast_temp_1
      excluded-tables:
        public:
          - temp_table
          - log_table
        # 可以为其他schema配置排除的表
        # schema_name:
        #   - table1
        #   - table2
    kingbase:
      excluded-schemas:
        - information_schema
        - pg_catalog
        - pg_toast
        - pg_temp_1
        - pg_toast_temp_1
        - sys
        - SYS_HM
        - sys_catalog
        - sysmac
      excluded-tables:
        public:
          - system_table
          - temp_table
        # 可以为其他schema配置排除的表
        # schema_name:
        #   - table1
        #   - table2
```

## 配置说明

### 1. Schema级别过滤

- **excluded-schemas**: 配置需要排除的schema列表
- 系统会在扫描时自动过滤这些schema下的所有表
- 不同数据库类型有不同的默认系统schema

### 2. 表级别过滤

- **excluded-tables**: 配置在特定schema下需要排除的表
- 格式：`schema_name: [table1, table2, ...]`
- 只有在schema没有被完全排除的情况下，表级别过滤才会生效

### 3. 数据库类型差异

#### MySQL
- 不使用schema概念，主要过滤系统数据库
- `excluded-schemas` 中配置的是数据库名称
- `excluded-tables` 中的key是数据库名称

#### PostgreSQL
- 使用schema概念
- 过滤系统schema：`information_schema`、`pg_catalog`、`pg_toast`等
- 支持按schema配置需要排除的表

#### KingbaseES（人大金仓）
- 基于PostgreSQL，使用schema概念
- 除了PostgreSQL的系统schema外，还过滤人大金仓特有的系统schema
- 特有系统schema：`sys`、`SYS_HM`、`sys_catalog`、`sysmac`

## 实现原理

### 1. 配置类

**DatabaseScanFilterProperties.java**
- 使用`@ConfigurationProperties`注解自动绑定配置
- 提供便捷的方法检查schema和表是否应该被排除

### 2. 数据库提取器

**AbstractDatabaseSchemaExtractor.java**
- 所有数据库提取器的基类
- 注入配置属性，提供获取过滤配置的方法

**具体实现类**
- `MySQLDatabaseSchemaExtractor`
- `PostgreSQLDatabaseSchemaExtractor`
- `KingbaseDatabaseSchemaExtractor`

### 3. 过滤逻辑

1. **SQL级别过滤**：在SQL查询中使用`WHERE ... NOT IN (...)`过滤schema
2. **应用级别过滤**：使用Java Stream API过滤表级别的结果

## 使用示例

### 1. 添加新的排除schema

```yaml
database:
  scan-filters:
    postgresql:
      excluded-schemas:
        - information_schema
        - pg_catalog
        - pg_toast
        - my_system_schema  # 新增的系统schema
```

### 2. 添加表级别过滤

```yaml
database:
  scan-filters:
    postgresql:
      excluded-tables:
        public:
          - temp_table
          - log_table
          - backup_table  # 新增需要排除的表
        my_schema:
          - system_table
          - cache_table
```

### 3. 为新数据库类型添加配置

如果需要支持新的数据库类型，需要：

1. 在配置文件中添加新的数据库类型配置
2. 在`DatabaseScanFilterProperties.getFilterConfig()`方法中添加对应的case
3. 创建对应的数据库提取器实现类

## 向后兼容性

- 如果没有配置过滤规则，系统会使用空的过滤配置
- 保持与现有硬编码过滤逻辑的兼容性
- 配置为空时不会影响现有功能

## 注意事项

1. **配置格式**：确保YAML格式正确，注意缩进
2. **大小写敏感**：schema和表名称区分大小写
3. **重启生效**：配置修改后需要重启应用才能生效
4. **性能考虑**：过多的表级别过滤可能影响性能，建议优先使用schema级别过滤

## 故障排除

### 1. 配置不生效

- 检查YAML格式是否正确
- 确认配置路径是否正确
- 重启应用确保配置加载

### 2. 过滤过多或过少

- 检查schema和表名称是否正确
- 确认数据库类型配置是否匹配
- 查看日志确认过滤逻辑是否正常执行

### 3. 性能问题

- 减少表级别过滤，优先使用schema级别过滤
- 检查SQL查询是否有性能问题
- 考虑添加数据库索引优化查询性能
