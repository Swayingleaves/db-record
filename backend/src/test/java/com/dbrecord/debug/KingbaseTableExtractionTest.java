package com.dbrecord.debug;

import com.dbrecord.entity.domain.Datasource;
import com.dbrecord.service.impl.KingbaseDatabaseSchemaExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * 人大金仓表提取调试测试
 */
@SpringBootTest
public class KingbaseTableExtractionTest {

    @Autowired
    private KingbaseDatabaseSchemaExtractor kingbaseExtractor;

    @Test
    public void testKingbaseTableExtraction() {
        try {
            // 创建测试数据源（需要根据实际情况调整）
            Datasource datasource = new Datasource();
            datasource.setHost("localhost");
            datasource.setPort(54321);
            datasource.setDatabaseName("experiment_user");
            datasource.setUsername("kingbase");
            datasource.setPassword("123456");
            datasource.setType("kingbase");
            
            System.out.println("=== 测试人大金仓表结构提取 ===");
            
            // 测试获取数据库信息
            try {
                Map<String, Object> databaseInfo = kingbaseExtractor.getDatabaseInfo(datasource);
                System.out.println("数据库信息获取成功");
                System.out.println("Schema信息: " + databaseInfo.get("schemas_info"));
            } catch (Exception e) {
                System.err.println("获取数据库信息失败: " + e.getMessage());
            }
            
            // 测试获取表结构
            try {
                List<Map<String, Object>> tables = kingbaseExtractor.getTablesStructure(datasource);
                System.out.println("表结构获取成功，表数量: " + tables.size());
                
                // 按schema分组统计
                Map<String, Integer> schemaCount = new java.util.HashMap<>();
                for (Map<String, Object> table : tables) {
                    String schemaName = (String) table.get("schema_name");
                    String tableName = (String) table.get("table_name");
                    
                    System.out.println("表: " + tableName + ", Schema: " + schemaName);
                    
                    if (schemaName == null) {
                        schemaName = "null";
                    }
                    schemaCount.put(schemaName, schemaCount.getOrDefault(schemaName, 0) + 1);
                }
                
                System.out.println("\n=== Schema统计 ===");
                for (Map.Entry<String, Integer> entry : schemaCount.entrySet()) {
                    System.out.println("Schema: " + entry.getKey() + ", 表数量: " + entry.getValue());
                }
                
            } catch (Exception e) {
                System.err.println("获取表结构失败: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    public void testKingbaseSpecificSchemaQuery() {
        try {
            // 创建测试数据源
            Datasource datasource = new Datasource();
            datasource.setHost("localhost");
            datasource.setPort(54321);
            datasource.setDatabaseName("experiment_user");
            datasource.setUsername("kingbase");
            datasource.setPassword("123456");
            datasource.setType("kingbase");
            
            System.out.println("=== 测试特定Schema的表查询 ===");
            
            // 测试获取test_schema中的表
            try {
                List<Map<String, Object>> testSchemaColumns = kingbaseExtractor.getTableColumns(datasource, "test_schema", "test_table");
                System.out.println("test_schema.test_table 字段数量: " + testSchemaColumns.size());
                
                for (Map<String, Object> column : testSchemaColumns) {
                    System.out.println("字段: " + column.get("column_name") + ", 类型: " + column.get("data_type"));
                }
                
            } catch (Exception e) {
                System.err.println("获取test_schema表字段失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
