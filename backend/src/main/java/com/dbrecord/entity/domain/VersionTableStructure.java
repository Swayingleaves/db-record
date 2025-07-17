package com.dbrecord.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 版本表结构表
 * @TableName version_table_structure
 */
@TableName(value = "version_table_structure")
@Data
public class VersionTableStructure implements Serializable {
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
     * 表名
     */
    private String tableName;

    /**
     * Schema名称（PostgreSQL专用）
     */
    private String schemaName;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 表类型
     */
    private String tableType;

    /**
     * 存储引擎
     */
    private String engine;

    /**
     * 字符集
     */
    private String charset;

    /**
     * 排序规则
     */
    private String collation;

    /**
     * 行格式
     */
    private String rowFormat;

    /**
     * 表行数
     */
    private Long tableRows;

    /**
     * 平均行长度
     */
    private Long avgRowLength;

    /**
     * 数据长度
     */
    private Long dataLength;

    /**
     * 索引长度
     */
    private Long indexLength;

    /**
     * 自增值
     */
    private Long autoIncrement;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    
    // 手动添加getter方法以解决编译问题
    public Long getId() {
        return id;
    }
    
    public String getEngine() {
        return engine;
    }
    
    public String getCharset() {
        return charset;
    }
    
    public String getTableComment() {
        return tableComment;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public String getSchemaName() {
        return schemaName;
    }
    
    public String getTableType() {
        return tableType;
    }
    
    public String getCollation() {
        return collation;
    }
    
    public String getRowFormat() {
        return rowFormat;
    }
    
    public Long getTableRows() {
        return tableRows;
    }
    
    public Long getAvgRowLength() {
        return avgRowLength;
    }
    
    public Long getDataLength() {
        return dataLength;
    }
    
    public Long getIndexLength() {
        return indexLength;
    }
    
    public Long getAutoIncrement() {
        return autoIncrement;
    }
    
    public Long getProjectVersionId() {
        return projectVersionId;
    }
    
    public void setProjectVersionId(Long projectVersionId) {
        this.projectVersionId = projectVersionId;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
    
    public void setTableComment(String tableComment) {
        this.tableComment = tableComment;
    }
    
    public void setTableType(String tableType) {
        this.tableType = tableType;
    }
    
    public void setEngine(String engine) {
        this.engine = engine;
    }
    
    public void setCharset(String charset) {
        this.charset = charset;
    }
    
    public void setCollation(String collation) {
        this.collation = collation;
    }
    
    public void setRowFormat(String rowFormat) {
        this.rowFormat = rowFormat;
    }
    
    public void setTableRows(Long tableRows) {
        this.tableRows = tableRows;
    }
    
    public void setAvgRowLength(Long avgRowLength) {
        this.avgRowLength = avgRowLength;
    }
    
    public void setDataLength(Long dataLength) {
        this.dataLength = dataLength;
    }
    
    public void setIndexLength(Long indexLength) {
        this.indexLength = indexLength;
    }
    
    public void setAutoIncrement(Long autoIncrement) {
        this.autoIncrement = autoIncrement;
    }
}