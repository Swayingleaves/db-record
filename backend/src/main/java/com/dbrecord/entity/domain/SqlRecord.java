package com.dbrecord.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SQL执行记录表
 * @TableName sql_record
 */
@TableName(value = "sql_record")
@Data
public class SqlRecord implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * SQL语句内容
     */
    private String sqlContent;

    /**
     * 执行结果
     */
    private String executeResult;

    /**
     * 执行状态(0失败,1成功)
     */
    private Integer executeStatus;

    /**
     * 执行耗时(毫秒)
     */
    private Long executeTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
} 