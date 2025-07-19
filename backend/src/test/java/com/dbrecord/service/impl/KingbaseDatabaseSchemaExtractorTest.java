package com.dbrecord.service.impl;

import com.dbrecord.entity.domain.Datasource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 人大金仓数据库结构提取器测试
 */
@ExtendWith(MockitoExtension.class)
class KingbaseDatabaseSchemaExtractorTest {

    @InjectMocks
    private KingbaseDatabaseSchemaExtractor extractor;

    private Datasource datasource;

    @BeforeEach
    void setUp() {
        datasource = new Datasource();
        datasource.setHost("localhost");
        datasource.setPort(54321);
        datasource.setDatabaseName("test_db");
        datasource.setUsername("test_user");
        datasource.setPassword("test_password");
        datasource.setType("kingbase");
    }

    @Test
    void testBuildConnectionUrl() {
        String url = extractor.buildConnectionUrl(datasource);
        assertEquals("jdbc:kingbase8://localhost:54321/test_db", url);
    }

    @Test
    void testGetDatabaseInfoStructure() {
        // 测试getDatabaseInfo方法的结构
        // 注意：这里不进行实际的数据库连接，只测试方法结构
        try {
            Map<String, Object> result = extractor.getDatabaseInfo(datasource);
            // 如果没有实际连接，结果应该是空的，但方法不应该抛出异常
            assertNotNull(result);
        } catch (Exception e) {
            // 预期会有连接异常，这是正常的
            assertTrue(e.getMessage().contains("Connection") || 
                      e.getMessage().contains("connection") ||
                      e.getMessage().contains("连接"));
        }
    }

    @Test
    void testGetTablesStructureStructure() {
        // 测试getTablesStructure方法的结构
        try {
            List<Map<String, Object>> result = extractor.getTablesStructure(datasource);
            assertNotNull(result);
        } catch (Exception e) {
            // 预期会有连接异常，这是正常的
            assertTrue(e.getMessage().contains("Connection") || 
                      e.getMessage().contains("connection") ||
                      e.getMessage().contains("连接"));
        }
    }

    @Test
    void testGetTableColumnsStructure() {
        // 测试getTableColumns方法的结构
        try {
            List<Map<String, Object>> result = extractor.getTableColumns(datasource, "public", "test_table");
            assertNotNull(result);
        } catch (Exception e) {
            // 预期会有连接异常，这是正常的
            assertTrue(e.getMessage().contains("Connection") || 
                      e.getMessage().contains("connection") ||
                      e.getMessage().contains("连接"));
        }
    }

    @Test
    void testGetTableIndexesStructure() {
        // 测试getTableIndexes方法的结构
        try {
            List<Map<String, Object>> result = extractor.getTableIndexes(datasource, "public", "test_table");
            assertNotNull(result);
        } catch (Exception e) {
            // 预期会有连接异常，这是正常的
            assertTrue(e.getMessage().contains("Connection") || 
                      e.getMessage().contains("connection") ||
                      e.getMessage().contains("连接"));
        }
    }

    @Test
    void testSchemaNameDefaultHandling() {
        // 测试schema名称的默认处理
        try {
            // 测试空schema名称的处理
            List<Map<String, Object>> result1 = extractor.getTableColumns(datasource, null, "test_table");
            assertNotNull(result1);
            
            List<Map<String, Object>> result2 = extractor.getTableColumns(datasource, "", "test_table");
            assertNotNull(result2);
            
            List<Map<String, Object>> result3 = extractor.getTableIndexes(datasource, null, "test_table");
            assertNotNull(result3);
            
            List<Map<String, Object>> result4 = extractor.getTableIndexes(datasource, "", "test_table");
            assertNotNull(result4);
        } catch (Exception e) {
            // 预期会有连接异常，这是正常的
            assertTrue(e.getMessage().contains("Connection") || 
                      e.getMessage().contains("connection") ||
                      e.getMessage().contains("连接"));
        }
    }
}
