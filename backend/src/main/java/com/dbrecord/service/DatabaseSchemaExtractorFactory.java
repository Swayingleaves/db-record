package com.dbrecord.service;

import com.dbrecord.enums.DatabaseType;
import com.dbrecord.service.impl.MySQLDatabaseSchemaExtractor;
import com.dbrecord.service.impl.PostgreSQLDatabaseSchemaExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库结构提取器工厂
 */
@Component
public class DatabaseSchemaExtractorFactory {
    
    private final Map<DatabaseType, DatabaseSchemaExtractor> extractors = new HashMap<>();
    
    @Autowired
    public DatabaseSchemaExtractorFactory(MySQLDatabaseSchemaExtractor mysqlExtractor,
                                         PostgreSQLDatabaseSchemaExtractor postgresqlExtractor) {
        extractors.put(DatabaseType.MYSQL, mysqlExtractor);
        extractors.put(DatabaseType.POSTGRESQL, postgresqlExtractor);
    }
    
    /**
     * 根据数据库类型获取对应的提取器
     * @param databaseType 数据库类型
     * @return 数据库结构提取器
     * @throws IllegalArgumentException 不支持的数据库类型
     */
    public DatabaseSchemaExtractor getExtractor(DatabaseType databaseType) {
        DatabaseSchemaExtractor extractor = extractors.get(databaseType);
        if (extractor == null) {
            throw new IllegalArgumentException("不支持的数据库类型: " + databaseType);
        }
        return extractor;
    }
    
    /**
     * 根据数据库类型代码获取对应的提取器
     * @param databaseTypeCode 数据库类型代码
     * @return 数据库结构提取器
     * @throws IllegalArgumentException 不支持的数据库类型
     */
    public DatabaseSchemaExtractor getExtractor(String databaseTypeCode) {
        DatabaseType databaseType = DatabaseType.fromCode(databaseTypeCode);
        if (databaseType == null) {
            throw new IllegalArgumentException("不支持的数据库类型: " + databaseTypeCode);
        }
        return getExtractor(databaseType);
    }
    
    /**
     * 检查是否支持指定的数据库类型
     * @param databaseTypeCode 数据库类型代码
     * @return 是否支持
     */
    public boolean isSupported(String databaseTypeCode) {
        return DatabaseType.isSupported(databaseTypeCode);
    }
}