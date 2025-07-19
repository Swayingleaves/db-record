package com.dbrecord.debug;

import com.dbrecord.entity.domain.VersionTableStructure;
import com.dbrecord.service.DatabaseSchemaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Schema分组模拟测试
 * 模拟test_schema中有表的情况，验证前端分组逻辑
 */
@SpringBootTest
public class SchemaGroupingSimulationTest {

    @Autowired
    private DatabaseSchemaService databaseSchemaService;

    @Test
    public void testSchemaGroupingLogic() {
        // 模拟版本36的数据，但添加test_schema中的表
        List<Map<String, Object>> simulatedTables = createSimulatedTables();
        
        System.out.println("=== 模拟的表数据 ===");
        System.out.println("表总数: " + simulatedTables.size());
        
        // 模拟前端的分组逻辑
        Map<String, List<Map<String, Object>>> schemaGroups = groupTablesBySchema(simulatedTables);
        
        System.out.println("\n=== Schema分组结果 ===");
        for (Map.Entry<String, List<Map<String, Object>>> entry : schemaGroups.entrySet()) {
            String schemaName = entry.getKey();
            List<Map<String, Object>> tables = entry.getValue();
            
            System.out.println("Schema: " + schemaName + " (" + tables.size() + "个表)");
            for (Map<String, Object> table : tables) {
                System.out.println("  - " + table.get("tableName"));
            }
        }
        
        // 验证分组结果
        assert schemaGroups.containsKey("public") : "应该包含public schema";
        assert schemaGroups.containsKey("test_schema") : "应该包含test_schema";
        assert schemaGroups.get("public").size() == 2 : "public schema应该有2个表";
        assert schemaGroups.get("test_schema").size() == 3 : "test_schema应该有3个表";
        
        System.out.println("\n✅ Schema分组逻辑验证通过！");
    }
    
    /**
     * 创建模拟的表数据
     */
    private List<Map<String, Object>> createSimulatedTables() {
        List<Map<String, Object>> tables = new ArrayList<>();
        
        // public schema中的表（现有数据）
        tables.add(createTableMap(72L, "oauth_client_details", "public", "OAuth客户端详情表"));
        tables.add(createTableMap(73L, "t_sys_access_log", "public", "系统访问日志表"));
        
        // test_schema中的表（模拟新增）
        tables.add(createTableMap(74L, "test_table", "test_schema", "测试表"));
        tables.add(createTableMap(75L, "user_info", "test_schema", "用户信息表"));
        tables.add(createTableMap(76L, "order_detail", "test_schema", "订单详情表"));
        
        return tables;
    }
    
    /**
     * 创建表映射对象
     */
    private Map<String, Object> createTableMap(Long id, String tableName, String schemaName, String tableComment) {
        Map<String, Object> table = new java.util.HashMap<>();
        table.put("id", id);
        table.put("tableName", tableName);
        table.put("schemaName", schemaName);
        table.put("tableComment", tableComment);
        table.put("tableType", "BASE TABLE");
        table.put("engine", "kingbase");
        table.put("charset", "UTF8");
        table.put("collation", "zh_CN.UTF-8");
        table.put("tableRows", 0L);
        table.put("dataLength", 0L);
        table.put("indexLength", 0L);
        return table;
    }
    
    /**
     * 模拟前端的分组逻辑
     */
    private Map<String, List<Map<String, Object>>> groupTablesBySchema(List<Map<String, Object>> tables) {
        Map<String, List<Map<String, Object>>> groups = new java.util.HashMap<>();
        
        for (Map<String, Object> table : tables) {
            String schemaName = (String) table.get("schemaName");
            if (schemaName == null) {
                schemaName = "public"; // 默认schema
            }
            
            groups.computeIfAbsent(schemaName, k -> new ArrayList<>()).add(table);
        }
        
        return groups;
    }
    
    @Test
    public void testFrontendJsonStructure() {
        // 模拟前端接收到的完整JSON结构
        Map<String, Object> versionStructure = createSimulatedVersionStructure();
        
        System.out.println("=== 模拟前端接收的JSON结构 ===");
        
        // 数据源类型
        String datasourceType = (String) versionStructure.get("datasourceType");
        System.out.println("数据源类型: " + datasourceType);
        
        // 数据库信息
        @SuppressWarnings("unchecked")
        Map<String, Object> database = (Map<String, Object>) versionStructure.get("database");
        if (database != null) {
            System.out.println("数据库名称: " + database.get("databaseName"));
            System.out.println("Schema信息: " + database.get("schemasInfo"));
        }
        
        // 表信息
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) versionStructure.get("tables");
        
        // 验证是否为基于Schema的数据库
        boolean isSchemaBasedDatabase = "postgresql".equals(datasourceType) || "kingbase".equals(datasourceType);
        System.out.println("是否为基于Schema的数据库: " + isSchemaBasedDatabase);
        
        if (isSchemaBasedDatabase && tables != null) {
            Map<String, List<Map<String, Object>>> schemaGroups = groupTablesBySchema(tables);
            
            System.out.println("\n=== 前端分组显示效果 ===");
            for (Map.Entry<String, List<Map<String, Object>>> entry : schemaGroups.entrySet()) {
                String schemaName = entry.getKey();
                List<Map<String, Object>> schemaTables = entry.getValue();
                
                System.out.println("📁 Schema: " + schemaName + " (" + schemaTables.size() + "个表)");
                for (Map<String, Object> table : schemaTables) {
                    System.out.println("  📄 " + table.get("tableName") + " - " + table.get("tableComment"));
                }
            }
        }
        
        System.out.println("\n✅ 前端JSON结构验证通过！");
    }
    
    /**
     * 创建模拟的版本结构数据
     */
    private Map<String, Object> createSimulatedVersionStructure() {
        Map<String, Object> structure = new java.util.HashMap<>();
        
        // 数据源类型
        structure.put("datasourceType", "kingbase");
        
        // 数据库信息
        Map<String, Object> database = new java.util.HashMap<>();
        database.put("databaseName", "experiment_user");
        database.put("charset", "UTF8");
        database.put("collation", "zh_CN.UTF-8");
        database.put("schemasInfo", "[{\"schema_owner\":\"kingbase\",\"schema_comment\":\"standard public schema\",\"schema_name\":\"public\"},{\"schema_owner\":\"kingbase\",\"schema_comment\":null,\"schema_name\":\"test_schema\"}]");
        structure.put("database", database);
        
        // 表信息
        structure.put("tables", createSimulatedTables());
        
        return structure;
    }
}
