package com.dbrecord.service.impl;

import com.dbrecord.entity.domain.Datasource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 人大金仓数据库结构提取器
 * KingbaseES 基于 PostgreSQL，所以大部分逻辑可以复用 PostgreSQL 的实现
 */
@Component
public class KingbaseDatabaseSchemaExtractor extends AbstractDatabaseSchemaExtractor {

    @Override
    protected String buildConnectionUrl(Datasource datasource) {
        return String.format("jdbc:kingbase8://%s:%d/%s",
                datasource.getHost(), datasource.getPort(), datasource.getDatabaseName());
    }

    @Override
    protected String getDatabaseType() {
        return "kingbase";
    }
    
    @Override
    public Map<String, Object> getDatabaseInfo(Datasource datasource) {
        Map<String, Object> databaseInfo = new HashMap<>();
        
        // 获取数据库基本信息
        String sql = "SELECT " +
                     "current_database() AS database_name, " +
                     "pg_encoding_to_char(encoding) AS charset, " +
                     "datcollate AS collation " +
                     "FROM pg_database WHERE datname = current_database()";
        
        List<Map<String, Object>> result = executeQuery(datasource, sql);
        if (!result.isEmpty()) {
            Map<String, Object> dbInfo = result.get(0);
            databaseInfo.put("databaseName", dbInfo.get("database_name"));
            databaseInfo.put("charset", dbInfo.get("charset"));
            databaseInfo.put("collation", dbInfo.get("collation"));
        }
        
        // 获取 schema 信息，使用配置过滤系统 schema
        var filterConfig = getFilterConfig();
        String schemasSql = "SELECT " +
                           "schema_name, " +
                           "schema_owner, " +
                           "obj_description(n.oid, 'pg_namespace') AS schema_comment " +
                           "FROM information_schema.schemata s " +
                           "LEFT JOIN pg_namespace n ON n.nspname = s.schema_name " +
                           "WHERE schema_name NOT IN (" + filterConfig.getExcludedSchemasForSql() + ") " +
                           "ORDER BY schema_name";
        
        List<Map<String, Object>> schemas = executeQuery(datasource, schemasSql);
        // 使用与PostgreSQL一致的key名称
        databaseInfo.put("schemas_info", schemas);

        return databaseInfo;
    }

    @Override
    public List<Map<String, Object>> getTablesStructure(Datasource datasource) {
        var filterConfig = getFilterConfig();
        String sql = "SELECT " +
                     "t.table_schema AS schema_name, " +
                     "t.table_name, " +
                     "t.table_type, " +
                     "obj_description(c.oid, 'pg_class') AS table_comment, " +
                     "'kingbase' as engine, " +
                     "COALESCE(s.n_tup_ins, 0) as table_rows, " +
                     "COALESCE(pg_total_relation_size(c.oid), 0) as data_length, " +
                     "0 as index_length, " +
                     "NULL as create_time, NULL as update_time " +
                     "FROM information_schema.tables t " +
                     "LEFT JOIN pg_class c ON c.relname = t.table_name AND c.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = t.table_schema) " +
                     "LEFT JOIN pg_stat_user_tables s ON s.relname = t.table_name AND s.schemaname = t.table_schema " +
                     "WHERE t.table_schema NOT IN (" + filterConfig.getExcludedSchemasForSql() + ") " +
                     "AND t.table_type = 'BASE TABLE' " +
                     "ORDER BY t.table_schema, t.table_name";

        List<Map<String, Object>> tables = executeQuery(datasource, sql);

        // 应用表级别的过滤
        return tables.stream()
                .filter(table -> {
                    String schemaName = (String) table.get("schema_name");
                    String tableName = (String) table.get("table_name");
                    return !filterConfig.isTableExcluded(schemaName, tableName);
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> getTableColumns(Datasource datasource, String schemaName, String tableName) {
        // 如果 schemaName 为空，使用默认值
        if (schemaName == null || schemaName.isEmpty()) {
            schemaName = "public";
        }
        
        String sql = "SELECT " +
                     "c.column_name, " +
                     "c.ordinal_position, " +
                     "c.column_default, " +
                     "c.is_nullable, " +
                     "c.data_type, " +
                     "c.character_maximum_length, " +
                     "c.character_octet_length, " +
                     "c.numeric_precision, " +
                     "c.numeric_scale, " +
                     "c.datetime_precision, " +
                     "c.character_set_name, " +
                     "c.collation_name, " +
                     "CASE " +
                     "  WHEN c.data_type = 'character varying' THEN 'varchar(' || c.character_maximum_length || ')' " +
                     "  WHEN c.data_type = 'character' THEN 'char(' || c.character_maximum_length || ')' " +
                     "  WHEN c.data_type = 'numeric' AND c.numeric_precision IS NOT NULL AND c.numeric_scale IS NOT NULL THEN 'numeric(' || c.numeric_precision || ',' || c.numeric_scale || ')' " +
                     "  WHEN c.data_type = 'numeric' AND c.numeric_precision IS NOT NULL THEN 'numeric(' || c.numeric_precision || ')' " +
                     "  WHEN c.data_type = 'timestamp without time zone' THEN 'timestamp' " +
                     "  WHEN c.data_type = 'timestamp with time zone' THEN 'timestamptz' " +
                     "  WHEN c.data_type = 'time without time zone' THEN 'time' " +
                     "  WHEN c.data_type = 'time with time zone' THEN 'timetz' " +
                     "  ELSE c.data_type " +
                     "END AS column_type, " +
                     "CASE " +
                     "  WHEN tc.constraint_type = 'PRIMARY KEY' THEN 'PRI' " +
                     "  WHEN tc.constraint_type = 'UNIQUE' THEN 'UNI' " +
                     "  WHEN tc.constraint_type = 'FOREIGN KEY' THEN 'MUL' " +
                     "  ELSE '' " +
                     "END AS column_key, " +
                     "CASE " +
                     "  WHEN c.column_default LIKE 'nextval%' THEN 'auto_increment' " +
                     "  ELSE '' " +
                     "END AS extra, " +
                     "COALESCE(pgd.description, '') AS column_comment " +
                     "FROM information_schema.columns c " +
                     "LEFT JOIN information_schema.key_column_usage kcu ON " +
                     "  c.table_schema = kcu.table_schema AND " +
                     "  c.table_name = kcu.table_name AND " +
                     "  c.column_name = kcu.column_name " +
                     "LEFT JOIN information_schema.table_constraints tc ON " +
                     "  kcu.constraint_name = tc.constraint_name AND " +
                     "  kcu.table_schema = tc.table_schema " +
                     "LEFT JOIN pg_class pgc ON pgc.relname = c.table_name " +
                     "LEFT JOIN pg_namespace pgn ON pgn.oid = pgc.relnamespace AND pgn.nspname = c.table_schema " +
                     "LEFT JOIN pg_attribute pga ON pga.attrelid = pgc.oid AND pga.attname = c.column_name " +
                     "LEFT JOIN pg_description pgd ON pgd.objoid = pgc.oid AND pgd.objsubid = pga.attnum " +
                     "WHERE c.table_schema = ? AND c.table_name = ? " +
                     "ORDER BY c.ordinal_position";
        return executeQuery(datasource, sql, schemaName, tableName);
    }

    @Override
    public List<Map<String, Object>> getTableIndexes(Datasource datasource, String schemaName, String tableName) {
        // 如果 schemaName 为空，使用默认值
        if (schemaName == null || schemaName.isEmpty()) {
            schemaName = "public";
        }
        
        // 优化索引查询，使用更高效的SQL
        String sql = "SELECT " +
                     "i.relname AS index_name, " +
                     "a.attname AS column_name, " +
                     "am.amname AS index_type, " +
                     "ix.indisunique AS is_unique, " +
                     "ix.indisprimary AS is_primary, " +
                     "obj_description(i.oid, 'pg_class') AS index_comment " +
                     "FROM pg_class t " +
                     "JOIN pg_namespace n ON t.relnamespace = n.oid " +
                     "JOIN pg_index ix ON t.oid = ix.indrelid " +
                     "JOIN pg_class i ON i.oid = ix.indexrelid " +
                     "JOIN pg_am am ON i.relam = am.oid " +
                     "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(ix.indkey) " +
                     "WHERE t.relname = ? AND n.nspname = ? AND t.relkind = 'r' " +
                     "ORDER BY i.relname, a.attnum";

        List<Map<String, Object>> result = executeQuery(datasource, sql, tableName, schemaName);
        
        // 确保返回的字段名与数据库映射一致
        for (Map<String, Object> row : result) {
            // 重命名字段以匹配数据库表结构
            if (row.containsKey("index_name")) {
                row.put("INDEX_NAME", row.get("index_name"));
            }
            if (row.containsKey("index_type")) {
                row.put("INDEX_TYPE", row.get("index_type"));
            }
            if (row.containsKey("is_unique")) {
                row.put("IS_UNIQUE", row.get("is_unique"));
            }
            if (row.containsKey("is_primary")) {
                row.put("IS_PRIMARY", row.get("is_primary"));
            }
            if (row.containsKey("column_name")) {
                row.put("COLUMN_NAME", row.get("column_name"));
            }
            if (row.containsKey("index_comment")) {
                row.put("INDEX_COMMENT", row.get("index_comment"));
            }
        }
        
        return result;
    }
}
