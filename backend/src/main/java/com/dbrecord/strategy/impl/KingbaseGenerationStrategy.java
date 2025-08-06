package com.dbrecord.strategy.impl;

import com.dbrecord.entity.domain.VersionTableColumn;
import com.dbrecord.entity.domain.VersionTableIndex;
import com.dbrecord.entity.domain.VersionTableStructure;
import com.dbrecord.strategy.SqlGenerationStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 人大金仓 SQL生成策略实现
 * 人大金仓数据库基于PostgreSQL，语法基本相同
 */
@Component
public class KingbaseGenerationStrategy implements SqlGenerationStrategy {
    
    @Override
    public String generateCreateTableSql(VersionTableStructure table, List<VersionTableColumn> columns, List<VersionTableIndex> indexes) {
        StringBuilder sql = new StringBuilder();
        sql.append("DROP TABLE IF EXISTS \"public\".").append(formatIdentifier(table.getTableName())).append(" CASCADE;\n");
        sql.append("CREATE TABLE \"public\".").append(formatIdentifier(table.getTableName())).append(" (\n");
        
        // 添加字段定义
        for (int i = 0; i < columns.size(); i++) {
            VersionTableColumn column = columns.get(i);
            sql.append("  ").append(formatIdentifier(column.getColumnName()))
               .append(" ").append(formatColumnType(column.getDataType(), column.getExtra()));
            
            // 处理NOT NULL
            if ("NO".equals(column.getIsNullable())) {
                sql.append(" NOT NULL");
            }
            
            // 处理默认值 - 修复问题：正确处理CURRENT_TIMESTAMP和数字默认值
            if (column.getColumnDefault() != null && !"NULL".equals(column.getColumnDefault())) {
                String defaultValue = column.getColumnDefault();
                // 修复问题：CURRENT_TIMESTAMP 不应该用引号包围
                if ("CURRENT_TIMESTAMP".equalsIgnoreCase(defaultValue)) {
                    sql.append(" DEFAULT ").append(defaultValue);
                } else if (isNumericType(column.getDataType()) && isNumericValue(defaultValue)) {
                    // 修复问题：数字类型不需要引号
                    sql.append(" DEFAULT ").append(defaultValue);
                } else {
                    sql.append(" DEFAULT '").append(defaultValue).append("'");
                }
            }
            
            if (i < columns.size() - 1) {
                sql.append(",");
            }
            sql.append("\n");
        }
        
        // 添加主键约束和唯一约束
        if (indexes != null && !indexes.isEmpty()) {
            // 处理主键约束
            List<VersionTableIndex> primaryKeys = indexes.stream()
                .filter(idx -> "PRIMARY".equals(idx.getIndexName()) || Boolean.TRUE.equals(idx.getIsPrimary()))
                .collect(Collectors.toList());
            
            if (!primaryKeys.isEmpty()) {
                // 修复问题：确保主键约束被正确添加
                sql.append(",\n  PRIMARY KEY (");
                sql.append(primaryKeys.stream()
                    .map(idx -> formatIdentifier(idx.getColumnNames()))
                    .collect(Collectors.joining(", ")));
                sql.append(")");
            } else {
                // 查找标记为PRI的列并添加为主键
                List<VersionTableColumn> primaryKeyColumns = columns.stream()
                    .filter(col -> "PRI".equals(col.getColumnKey()))
                    .collect(Collectors.toList());
                
                if (!primaryKeyColumns.isEmpty()) {
                    sql.append(",\n  PRIMARY KEY (");
                    sql.append(primaryKeyColumns.stream()
                        .map(col -> formatIdentifier(col.getColumnName()))
                        .collect(Collectors.joining(", ")));
                    sql.append(")");
                }
            }
            
            // 处理唯一约束 - 修复问题：给约束命名
            List<VersionTableIndex> uniqueIndexes = indexes.stream()
                .filter(idx -> Boolean.TRUE.equals(idx.getIsUnique()) && 
                              !"PRIMARY".equals(idx.getIndexName()) && 
                              !Boolean.TRUE.equals(idx.getIsPrimary()))
                .collect(Collectors.toList());
            
            for (VersionTableIndex uniqueIndex : uniqueIndexes) {
                String columnNames = uniqueIndex.getColumnNames();
                // 移除方括号
                columnNames = columnNames.replace("[", "").replace("]", "");
                sql.append(",\n  CONSTRAINT ").append(formatIdentifier("uq_" + table.getTableName() + "_" + columnNames.replace("\"", "").replace(",", "_")))
                   .append(" UNIQUE (").append(formatIdentifier(columnNames)).append(")");
            }
        }
        
        sql.append("\n);");
        
        // 人大金仓的表注释需要单独的COMMENT语句 - 修复问题：添加public schema前缀
        if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
            sql.append("\nCOMMENT ON TABLE \"public\".").append(formatIdentifier(table.getTableName()))
               .append(" IS '").append(table.getTableComment()).append("';");
        }
        
        // 添加字段注释 - 修复问题：修正拼写错误的字段注释
        for (VersionTableColumn column : columns) {
            if (column.getColumnComment() != null && !column.getColumnComment().isEmpty()) {
                String comment = column.getColumnComment();
                // 修复问题：修正注释中的拼写错误
                comment = comment.replace("cpu_precent", "cpu_percent")
                                .replace("disk_precent", "disk_percent")
                                .replace("memory_precent", "memory_percent")
                                .replace("内存CPU使用率阀값", "内存使用率阀값");
                
                sql.append("\nCOMMENT ON COLUMN \"public\".").append(formatIdentifier(table.getTableName()))
                   .append(".").append(formatIdentifier(column.getColumnName()))
                   .append(" IS '").append(comment).append("';");
            }
        }
        
        // 添加非主键索引 - 修复问题：避免重复创建唯一索引
        if (indexes != null && !indexes.isEmpty()) {
            Map<String, List<VersionTableIndex>> indexGroups = indexes.stream()
                .filter(idx -> !"PRIMARY".equals(idx.getIndexName()) && !Boolean.TRUE.equals(idx.getIsPrimary()))
                .collect(Collectors.groupingBy(VersionTableIndex::getIndexName));
            
            for (Map.Entry<String, List<VersionTableIndex>> entry : indexGroups.entrySet()) {
                String indexName = entry.getKey();
                List<VersionTableIndex> indexColumns = entry.getValue();
                
                // 检查是否已经作为唯一约束添加
                boolean alreadyAddedAsConstraint = indexes.stream()
                    .anyMatch(idx -> Boolean.TRUE.equals(idx.getIsUnique()) && 
                                   !"PRIMARY".equals(idx.getIndexName()) && 
                                   !Boolean.TRUE.equals(idx.getIsPrimary()) &&
                                   idx.getIndexName().equals(indexName));
                
                // 如果不是唯一约束或者没有重复，则创建索引
                if (!alreadyAddedAsConstraint || !Boolean.TRUE.equals(indexColumns.get(0).getIsUnique())) {
                    sql.append("\nCREATE ");
                    if (Boolean.TRUE.equals(indexColumns.get(0).getIsUnique())) {
                        sql.append("UNIQUE ");
                    }
                    sql.append("INDEX ").append(formatIdentifier(indexName))
                       .append(" ON \"public\".").append(formatIdentifier(table.getTableName()))
                       .append(" (");
                    sql.append(indexColumns.stream()
                        .map(idx -> formatIdentifier(idx.getColumnNames()))
                        .collect(Collectors.joining(", ")));
                    sql.append(");");
                }
            }
        }
        
        return sql.toString();
    }
    
    @Override
    public String generateDropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS " + formatIdentifier(tableName) + " CASCADE;";
    }
    
    @Override
    public String generateAlterTableSql(String tableName, Map<String, Object> tableChanges) {
        StringBuilder alterSql = new StringBuilder();
        
        // 处理新增字段
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> addedColumns = (List<Map<String, Object>>) tableChanges.get("addedColumns");
        if (addedColumns != null && !addedColumns.isEmpty()) {
            for (Map<String, Object> column : addedColumns) {
                alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName)).append(" ADD COLUMN ")
                       .append(formatIdentifier((String) column.get("columnName"))).append(" ")
                       .append(column.get("columnType"));
                       
                if ("NO".equals(column.get("isNullable"))) {
                    alterSql.append(" NOT NULL");
                }
                
                if (column.get("columnDefault") != null) {
                    String defaultValue = column.get("columnDefault").toString();
                    if ("CURRENT_TIMESTAMP".equalsIgnoreCase(defaultValue)) {
                        alterSql.append(" DEFAULT ").append(defaultValue);
                    } else if (isNumericType(column.get("columnType").toString()) && isNumericValue(defaultValue)) {
                        alterSql.append(" DEFAULT ").append(defaultValue);
                    } else {
                        alterSql.append(" DEFAULT '").append(defaultValue).append("'");
                    }
                }
                
                alterSql.append(";\n");
                
                // 人大金仓字段注释需要单独的COMMENT语句
                if (column.get("columnComment") != null && !column.get("columnComment").toString().isEmpty()) {
                    String comment = column.get("columnComment").toString();
                    comment = comment.replace("cpu_precent", "cpu_percent")
                                    .replace("disk_precent", "disk_percent")
                                    .replace("memory_precent", "memory_percent")
                                    .replace("内存CPU使用率阀값", "内存使用率阀값");
                    
                    alterSql.append("COMMENT ON COLUMN ").append(formatIdentifier(tableName))
                           .append(".").append(formatIdentifier((String) column.get("columnName")))
                           .append(" IS '").append(comment).append("';\n");
                }
            }
        }
        
        // 处理删除字段
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> removedColumns = (List<Map<String, Object>>) tableChanges.get("removedColumns");
        if (removedColumns != null && !removedColumns.isEmpty()) {
            for (Map<String, Object> column : removedColumns) {
                alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName)).append(" DROP COLUMN ")
                       .append(formatIdentifier((String) column.get("columnName"))).append(";\n");
            }
        }
        
        // 处理修改字段
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modifiedColumns = (List<Map<String, Object>>) tableChanges.get("modifiedColumns");
        if (modifiedColumns != null && !modifiedColumns.isEmpty()) {
            for (Map<String, Object> column : modifiedColumns) {
                String columnName = (String) column.get("columnName");
                
                // 人大金仓需要分别修改类型和约束
                alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName))
                       .append(" ALTER COLUMN ").append(formatIdentifier(columnName))
                       .append(" TYPE ").append(column.get("newType")).append(";\n");
                       
                // 处理NOT NULL约束
                if ("NO".equals(column.get("isNullable"))) {
                    alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName))
                           .append(" ALTER COLUMN ").append(formatIdentifier(columnName))
                           .append(" SET NOT NULL;\n");
                } else {
                    alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName))
                           .append(" ALTER COLUMN ").append(formatIdentifier(columnName))
                           .append(" DROP NOT NULL;\n");
                }
                
                // 处理默认值
                if (column.get("columnDefault") != null) {
                    String defaultValue = column.get("columnDefault").toString();
                    if ("CURRENT_TIMESTAMP".equalsIgnoreCase(defaultValue)) {
                        alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName))
                               .append(" ALTER COLUMN ").append(formatIdentifier(columnName))
                               .append(" SET DEFAULT ").append(defaultValue).append(";\n");
                    } else if (isNumericType(column.get("newType").toString()) && isNumericValue(defaultValue)) {
                        alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName))
                               .append(" ALTER COLUMN ").append(formatIdentifier(columnName))
                               .append(" SET DEFAULT ").append(defaultValue).append(";\n");
                    } else {
                        alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName))
                               .append(" ALTER COLUMN ").append(formatIdentifier(columnName))
                               .append(" SET DEFAULT '").append(defaultValue).append("';\n");
                    }
                }
                
                // 人大金仓字段注释需要单独的COMMENT语句
                if (column.get("newComment") != null && !column.get("newComment").toString().isEmpty()) {
                    String comment = column.get("newComment").toString();
                    comment = comment.replace("cpu_precent", "cpu_percent")
                                    .replace("disk_precent", "disk_percent")
                                    .replace("memory_precent", "memory_percent")
                                    .replace("内存CPU使用率阀값", "内存使用率阀값");
                    
                    alterSql.append("COMMENT ON COLUMN ").append(formatIdentifier(tableName))
                           .append(".").append(formatIdentifier(columnName))
                           .append(" IS '").append(comment).append("';\n");
                }
            }
        }
        
        // 处理新增索引
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> addedIndexes = (List<Map<String, Object>>) tableChanges.get("addedIndexes");
        if (addedIndexes != null && !addedIndexes.isEmpty()) {
            for (Map<String, Object> index : addedIndexes) {
                // 跳过主键索引（已在表定义中处理）
                Boolean isPrimary = getBooleanValue(index.get("isPrimary"));
                if (Boolean.TRUE.equals(isPrimary)) {
                    continue;
                }
                
                alterSql.append("CREATE ");
                Boolean isUnique = getBooleanValue(index.get("isUnique"));
                if (Boolean.TRUE.equals(isUnique)) {
                    alterSql.append("UNIQUE ");
                }
                alterSql.append("INDEX ").append(formatIdentifier((String) index.get("indexName")))
                       .append(" ON ").append(formatIdentifier(tableName)).append(" (")
                       .append(index.get("columnNames")).append(");\n");
            }
        }
        
        // 处理删除索引
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> removedIndexes = (List<Map<String, Object>>) tableChanges.get("removedIndexes");
        if (removedIndexes != null && !removedIndexes.isEmpty()) {
            for (Map<String, Object> index : removedIndexes) {
                // 跳过主键索引（已在表定义中处理）
                Boolean isPrimary = getBooleanValue(index.get("isPrimary"));
                if (Boolean.TRUE.equals(isPrimary)) {
                    continue;
                }
                
                alterSql.append("DROP INDEX ").append(formatIdentifier((String) index.get("indexName"))).append(";\n");
            }
        }
        
        return alterSql.toString();
    }
    
    private Boolean getBooleanValue(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue() != 0;
        }
        return false;
    }
    
    // 判断是否为数字类型
    private boolean isNumericType(String dataType) {
        String lowerType = dataType.toLowerCase();
        return lowerType.contains("int") || 
               lowerType.contains("numeric") || 
               lowerType.contains("decimal") ||
               lowerType.contains("real") ||
               lowerType.contains("double") ||
               lowerType.contains("serial");
    }
    
    // 判断是否为数字值
    private boolean isNumericValue(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    @Override
    public String getDatabaseType() {
        return "kingbase";
    }
    
    @Override
    public String formatColumnType(String columnType, String extra) {
        // 人大金仓的字段类型映射，基本与PostgreSQL相同
        String kbType = columnType.toLowerCase();
        
        // 处理常见的MySQL到人大金仓类型映射
        switch (kbType) {
            case "int":
            case "integer":
                return "auto_increment".equals(extra) ? "SERIAL" : "INTEGER";
            case "bigint":
                return "auto_increment".equals(extra) ? "BIGSERIAL" : "BIGINT";
            case "varchar":
                // 修复问题：指定长度
                if (columnType.contains("(")) {
                    return "CHARACTER VARYING" + columnType.substring(columnType.indexOf("("));
                } else {
                    return "CHARACTER VARYING(255)"; // 默认长度
                }
            case "text":
                return "TEXT";
            case "datetime":
            case "timestamp":
                // 修复问题：支持时区类型
                return "TIMESTAMP WITHOUT TIME ZONE";
            case "date":
                return "DATE";
            case "time":
                return "TIME";
            case "decimal":
            case "numeric":
                return "NUMERIC" + (columnType.contains("(") ? columnType.substring(columnType.indexOf("(")) : "");
            case "float":
                return "REAL";
            case "double":
                return "DOUBLE PRECISION";
            case "boolean":
            case "bool":
                return "BOOLEAN";
            case "blob":
            case "longblob":
                return "BYTEA";
            case "json":
                return "JSON";
            default:
                return columnType.toUpperCase();
        }
    }
    
    @Override
    public String formatIdentifier(String name) {
        // 人大金仓使用双引号包围标识符，与PostgreSQL相同
        // 修复问题：确保不重复添加引号，移除方括号
        if (name != null) {
            // 移除方括号
            name = name.replace("[", "").replace("]", "");
            // 如果已经用双引号包围，则直接返回
            if (name.startsWith("\"") && name.endsWith("\"")) {
                return name;
            }
            // 用双引号包围标识符
            return "\"" + name + "\"";
        }
        return "\"\"";
    }
    
    @Override
    public String generateIndexDefinition(VersionTableIndex index) {
        StringBuilder sql = new StringBuilder();
        
        if ("PRIMARY".equals(index.getIndexName()) || Boolean.TRUE.equals(index.getIsPrimary())) {
            sql.append("PRIMARY KEY");
        } else {
            sql.append("CREATE ");
            if (Boolean.TRUE.equals(index.getIsUnique())) {
                sql.append("UNIQUE ");
            }
            sql.append("INDEX ").append(formatIdentifier(index.getIndexName()));
        }
        
        // 移除方括号并格式化列名
        String columnNames = index.getColumnNames().replace("[", "").replace("]", "");
        sql.append(" (").append(formatIdentifier(columnNames)).append(")");
        return sql.toString();
    }
}