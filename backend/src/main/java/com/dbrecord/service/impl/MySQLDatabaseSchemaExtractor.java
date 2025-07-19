package com.dbrecord.service.impl;

import com.dbrecord.entity.domain.Datasource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL数据库结构提取器
 */
@Component
public class MySQLDatabaseSchemaExtractor extends AbstractDatabaseSchemaExtractor {

    @Override
    protected String buildConnectionUrl(Datasource datasource) {
        return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC",
                datasource.getHost(), datasource.getPort(), datasource.getDatabaseName());
    }

    @Override
    protected String getDatabaseType() {
        return "mysql";
    }
    
    @Override
    public Map<String, Object> getDatabaseInfo(Datasource datasource) {
        String sql = "SELECT DEFAULT_CHARACTER_SET_NAME as charset, DEFAULT_COLLATION_NAME as collation " +
                     "FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?";
        
        List<Map<String, Object>> results = executeQuery(datasource, sql, datasource.getDatabaseName());
        
        if (!results.isEmpty()) {
            return results.get(0);
        }
        
        return new HashMap<>();
    }
    
    @Override
    public List<Map<String, Object>> getTablesStructure(Datasource datasource) {
        var filterConfig = getFilterConfig();

        // MySQL不使用schema概念，这里检查当前数据库是否在排除列表中
        if (filterConfig.isSchemaExcluded(datasource.getDatabaseName())) {
            return new java.util.ArrayList<>(); // 如果整个数据库被排除，返回空列表
        }

        String sql = "SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
        List<Map<String, Object>> tables = executeQuery(datasource, sql, datasource.getDatabaseName());

        // 应用表级别的过滤（MySQL中schema就是数据库名）
        return tables.stream()
                .filter(table -> {
                    String tableName = (String) table.get("TABLE_NAME");
                    return !filterConfig.isTableExcluded(datasource.getDatabaseName(), tableName);
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> getTableColumns(Datasource datasource, String schemaName, String tableName) {
        String sql = "SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        return executeQuery(datasource, sql, datasource.getDatabaseName(), tableName);
    }
    
    @Override
    public List<Map<String, Object>> getTableIndexes(Datasource datasource, String schemaName, String tableName) {
        String sql = "SELECT " +
                     "INDEX_NAME, " +
                     "TABLE_NAME, " +
                     "COLUMN_NAME, " +
                     "NON_UNIQUE, " +
                     "SEQ_IN_INDEX, " +
                     "INDEX_TYPE, " +
                     "INDEX_COMMENT, " +
                     "SUB_PART " +
                     "FROM information_schema.STATISTICS " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                     "ORDER BY INDEX_NAME, SEQ_IN_INDEX";
        
        return executeQuery(datasource, sql, datasource.getDatabaseName(), tableName);
    }
}