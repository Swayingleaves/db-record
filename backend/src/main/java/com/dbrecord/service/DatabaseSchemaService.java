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
    
    /**
     * 获取版本的完整数据库结构信息
     * @param projectVersionId 项目版本ID
     * @return 完整的数据库结构信息，包括数据库、表、字段、索引
     */
    Map<String, Object> getVersionCompleteStructure(Long projectVersionId);
    
    /**
     * 生成版本差异SQL
     * @param fromVersionId 源版本ID
     * @param toVersionId 目标版本ID
     * @param fromVersionName 源版本名称
     * @param toVersionName 目标版本名称
     * @return 差异SQL字符串
     */
    String generateDiffSql(Long fromVersionId, Long toVersionId, String fromVersionName, String toVersionName);
}