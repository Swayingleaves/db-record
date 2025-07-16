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
} 