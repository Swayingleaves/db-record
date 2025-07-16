package com.dbrecord.service;

import com.dbrecord.entity.domain.Datasource;
import com.dbrecord.entity.domain.VersionDatabaseSchema;
import com.dbrecord.entity.domain.VersionTableStructure;

import java.util.List;
import java.util.Map;

/**
 * 数据库结构服务接口
 */
public interface DatabaseSchemaService {
    
    /**
     * 获取数据库结构信息并保存到版本表中
     * @param projectVersionId 项目版本ID
     * @param datasource 数据源信息
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean captureAndSaveDatabaseSchema(Long projectVersionId, Datasource datasource, Long userId);
    
    /**
     * 获取数据库基本信息
     * @param datasource 数据源信息
     * @return 数据库信息
     */
    Map<String, Object> getDatabaseInfo(Datasource datasource);
    
    /**
     * 获取数据库中所有表的结构信息
     * @param datasource 数据源信息
     * @return 表结构信息列表
     */
    List<Map<String, Object>> getTablesStructure(Datasource datasource);
    
    /**
     * 获取指定表的字段信息
     * @param datasource 数据源信息
     * @param tableName 表名
     * @return 字段信息列表
     */
    List<Map<String, Object>> getTableColumns(Datasource datasource, String tableName);
    
    /**
     * 获取指定表的索引信息
     * @param datasource 数据源信息
     * @param tableName 表名
     * @return 索引信息列表
     */
    List<Map<String, Object>> getTableIndexes(Datasource datasource, String tableName);
    
    /**
     * 根据项目版本ID获取数据库结构
     * @param projectVersionId 项目版本ID
     * @return 版本数据库结构
     */
    VersionDatabaseSchema getVersionDatabaseSchema(Long projectVersionId);
    
    /**
     * 根据项目版本ID获取表结构列表
     * @param projectVersionId 项目版本ID
     * @return 表结构列表
     */
    List<VersionTableStructure> getVersionTableStructures(Long projectVersionId);
    
    /**
     * 对比两个版本的数据库结构差异
     * @param fromVersionId 源版本ID
     * @param toVersionId 目标版本ID
     * @return 差异信息
     */
    Map<String, Object> compareVersions(Long fromVersionId, Long toVersionId);
} 