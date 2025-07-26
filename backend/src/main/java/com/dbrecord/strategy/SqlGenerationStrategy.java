package com.dbrecord.strategy;

import com.dbrecord.entity.domain.VersionTableColumn;
import com.dbrecord.entity.domain.VersionTableIndex;
import com.dbrecord.entity.domain.VersionTableStructure;

import java.util.List;
import java.util.Map;

/**
 * SQL生成策略接口
 * 用于根据不同数据库类型生成相应的SQL语句
 */
public interface SqlGenerationStrategy {
    
    /**
     * 生成CREATE TABLE语句
     * @param table 表结构信息
     * @param columns 字段列表
     * @param indexes 索引列表
     * @return CREATE TABLE SQL语句
     */
    String generateCreateTableSql(VersionTableStructure table, List<VersionTableColumn> columns, List<VersionTableIndex> indexes);
    
    /**
     * 生成DROP TABLE语句
     * @param tableName 表名
     * @return DROP TABLE SQL语句
     */
    String generateDropTableSql(String tableName);
    
    /**
     * 生成ALTER TABLE语句
     * @param tableName 表名
     * @param tableChanges 表变更信息
     * @return ALTER TABLE SQL语句
     */
    String generateAlterTableSql(String tableName, Map<String, Object> tableChanges);
    
    /**
     * 获取数据库类型
     * @return 数据库类型标识
     */
    String getDatabaseType();
    
    /**
     * 格式化字段类型
     * @param columnType 原始字段类型
     * @param extra 额外信息（如AUTO_INCREMENT）
     * @return 格式化后的字段类型
     */
    String formatColumnType(String columnType, String extra);
    
    /**
     * 格式化表名或字段名
     * @param name 名称
     * @return 格式化后的名称（加引号等）
     */
    String formatIdentifier(String name);
    
    /**
     * 生成索引定义SQL
     * @param index 索引信息
     * @return 索引定义SQL
     */
    String generateIndexDefinition(VersionTableIndex index);
}