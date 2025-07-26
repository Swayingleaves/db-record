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
 * MySQL SQL生成策略实现
 */
@Component
public class MySqlGenerationStrategy implements SqlGenerationStrategy {
    
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
            
            // 处理AUTO_INCREMENT
            if ("auto_increment".equals(column.getExtra())) {
                sql.append(" AUTO_INCREMENT");
            }
            
            // 处理注释
            if (column.getColumnComment() != null && !column.getColumnComment().isEmpty()) {
                sql.append(" COMMENT '").append(column.getColumnComment()).append("'");
            }
            
            if (i < columns.size() - 1) {
                sql.append(",");
            }
            sql.append("\n");
        }
        
        // 添加索引定义
        if (indexes != null && !indexes.isEmpty()) {
            // 按索引名分组
            Map<String, List<VersionTableIndex>> indexGroups = indexes.stream()
                .collect(Collectors.groupingBy(VersionTableIndex::getIndexName));
            
            for (Map.Entry<String, List<VersionTableIndex>> entry : indexGroups.entrySet()) {
                String indexName = entry.getKey();
                List<VersionTableIndex> indexColumns = entry.getValue();
                
                if ("PRIMARY".equals(indexName)) {
                    sql.append(",\n  PRIMARY KEY (");
                    sql.append(indexColumns.stream()
                        .map(idx -> formatIdentifier(idx.getColumnNames()))
                        .collect(Collectors.joining(", ")));
                    sql.append(")");
                } else {
                    sql.append(",\n  ");
                    if (Boolean.TRUE.equals(indexColumns.get(0).getIsUnique())) {
                        sql.append("UNIQUE ");
                    }
                    sql.append("KEY ").append(formatIdentifier(indexName)).append(" (");
                    sql.append(indexColumns.stream()
                        .map(idx -> formatIdentifier(idx.getColumnNames()))
                        .collect(Collectors.joining(", ")));
                    sql.append(")");
                }
            }
        }
        
        sql.append("\n)");
        
        // 添加表注释
        if (table.getTableComment() != null && !table.getTableComment().isEmpty()) {
            sql.append(" COMMENT='").append(table.getTableComment()).append("'");
        }
        
        sql.append(";");
        return sql.toString();
    }
    
    @Override
    public String generateDropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS " + formatIdentifier(tableName) + ";";
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
                
                if (column.get("columnComment") != null && !column.get("columnComment").toString().isEmpty()) {
                    alterSql.append(" COMMENT '").append(column.get("columnComment")).append("'");
                }
                
                alterSql.append(";\n");
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
                alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName)).append(" MODIFY COLUMN ")
                       .append(formatIdentifier((String) column.get("columnName"))).append(" ")
                       .append(column.get("newType"));
                       
                if ("NO".equals(column.get("isNullable"))) {
                    alterSql.append(" NOT NULL");
                }
                
                if (column.get("columnDefault") != null) {
                    alterSql.append(" DEFAULT '").append(column.get("columnDefault")).append("'");
                }
                
                if (column.get("newComment") != null && !column.get("newComment").toString().isEmpty()) {
                    alterSql.append(" COMMENT '").append(column.get("newComment")).append("'");
                }
                
                alterSql.append(";\n");
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
                    String indexType = Boolean.TRUE.equals(index.get("isUnique")) ? "UNIQUE INDEX" : "INDEX";
                    alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName)).append(" ADD ").append(indexType)
                           .append(" ").append(formatIdentifier((String) index.get("indexName"))).append(" (")
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
                    alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName)).append(" DROP PRIMARY KEY;\n");
                } else {
                    alterSql.append("ALTER TABLE ").append(formatIdentifier(tableName)).append(" DROP INDEX ")
                           .append(formatIdentifier((String) index.get("indexName"))).append(";\n");
                }
            }
        }
        
        return alterSql.toString();
    }
    
    @Override
    public String getDatabaseType() {
        return "mysql";
    }
    
    @Override
    public String formatColumnType(String columnType, String extra) {
        // MySQL的字段类型格式化
        return columnType.toUpperCase();
    }
    
    @Override
    public String formatIdentifier(String name) {
        // MySQL使用反引号包围标识符
        return "`" + name + "`";
    }
    
    @Override
    public String generateIndexDefinition(VersionTableIndex index) {
        StringBuilder sql = new StringBuilder();
        
        if ("PRIMARY".equals(index.getIndexName())) {
            sql.append("PRIMARY KEY");
        } else {
            if (Boolean.TRUE.equals(index.getIsUnique())) {
                sql.append("UNIQUE KEY ");
            } else {
                sql.append("KEY ");
            }
            sql.append(formatIdentifier(index.getIndexName()));
        }
        
        sql.append(" (").append(formatIdentifier(index.getColumnNames())).append(")");
        return sql.toString();
    }
}