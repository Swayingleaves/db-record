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
                    alterSql.append(" DEFAULT '").append(column.get("columnDefault")).append("'");
                }
                
                alterSql.append(";\n");
                
                // PostgreSQL字段注释需要单独的COMMENT语句
                if (column.get("columnComment") != null && !column.get("columnComment").toString().isEmpty()) {
                    alterSql.append("COMMENT ON COLUMN ").append(formatIdentifier(tableName))
                           .append(".").append(formatIdentifier((String) column.get("columnName")))
                           .append(" IS '").append(column.get("columnComment")).append("';\n");
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
                
                // PostgreSQL需要分别修改类型和约束
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
                    alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName))
                           .append(" ALTER COLUMN ").append(formatIdentifier(columnName))
                           .append(" SET DEFAULT '").append(column.get("columnDefault")).append("';\n");
                }
                
                // PostgreSQL字段注释需要单独的COMMENT语句
                if (column.get("newComment") != null && !column.get("newComment").toString().isEmpty()) {
                    alterSql.append("COMMENT ON COLUMN ").append(formatIdentifier(tableName))
                           .append(".").append(formatIdentifier(columnName))
                           .append(" IS '").append(column.get("newComment")).append("';\n");
                }
            }
        }
        
        // 处理新增索引
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> addedIndexes = (List<Map<String, Object>>) tableChanges.get("addedIndexes");
        if (addedIndexes != null && !addedIndexes.isEmpty()) {
            for (Map<String, Object> index : addedIndexes) {
                if (Boolean.TRUE.equals(index.get("isPrimary"))) {
                    alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName)).append(" ADD PRIMARY KEY (")
                           .append(index.get("columnNames")).append(");\n");
                } else {
                    alterSql.append("CREATE ");
                    if (Boolean.TRUE.equals(index.get("isUnique"))) {
                        alterSql.append("UNIQUE ");
                    }
                    alterSql.append("INDEX ").append(formatIdentifier((String) index.get("indexName")))
                           .append(" ON ").append(formatIdentifier(tableName)).append(" (")
                           .append(index.get("columnNames")).append(");\n");
                }
            }
        }
        
        // 处理删除索引
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> removedIndexes = (List<Map<String, Object>>) tableChanges.get("removedIndexes");
        if (removedIndexes != null && !removedIndexes.isEmpty()) {
            for (Map<String, Object> index : removedIndexes) {
                if (Boolean.TRUE.equals(index.get("isPrimary"))) {
                    alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName)).append(" DROP CONSTRAINT ")
                           .append(formatIdentifier(tableName + "_pkey")).append(";\n");
                } else {
                    alterSql.append("DROP INDEX ").append(formatIdentifier((String) index.get("indexName"))).append(";\n");
                }
            }
        }
        
        return alterSql.toString();
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