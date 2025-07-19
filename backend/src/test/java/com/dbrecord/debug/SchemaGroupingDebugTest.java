package com.dbrecord.debug;

import com.dbrecord.service.DatabaseSchemaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * Schema分组调试测试
 */
@SpringBootTest
public class SchemaGroupingDebugTest {

    @Autowired
    private DatabaseSchemaService databaseSchemaService;

    @Test
    public void testVersionStructureForVersion36() {
        try {
            // 测试版本36的结构
            Long versionId = 36L;
            Map<String, Object> structure = databaseSchemaService.getVersionCompleteStructure(versionId);
            
            System.out.println("=== 版本36结构信息 ===");
            System.out.println("数据源类型: " + structure.get("datasourceType"));
            
            // 检查数据库信息
            @SuppressWarnings("unchecked")
            Map<String, Object> database = (Map<String, Object>) structure.get("database");
            if (database != null) {
                System.out.println("数据库名称: " + database.get("databaseName"));
                System.out.println("Schema信息: " + database.get("schemasInfo"));
            }
            
            // 检查表信息
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tables = (List<Map<String, Object>>) structure.get("tables");
            if (tables != null) {
                System.out.println("表总数: " + tables.size());
                
                // 按schema分组统计
                Map<String, Integer> schemaCount = new java.util.HashMap<>();
                for (Map<String, Object> table : tables) {
                    String schemaName = (String) table.get("schemaName");
                    String tableName = (String) table.get("tableName");
                    
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
            }
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    public void testVersionTableStructures() {
        try {
            Long versionId = 36L;
            var tableStructures = databaseSchemaService.getVersionTableStructures(versionId);
            
            System.out.println("=== 直接查询表结构 ===");
            System.out.println("表结构数量: " + tableStructures.size());
            
            for (var table : tableStructures) {
                System.out.println("表ID: " + table.getId() + 
                                 ", 表名: " + table.getTableName() + 
                                 ", Schema: " + table.getSchemaName());
            }
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
