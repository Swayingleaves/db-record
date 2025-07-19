package com.dbrecord.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库扫描过滤配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "database.scan-filters")
public class DatabaseScanFilterProperties {

    /**
     * MySQL过滤配置
     */
    private DatabaseFilterConfig mysql = new DatabaseFilterConfig();

    /**
     * PostgreSQL过滤配置
     */
    private DatabaseFilterConfig postgresql = new DatabaseFilterConfig();

    /**
     * KingbaseES过滤配置
     */
    private DatabaseFilterConfig kingbase = new DatabaseFilterConfig();

    /**
     * 根据数据库类型获取过滤配置
     * @param databaseType 数据库类型
     * @return 过滤配置
     */
    public DatabaseFilterConfig getFilterConfig(String databaseType) {
        if (databaseType == null) {
            return new DatabaseFilterConfig();
        }
        
        switch (databaseType.toLowerCase()) {
            case "mysql":
                return mysql;
            case "postgresql":
                return postgresql;
            case "kingbase":
                return kingbase;
            default:
                return new DatabaseFilterConfig();
        }
    }

    /**
     * 数据库过滤配置
     */
    @Data
    public static class DatabaseFilterConfig {
        /**
         * 需要排除的schema列表
         */
        private List<String> excludedSchemas = new ArrayList<>();

        /**
         * 需要排除的表配置
         * key: schema名称
         * value: 该schema下需要排除的表名列表
         */
        private Map<String, List<String>> excludedTables = new HashMap<>();

        /**
         * 检查schema是否应该被排除
         * @param schemaName schema名称
         * @return true表示应该排除
         */
        public boolean isSchemaExcluded(String schemaName) {
            return excludedSchemas.contains(schemaName);
        }

        /**
         * 检查表是否应该被排除
         * @param schemaName schema名称
         * @param tableName 表名称
         * @return true表示应该排除
         */
        public boolean isTableExcluded(String schemaName, String tableName) {
            List<String> tables = excludedTables.get(schemaName);
            return tables != null && tables.contains(tableName);
        }

        /**
         * 获取排除的schema列表（用于SQL查询）
         * @return schema列表的SQL IN子句格式
         */
        public String getExcludedSchemasForSql() {
            if (excludedSchemas.isEmpty()) {
                return "''"; // 返回空字符串，确保SQL语法正确
            }
            return "'" + String.join("', '", excludedSchemas) + "'";
        }
    }
}
