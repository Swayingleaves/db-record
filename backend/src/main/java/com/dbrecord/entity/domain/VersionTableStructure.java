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
} 