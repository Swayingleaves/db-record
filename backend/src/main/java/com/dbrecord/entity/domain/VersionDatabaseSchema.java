package com.dbrecord.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 版本数据库结构表
 * @TableName version_database_schema
 */
@TableName(value = "version_database_schema")
@Data
public class VersionDatabaseSchema implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目版本ID
     */
    private Long projectVersionId;

    /**
     * 数据库名称
     */
    private String databaseName;

    /**
     * 字符集
     */
    private String charset;

    /**
     * 排序规则
     */
    private String collation;

    /**
     * Schema信息（PostgreSQL专用，存储所有schema的详细信息）
     */
    private String schemasInfo;

    /**
     * 快照时间
     */
    private LocalDateTime snapshotTime;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    
    // 手动添加getter方法以解决编译问题
    public String getDatabaseName() {
        return databaseName;
    }
    
    public String getCharset() {
        return charset;
    }
    
    public String getCollation() {
        return collation;
    }
    
    public String getSchemasInfo() {
        return schemasInfo;
    }
    
    public LocalDateTime getSnapshotTime() {
        return snapshotTime;
    }
    
    public void setProjectVersionId(Long projectVersionId) {
        this.projectVersionId = projectVersionId;
    }
    
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
    
    public void setCharset(String charset) {
        this.charset = charset;
    }
    
    public void setCollation(String collation) {
        this.collation = collation;
    }
    
    public void setSchemasInfo(String schemasInfo) {
        this.schemasInfo = schemasInfo;
    }
    
    public void setSnapshotTime(LocalDateTime snapshotTime) {
        this.snapshotTime = snapshotTime;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}