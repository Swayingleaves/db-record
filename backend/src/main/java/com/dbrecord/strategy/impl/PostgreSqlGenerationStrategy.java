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
 * PostgreSQL SQL生成策略实现
 */
@Component
public class PostgreSqlGenerationStrategy implements SqlGenerationStrategy {
    
    @Override
    public String generateCreateTableSql(VersionTableStructure table, List<VersionTableColumn> columns, List<VersionTableIndex> indexes) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(formatIdentifier(table.getTableName())).append(" (\n");
        
        // 添加字段定义
        for (int i = 0; i < columns.size(); i++) {
            VersionTableColumn column = columns.get(i);
            sql.append("  ").append(formatIdentifier(column.getColumnName()))
               .append(" ").append(formatColumnType(column.getDataType(), column.getExtra()));
            
            // 处理NOT NULL
            if ("NO".equals(column.getIsNullable())) {
                sql.append(" NOT NULL");
            }
            
            // 处理默认值
            if (column.getColumnDefault() != null && !"NULL".equals(column.getColumnDefault())) {
                sql.append(" DEFAULT '").append(column.getColumnDefault()).append("'");
            }
            
            if (i < columns.size() - 1) {
                sql.append(",");
            }
            sql.append("\n");
        }
        
        // 添加主键约束
        if (indexes != null && !indexes.isEmpty()) {
            List<VersionTableIndex> primaryKeys = indexes.stream()
                .filter(idx -> "PRIMARY".equals(idx.getIndexName()))
                .collect(Collectors.toList());
            
            if (!primaryKeys.isEmpty()) {
                sql.append(",\n  PRIMARY KEY (");
                sql.append(primaryKeys.stream()
                    .map(idx -> formatIdentifier(idx.getColumnNames()))
                    .collect(Collectors.joining(", ")));
                sql.append(")");
            }
        }
        
        sql.append("\n);");
        
        // PostgreSQL的表注释需要单独的COMMENT语句
        if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
            sql.append("\nCOMMENT ON TABLE ").append(formatIdentifier(table.getTableName()))
               .append(" IS '").append(table.getTableComment()).append("';");
        }
        
        // 添加字段注释
        for (VersionTableColumn column : columns) {
            if (column.getColumnComment() != null && !column.getColumnComment().isEmpty()) {
                sql.append("\nCOMMENT ON COLUMN ").append(formatIdentifier(table.getTableName()))
                   .append(".").append(formatIdentifier(column.getColumnName()))
                   .append(" IS '").append(column.getColumnComment()).append("';");
            }
        }
        
        // 添加非主键索引
        if (indexes != null && !indexes.isEmpty()) {
            Map<String, List<VersionTableIndex>> indexGroups = indexes.stream()
                .filter(idx -> !"PRIMARY".equals(idx.getIndexName()))
                .collect(Collectors.groupingBy(VersionTableIndex::getIndexName));
            
            for (Map.Entry<String, List<VersionTableIndex>> entry : indexGroups.entrySet()) {
                String indexName = entry.getKey();
                List<VersionTableIndex> indexColumns = entry.getValue();
                
                sql.append("\nCREATE ");
                if (Boolean.TRUE.equals(indexColumns.get(0).getIsUnique())) {
                    sql.append("UNIQUE ");
                }
                sql.append("INDEX ").append(formatIdentifier(indexName))
                   .append(" ON ").append(formatIdentifier(table.getTableName()))
                   .append(" (");
                sql.append(indexColumns.stream()
                    .map(idx -> formatIdentifier(idx.getColumnNames()))
                    .collect(Collectors.joining(", ")));
                sql.append(");");
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
        StringBuilder sql = new StringBuilder();
        sql.append("-- ALTER TABLE statements for ").append(formatIdentifier(tableName)).append("\n");
        
        // 这里可以根据tableChanges的具体内容生成相应的ALTER语句
        // 暂时返回注释，具体实现需要根据业务需求
        sql.append("-- TODO: Implement specific ALTER TABLE logic based on table changes");
        
        return sql.toString();
    }
    
    @Override
    public String getDatabaseType() {
        return "postgresql";
    }
    
    @Override
    public String formatColumnType(String columnType, String extra) {
        // PostgreSQL的字段类型映射
        String pgType = columnType.toLowerCase();
        
        // 处理常见的MySQL到PostgreSQL类型映射
        switch (pgType) {
            case "int":
            case "integer":
                return "auto_increment".equals(extra) ? "SERIAL" : "INTEGER";
            case "bigint":
                return "auto_increment".equals(extra) ? "BIGSERIAL" : "BIGINT";
            case "varchar":
                return "CHARACTER VARYING" + (columnType.contains("(") ? columnType.substring(columnType.indexOf("(")) : "");
            case "text":
                return "TEXT";
            case "datetime":
            case "timestamp":
                return "TIMESTAMP";
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
            default:
                return columnType.toUpperCase();
        }
    }
    
    @Override
    public String formatIdentifier(String name) {
        // PostgreSQL使用双引号包围标识符
        return "\"" + name + "\"";
    }
    
    @Override
    public String generateIndexDefinition(VersionTableIndex index) {
        StringBuilder sql = new StringBuilder();
        
        if ("PRIMARY".equals(index.getIndexName())) {
            sql.append("PRIMARY KEY");
        } else {
            sql.append("CREATE ");
            if (Boolean.TRUE.equals(index.getIsUnique())) {
                sql.append("UNIQUE ");
            }
            sql.append("INDEX ").append(formatIdentifier(index.getIndexName()));
        }
        
        sql.append(" (").append(formatIdentifier(index.getColumnNames())).append(")");
        return sql.toString();
    }
}