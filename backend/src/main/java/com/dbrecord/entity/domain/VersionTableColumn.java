package com.dbrecord.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 版本表字段结构表
 * @TableName version_table_column
 */
@TableName(value = "version_table_column")
@Data
public class VersionTableColumn implements Serializable {
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
     * 字段名
     */
    private String columnName;

    /**
     * 字段位置
     */
    private Integer ordinalPosition;

    /**
     * 默认值
     */
    private String columnDefault;

    /**
     * 是否可空
     */
    private String isNullable;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 字符最大长度
     */
    private Long characterMaximumLength;

    /**
     * 字符字节长度
     */
    private Long characterOctetLength;

    /**
     * 数值精度
     */
    private Integer numericPrecision;

    /**
     * 数值小数位
     */
    private Integer numericScale;

    /**
     * 日期时间精度
     */
    private Integer datetimePrecision;

    /**
     * 字符集
     */
    private String characterSetName;

    /**
     * 排序规则
     */
    private String collationName;

    /**
     * 列类型
     */
    private String columnType;

    /**
     * 键类型(PRI/UNI/MUL)
     */
    private String columnKey;

    /**
     * 额外信息
     */
    private String extra;

    /**
     * 字段注释
     */
    private String columnComment;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    
    // 手动添加getter和setter方法以解决编译问题
    public void setVersionTableId(Long versionTableId) {
        this.versionTableId = versionTableId;
    }
    
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    
    public void setOrdinalPosition(Integer ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }
    
    public void setColumnDefault(String columnDefault) {
        this.columnDefault = columnDefault;
    }
    
    public void setIsNullable(String isNullable) {
        this.isNullable = isNullable;
    }
    
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    
    public void setCharacterMaximumLength(Long characterMaximumLength) {
        this.characterMaximumLength = characterMaximumLength;
    }
    
    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }
    
    public void setColumnKey(String columnKey) {
        this.columnKey = columnKey;
    }
    
    public void setExtra(String extra) {
        this.extra = extra;
    }
    
    public void setColumnComment(String columnComment) {
        this.columnComment = columnComment;
    }
    
    public void setCharacterOctetLength(Long characterOctetLength) {
        this.characterOctetLength = characterOctetLength;
    }
    
    public void setNumericPrecision(Integer numericPrecision) {
        this.numericPrecision = numericPrecision;
    }
    
    public void setNumericScale(Integer numericScale) {
        this.numericScale = numericScale;
    }
    
    public void setDatetimePrecision(Integer datetimePrecision) {
        this.datetimePrecision = datetimePrecision;
    }
    
    public void setCharacterSetName(String characterSetName) {
        this.characterSetName = characterSetName;
    }
    
    public void setCollationName(String collationName) {
        this.collationName = collationName;
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    public String getColumnType() {
        return columnType;
    }
    
    public String getIsNullable() {
        return isNullable;
    }
    
    public String getColumnDefault() {
        return columnDefault;
    }
    
    public String getColumnComment() {
        return columnComment;
    }
    
    public Integer getOrdinalPosition() {
        return ordinalPosition;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public Long getCharacterMaximumLength() {
        return characterMaximumLength;
    }
    
    public Long getCharacterOctetLength() {
        return characterOctetLength;
    }
    
    public Integer getNumericPrecision() {
        return numericPrecision;
    }
    
    public Integer getNumericScale() {
        return numericScale;
    }
    
    public Integer getDatetimePrecision() {
        return datetimePrecision;
    }
    
    public String getCharacterSetName() {
        return characterSetName;
    }
    
    public String getCollationName() {
        return collationName;
    }
    
    public String getColumnKey() {
        return columnKey;
    }
    
    public String getExtra() {
        return extra;
    }
}