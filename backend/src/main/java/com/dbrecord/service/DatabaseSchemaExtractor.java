package com.dbrecord.service;

import com.dbrecord.entity.domain.Datasource;

import java.util.List;
import java.util.Map;

/**
 * 数据库结构提取器接口
 */
public interface DatabaseSchemaExtractor {
    
    /**
     * 获取数据库基本信息
     * @param datasource 数据源
     * @return 数据库信息
     */
    Map<String, Object> getDatabaseInfo(Datasource datasource);
    
    /**
     * 获取所有表的结构信息
     * @param datasource 数据源
     * @return 表结构信息列表
     */
    List<Map<String, Object>> getTablesStructure(Datasource datasource);
    
    /**
     * 获取指定表的字段信息
     * @param datasource 数据源
     * @param tableName 表名
     * @return 字段信息列表
     */
    List<Map<String, Object>> getTableColumns(Datasource datasource, String tableName);
    
    /**
     * 获取指定表的索引信息
     * @param datasource 数据源
     * @param tableName 表名
     * @return 索引信息列表
     */
    List<Map<String, Object>> getTableIndexes(Datasource datasource, String tableName);
    
    /**
     * 获取数据库连接
     * @param datasource 数据源
     * @return 数据库连接
     * @throws Exception 连接异常
     */
    java.sql.Connection getConnection(Datasource datasource) throws Exception;
}