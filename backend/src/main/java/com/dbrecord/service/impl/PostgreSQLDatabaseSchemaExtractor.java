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
    public List<Map<String, Object>> getTableColumns(Datasource datasource, String schemaName, String tableName) {
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
        String sql = "SELECT " +
                     "i.relname AS index_name, " +
                     "t.relname AS table_name, " +
                     "string_agg(a.attname, ',' ORDER BY array_position(ix.indkey, a.attnum)) AS column_names, " +
                     "CASE WHEN ix.indisunique THEN 1 ELSE 0 END AS is_unique, " +
                     "CASE WHEN ix.indisprimary THEN 1 ELSE 0 END AS is_primary, " +
                     "am.amname AS index_type, " +
                     "obj_description(i.oid, 'pg_class') AS index_comment " +
                     "FROM pg_class t " +
                     "JOIN pg_namespace n ON t.relnamespace = n.oid " +
                     "JOIN pg_index ix ON t.oid = ix.indrelid " +
                     "JOIN pg_class i ON i.oid = ix.indexrelid " +
                     "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(ix.indkey) " +
                     "JOIN pg_am am ON i.relam = am.oid " +
                     "WHERE t.relname = ? AND n.nspname = ? AND t.relkind = 'r' " +
                     "GROUP BY i.relname, t.relname, ix.indisunique, ix.indisprimary, am.amname, i.oid " +
                     "ORDER BY i.relname";

        return executeQuery(datasource, sql, tableName, schemaName);
    }
}