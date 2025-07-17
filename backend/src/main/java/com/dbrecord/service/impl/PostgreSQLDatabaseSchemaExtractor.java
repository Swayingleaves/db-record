package com.dbrecord.service.impl;

import com.dbrecord.entity.domain.Datasource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL数据库结构提取器
 */
@Component
public class PostgreSQLDatabaseSchemaExtractor extends AbstractDatabaseSchemaExtractor {
    
    @Override
    protected String buildConnectionUrl(Datasource datasource) {
        return String.format("jdbc:postgresql://%s:%d/%s",
                datasource.getHost(), datasource.getPort(), datasource.getDatabaseName());
    }
    
    @Override
    public Map<String, Object> getDatabaseInfo(Datasource datasource) {
        String sql = "SELECT pg_encoding_to_char(encoding) as charset, datcollate as collation " +
                     "FROM pg_database WHERE datname = ?";
        
        List<Map<String, Object>> results = executeQuery(datasource, sql, datasource.getDatabaseName());
        
        Map<String, Object> dbInfo = new HashMap<>();
        if (!results.isEmpty()) {
            dbInfo.putAll(results.get(0));
        }
        
        // 获取所有schema信息
        String schemasSql = "SELECT schema_name, schema_owner " +
                           "FROM information_schema.schemata " +
                           "WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast') " +
                           "ORDER BY schema_name";
        
        List<Map<String, Object>> schemasResults = executeQuery(datasource, schemasSql);
        dbInfo.put("schemas_info", schemasResults);
        
        return dbInfo;
    }
    
    @Override
    public List<Map<String, Object>> getTablesStructure(Datasource datasource) {
        String sql = "SELECT t.table_schema as schema_name, t.table_name, obj_description(c.oid) as table_comment, " +
                     "t.table_type, 'postgresql' as engine, " +
                     "COALESCE(s.n_tup_ins, 0) as table_rows, " +
                     "COALESCE(pg_total_relation_size(c.oid), 0) as data_length, " +
                     "0 as index_length, " +
                     "NULL as create_time, NULL as update_time " +
                     "FROM information_schema.tables t " +
                     "LEFT JOIN pg_class c ON c.relname = t.table_name AND c.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = t.table_schema) " +
                     "LEFT JOIN pg_stat_user_tables s ON s.relname = t.table_name AND s.schemaname = t.table_schema " +
                     "WHERE t.table_schema NOT IN ('information_schema', 'pg_catalog', 'pg_toast') AND t.table_type = 'BASE TABLE' " +
                     "ORDER BY t.table_schema, t.table_name";
        
        return executeQuery(datasource, sql);
    }
    
    @Override
    public List<Map<String, Object>> getTableColumns(Datasource datasource, String tableName) {
        // 解析表名，支持schema.table格式
        String schemaName = "public";
        String actualTableName = tableName;
        
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.", 2);
            schemaName = parts[0];
            actualTableName = parts[1];
        }
        
        String sql = "SELECT *, " +
                     "CASE " +
                     "  WHEN data_type = 'character varying' THEN 'varchar(' || character_maximum_length || ')' " +
                     "  WHEN data_type = 'character' THEN 'char(' || character_maximum_length || ')' " +
                     "  WHEN data_type = 'numeric' AND numeric_precision IS NOT NULL AND numeric_scale IS NOT NULL THEN 'numeric(' || numeric_precision || ',' || numeric_scale || ')' " +
                     "  WHEN data_type = 'numeric' AND numeric_precision IS NOT NULL THEN 'numeric(' || numeric_precision || ')' " +
                     "  WHEN data_type = 'timestamp without time zone' THEN 'timestamp' " +
                     "  WHEN data_type = 'timestamp with time zone' THEN 'timestamptz' " +
                     "  WHEN data_type = 'time without time zone' THEN 'time' " +
                     "  WHEN data_type = 'time with time zone' THEN 'timetz' " +
                     "  ELSE data_type " +
                     "END AS column_type " +
                     "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        return executeQuery(datasource, sql, schemaName, actualTableName);
    }
    
    @Override
    public List<Map<String, Object>> getTableIndexes(Datasource datasource, String tableName) {
        // 解析表名，支持schema.table格式
        String schemaName = "public";
        String actualTableName = tableName;
        
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.", 2);
            schemaName = parts[0];
            actualTableName = parts[1];
        }
        
        String sql = "SELECT " +
                     "i.relname AS INDEX_NAME, " +
                     "t.relname AS TABLE_NAME, " +
                     "a.attname AS COLUMN_NAME, " +
                     "CASE WHEN ix.indisunique THEN 0 ELSE 1 END AS NON_UNIQUE, " +
                     "a.attnum AS SEQ_IN_INDEX, " +
                     "am.amname AS INDEX_TYPE, " +
                     "obj_description(i.oid, 'pg_class') AS INDEX_COMMENT, " +
                     "NULL AS SUB_PART " +
                     "FROM pg_class t " +
                     "JOIN pg_namespace n ON t.relnamespace = n.oid " +
                     "JOIN pg_index ix ON t.oid = ix.indrelid " +
                     "JOIN pg_class i ON i.oid = ix.indexrelid " +
                     "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(ix.indkey) " +
                     "JOIN pg_am am ON i.relam = am.oid " +
                     "WHERE t.relname = ? AND n.nspname = ? AND t.relkind = 'r' " +
                     "ORDER BY i.relname, a.attnum";
        
        return executeQuery(datasource, sql, actualTableName, schemaName);
    }
}