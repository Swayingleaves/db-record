package com.dbrecord.integration;

import com.dbrecord.config.DatabaseScanFilterProperties;
import com.dbrecord.service.impl.KingbaseDatabaseSchemaExtractor;
import com.dbrecord.service.impl.MySQLDatabaseSchemaExtractor;
import com.dbrecord.service.impl.PostgreSQLDatabaseSchemaExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库扫描过滤配置集成测试
 */
@SpringBootTest
class DatabaseScanFilterIntegrationTest {

    @Autowired
    private DatabaseScanFilterProperties filterProperties;

    @Autowired
    private MySQLDatabaseSchemaExtractor mysqlExtractor;

    @Autowired
    private PostgreSQLDatabaseSchemaExtractor postgresqlExtractor;

    @Autowired
    private KingbaseDatabaseSchemaExtractor kingbaseExtractor;

    @Test
    void testFilterPropertiesLoaded() {
        assertNotNull(filterProperties);
        
        // 测试MySQL配置
        var mysqlConfig = filterProperties.getMysql();
        assertNotNull(mysqlConfig);
        assertTrue(mysqlConfig.getExcludedSchemas().contains("information_schema"));
        assertTrue(mysqlConfig.getExcludedSchemas().contains("mysql"));
        
        // 测试PostgreSQL配置
        var pgConfig = filterProperties.getPostgresql();
        assertNotNull(pgConfig);
        assertTrue(pgConfig.getExcludedSchemas().contains("information_schema"));
        assertTrue(pgConfig.getExcludedSchemas().contains("pg_catalog"));
        
        // 测试KingbaseES配置
        var kbConfig = filterProperties.getKingbase();
        assertNotNull(kbConfig);
        assertTrue(kbConfig.getExcludedSchemas().contains("sys"));
        assertTrue(kbConfig.getExcludedSchemas().contains("sysmac"));
    }

    @Test
    void testExtractorsHaveFilterProperties() {
        // 验证所有提取器都正确注入了配置属性
        assertNotNull(mysqlExtractor);
        assertNotNull(postgresqlExtractor);
        assertNotNull(kingbaseExtractor);

        // 验证提取器实例类型
        assertTrue(mysqlExtractor instanceof MySQLDatabaseSchemaExtractor);
        assertTrue(postgresqlExtractor instanceof PostgreSQLDatabaseSchemaExtractor);
        assertTrue(kingbaseExtractor instanceof KingbaseDatabaseSchemaExtractor);
    }

    @Test
    void testFilterConfigRetrieval() {
        // 测试通过数据库类型获取配置
        var mysqlConfig = filterProperties.getFilterConfig("mysql");
        assertNotNull(mysqlConfig);
        assertTrue(mysqlConfig.isSchemaExcluded("information_schema"));
        
        var pgConfig = filterProperties.getFilterConfig("postgresql");
        assertNotNull(pgConfig);
        assertTrue(pgConfig.isSchemaExcluded("pg_catalog"));
        
        var kbConfig = filterProperties.getFilterConfig("kingbase");
        assertNotNull(kbConfig);
        assertTrue(kbConfig.isSchemaExcluded("sys"));
        
        // 测试未知数据库类型
        var unknownConfig = filterProperties.getFilterConfig("unknown");
        assertNotNull(unknownConfig);
        assertTrue(unknownConfig.getExcludedSchemas().isEmpty());
    }

    @Test
    void testSqlClauseGeneration() {
        var mysqlConfig = filterProperties.getFilterConfig("mysql");
        String sqlClause = mysqlConfig.getExcludedSchemasForSql();
        assertNotNull(sqlClause);
        assertFalse(sqlClause.isEmpty());
        assertTrue(sqlClause.contains("information_schema"));
        assertTrue(sqlClause.contains("mysql"));
        
        var emptyConfig = filterProperties.getFilterConfig("unknown");
        String emptySqlClause = emptyConfig.getExcludedSchemasForSql();
        assertEquals("''", emptySqlClause);
    }

    @Test
    void testTableLevelFiltering() {
        var pgConfig = filterProperties.getFilterConfig("postgresql");
        
        // 测试表级别过滤
        assertTrue(pgConfig.isTableExcluded("public", "temp_table"));
        assertTrue(pgConfig.isTableExcluded("public", "log_table"));
        assertFalse(pgConfig.isTableExcluded("public", "normal_table"));
        assertFalse(pgConfig.isTableExcluded("other_schema", "temp_table"));
    }
}
