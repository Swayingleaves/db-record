package com.dbrecord.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目版本表
 * @TableName project_version
 */
@TableName(value = "project_version")
@Data
public class ProjectVersion implements Serializable {
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
     * 版本名称
     */
    private String versionName;

    /**
     * 版本描述
     */
    private String description;

    /**
     * 数据库结构快照(JSON格式)
     */
    private String schemaSnapshot;

    /**
     * 状态(0禁用,1启用)
     */
    private Integer status;

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
    public Long getUserId() {
        return userId;
    }
    
    public String getVersionName() {
        return versionName;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}