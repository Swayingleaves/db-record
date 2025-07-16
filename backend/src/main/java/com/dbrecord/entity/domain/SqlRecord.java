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
    
    // 手动添加getter和setter方法以解决编译问题
    public Long getUserId() {
        return userId;
    }
    
    public void setExecuteStatus(Integer executeStatus) {
        this.executeStatus = executeStatus;
    }
    
    public void setExecuteTime(Long executeTime) {
        this.executeTime = executeTime;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public void setSqlContent(String sqlContent) {
        this.sqlContent = sqlContent;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public void setExecuteResult(String executeResult) {
        this.executeResult = executeResult;
    }
}