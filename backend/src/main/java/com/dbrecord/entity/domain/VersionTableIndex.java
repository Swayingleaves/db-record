package com.dbrecord.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 版本表索引表
 * @TableName version_table_index
 */
@TableName(value = "version_table_index")
@Data
public class VersionTableIndex implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 版本表结构ID
     */
    private Long versionTableId;

    /**
     * 索引名
     */
    private String indexName;

    /**
     * 索引类型(BTREE/HASH/FULLTEXT)
     */
    private String indexType;

    /**
     * 是否唯一索引
     */
    private Boolean isUnique;

    /**
     * 是否主键索引
     */
    private Boolean isPrimary;

    /**
     * 索引字段名(JSON数组)
     */
    private String columnNames;

    /**
     * 子部分长度(JSON数组)
     */
    private String subPart;

    /**
     * 索引注释
     */
    private String indexComment;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    
    // 手动添加getter方法以解决编译问题
    public Boolean getIsPrimary() {
        return isPrimary;
    }
    
    public Boolean getIsUnique() {
        return isUnique;
    }
    
    public String getIndexName() {
        return indexName;
    }
    
    public String getColumnNames() {
        return columnNames;
    }
    
    public String getIndexType() {
        return indexType;
    }
    
    public String getSubPart() {
        return subPart;
    }
    
    public String getIndexComment() {
        return indexComment;
    }
    
    public void setVersionTableId(Long versionTableId) {
        this.versionTableId = versionTableId;
    }
    
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
    
    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }
    
    public void setIsUnique(Boolean isUnique) {
        this.isUnique = isUnique;
    }
    
    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    
    public void setColumnNames(String columnNames) {
        this.columnNames = columnNames;
    }
    
    public void setSubPart(String subPart) {
        this.subPart = subPart;
    }
    
    public void setIndexComment(String indexComment) {
        this.indexComment = indexComment;
    }
}