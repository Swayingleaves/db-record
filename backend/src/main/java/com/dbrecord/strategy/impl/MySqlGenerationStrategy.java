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
        StringBuilder sql = new StringBuilder();
        sql.append("-- ALTER TABLE statements for ").append(formatIdentifier(tableName)).append("\n");
        
        // 这里可以根据tableChanges的具体内容生成相应的ALTER语句
        // 暂时返回注释，具体实现需要根据业务需求
        sql.append("-- TODO: Implement specific ALTER TABLE logic based on table changes");
        
        return sql.toString();
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