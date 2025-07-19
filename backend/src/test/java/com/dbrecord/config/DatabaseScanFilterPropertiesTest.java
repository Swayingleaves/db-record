package com.dbrecord.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库扫描过滤配置测试
 */
@SpringBootTest(classes = DatabaseScanFilterProperties.class)
@EnableConfigurationProperties(DatabaseScanFilterProperties.class)
@TestPropertySource(properties = {
    "database.scan-filters.mysql.excluded-schemas[0]=information_schema",
    "database.scan-filters.mysql.excluded-schemas[1]=mysql",
    "database.scan-filters.mysql.excluded-tables.test_db[0]=temp_table",
    "database.scan-filters.postgresql.excluded-schemas[0]=information_schema",
    "database.scan-filters.postgresql.excluded-schemas[1]=pg_catalog",
    "database.scan-filters.postgresql.excluded-tables.public[0]=log_table",
    "database.scan-filters.kingbase.excluded-schemas[0]=sys",
    "database.scan-filters.kingbase.excluded-schemas[1]=sysmac",
    "database.scan-filters.kingbase.excluded-tables.public[0]=system_table"
})
class DatabaseScanFilterPropertiesTest {

    private DatabaseScanFilterProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DatabaseScanFilterProperties();
        
        // 手动设置测试数据，模拟配置加载
        setupMySQLConfig();
        setupPostgreSQLConfig();
        setupKingbaseConfig();
    }

    private void setupMySQLConfig() {
        DatabaseScanFilterProperties.DatabaseFilterConfig mysql = properties.getMysql();
        mysql.setExcludedSchemas(Arrays.asList("information_schema", "mysql"));
        
        Map<String, List<String>> mysqlTables = new HashMap<>();
        mysqlTables.put("test_db", Arrays.asList("temp_table"));
        mysql.setExcludedTables(mysqlTables);
    }

    private void setupPostgreSQLConfig() {
        DatabaseScanFilterProperties.DatabaseFilterConfig postgresql = properties.getPostgresql();
        postgresql.setExcludedSchemas(Arrays.asList("information_schema", "pg_catalog"));
        
        Map<String, List<String>> pgTables = new HashMap<>();
        pgTables.put("public", Arrays.asList("log_table"));
        postgresql.setExcludedTables(pgTables);
    }

    private void setupKingbaseConfig() {
        DatabaseScanFilterProperties.DatabaseFilterConfig kingbase = properties.getKingbase();
        kingbase.setExcludedSchemas(Arrays.asList("sys", "sysmac"));
        
        Map<String, List<String>> kbTables = new HashMap<>();
        kbTables.put("public", Arrays.asList("system_table"));
        kingbase.setExcludedTables(kbTables);
    }

    @Test
    void testGetFilterConfigForMySQL() {
        DatabaseScanFilterProperties.DatabaseFilterConfig config = properties.getFilterConfig("mysql");
        
        assertNotNull(config);
        assertTrue(config.isSchemaExcluded("information_schema"));
        assertTrue(config.isSchemaExcluded("mysql"));
        assertFalse(config.isSchemaExcluded("test_db"));
        
        assertTrue(config.isTableExcluded("test_db", "temp_table"));
        assertFalse(config.isTableExcluded("test_db", "normal_table"));
    }

    @Test
    void testGetFilterConfigForPostgreSQL() {
        DatabaseScanFilterProperties.DatabaseFilterConfig config = properties.getFilterConfig("postgresql");
        
        assertNotNull(config);
        assertTrue(config.isSchemaExcluded("information_schema"));
        assertTrue(config.isSchemaExcluded("pg_catalog"));
        assertFalse(config.isSchemaExcluded("public"));
        
        assertTrue(config.isTableExcluded("public", "log_table"));
        assertFalse(config.isTableExcluded("public", "normal_table"));
    }

    @Test
    void testGetFilterConfigForKingbase() {
        DatabaseScanFilterProperties.DatabaseFilterConfig config = properties.getFilterConfig("kingbase");
        
        assertNotNull(config);
        assertTrue(config.isSchemaExcluded("sys"));
        assertTrue(config.isSchemaExcluded("sysmac"));
        assertFalse(config.isSchemaExcluded("public"));
        
        assertTrue(config.isTableExcluded("public", "system_table"));
        assertFalse(config.isTableExcluded("public", "normal_table"));
    }

    @Test
    void testGetFilterConfigForUnknownDatabase() {
        DatabaseScanFilterProperties.DatabaseFilterConfig config = properties.getFilterConfig("unknown");
        
        assertNotNull(config);
        assertTrue(config.getExcludedSchemas().isEmpty());
        assertTrue(config.getExcludedTables().isEmpty());
    }

    @Test
    void testGetFilterConfigForNullDatabase() {
        DatabaseScanFilterProperties.DatabaseFilterConfig config = properties.getFilterConfig(null);
        
        assertNotNull(config);
        assertTrue(config.getExcludedSchemas().isEmpty());
        assertTrue(config.getExcludedTables().isEmpty());
    }

    @Test
    void testGetExcludedSchemasForSql() {
        DatabaseScanFilterProperties.DatabaseFilterConfig config = properties.getFilterConfig("mysql");
        String sqlClause = config.getExcludedSchemasForSql();
        
        assertEquals("'information_schema', 'mysql'", sqlClause);
    }

    @Test
    void testGetExcludedSchemasForSqlEmpty() {
        DatabaseScanFilterProperties.DatabaseFilterConfig config = properties.getFilterConfig("unknown");
        String sqlClause = config.getExcludedSchemasForSql();
        
        assertEquals("''", sqlClause);
    }

    @Test
    void testCaseInsensitiveDatabaseType() {
        DatabaseScanFilterProperties.DatabaseFilterConfig config1 = properties.getFilterConfig("MYSQL");
        DatabaseScanFilterProperties.DatabaseFilterConfig config2 = properties.getFilterConfig("mysql");
        DatabaseScanFilterProperties.DatabaseFilterConfig config3 = properties.getFilterConfig("MySQL");
        
        // 应该都返回相同的配置（MySQL配置）
        assertEquals(config1.getExcludedSchemas(), config2.getExcludedSchemas());
        assertEquals(config2.getExcludedSchemas(), config3.getExcludedSchemas());
    }
}
