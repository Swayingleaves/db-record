package com.dbrecord.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dbrecord.entity.domain.ProjectVersion;
import com.dbrecord.entity.domain.User;
import com.dbrecord.entity.domain.Project;
import com.dbrecord.entity.domain.Datasource;
import com.dbrecord.service.ProjectVersionService;
import com.dbrecord.service.ProjectService;
import com.dbrecord.service.DatasourceService;
import com.dbrecord.service.DatabaseSchemaService;
import com.dbrecord.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目版本管理控制器
 */
@RestController
@RequestMapping("/api/project-version")
public class ProjectVersionController {
    
    @Autowired
    private ProjectVersionService projectVersionService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private DatasourceService datasourceService;
    
    @Autowired
    private DatabaseSchemaService databaseSchemaService;
    
    /**
     * 获取项目版本列表
     */
    @GetMapping("/list/{projectId}")
    public Result<List<ProjectVersion>> list(@PathVariable Long projectId) {
        try {
            User currentUser = getCurrentUser();
            QueryWrapper<ProjectVersion> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("project_id", projectId);
            queryWrapper.eq("user_id", currentUser.getId());
            queryWrapper.eq("status", 1);
            queryWrapper.orderByDesc("create_time");
            
            List<ProjectVersion> versions = projectVersionService.list(queryWrapper);
            return Result.success(versions);
        } catch (Exception e) {
            return Result.error("获取版本列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建项目版本
     */
    @PostMapping("/create")
    public Result<ProjectVersion> create(@RequestBody ProjectVersion projectVersion) {
        try {
            User currentUser = getCurrentUser();
            projectVersion.setUserId(currentUser.getId());
            projectVersion.setStatus(1);
            
            // 检查版本名称是否已存在
            QueryWrapper<ProjectVersion> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("project_id", projectVersion.getProjectId());
            queryWrapper.eq("version_name", projectVersion.getVersionName());
            queryWrapper.eq("status", 1);
            
            ProjectVersion existingVersion = projectVersionService.getOne(queryWrapper);
            if (existingVersion != null) {
                return Result.error(400, "版本名称已存在");
            }
            
            // 获取项目信息
            Project project = projectService.getById(projectVersion.getProjectId());
            if (project == null || !project.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "项目不存在或无权限访问");
            }
            
            boolean success = projectVersionService.save(projectVersion);
            if (success) {
                // 如果项目关联了数据源，自动捕获数据库结构
                if (project.getDatasourceId() != null) {
                    Datasource datasource = datasourceService.getById(project.getDatasourceId());
                    if (datasource != null && datasource.getUserId().equals(currentUser.getId())) {
                        try {
                            boolean schemaSuccess = databaseSchemaService.captureAndSaveDatabaseSchema(
                                projectVersion.getId(), datasource, currentUser.getId());
                            
                            if (schemaSuccess) {
                                return Result.success(projectVersion, "版本创建成功，数据库结构已捕获");
                            } else {
                                return Result.success(projectVersion, "版本创建成功，但数据库结构捕获失败");
                            }
                        } catch (Exception e) {
                            return Result.success(projectVersion, "版本创建成功，但数据库结构捕获失败: " + e.getMessage());
                        }
                    }
                }
                
                return Result.success(projectVersion, "版本创建成功");
            } else {
                return Result.error("版本创建失败");
            }
        } catch (Exception e) {
            return Result.error("版本创建失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新项目版本
     */
    @PutMapping("/update")
    public Result<Object> update(@RequestBody ProjectVersion projectVersion) {
        try {
            User currentUser = getCurrentUser();
            
            // 检查版本是否属于当前用户
            ProjectVersion existingVersion = projectVersionService.getById(projectVersion.getId());
            if (existingVersion == null || !existingVersion.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "版本不存在或无权限修改");
            }
            
            boolean success = projectVersionService.updateById(projectVersion);
            if (success) {
                return Result.success("版本更新成功");
            } else {
                return Result.error("版本更新失败");
            }
        } catch (Exception e) {
            return Result.error("版本更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除项目版本
     */
    @DeleteMapping("/delete/{id}")
    public Result<Object> delete(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            // 检查版本是否属于当前用户
            ProjectVersion existingVersion = projectVersionService.getById(id);
            if (existingVersion == null || !existingVersion.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "版本不存在或无权限删除");
            }
            
            // 软删除：设置状态为0
            existingVersion.setStatus(0);
            boolean success = projectVersionService.updateById(existingVersion);
            
            if (success) {
                return Result.success("版本删除成功");
            } else {
                return Result.error("版本删除失败");
            }
        } catch (Exception e) {
            return Result.error("版本删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取版本详情
     */
    @GetMapping("/detail/{id}")
    public Result<ProjectVersion> detail(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            ProjectVersion version = projectVersionService.getById(id);
            if (version == null || !version.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "版本不存在或无权限查看");
            }
            
            return Result.success(version);
        } catch (Exception e) {
            return Result.error("获取版本详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 版本对比
     */
    @GetMapping("/compare/{fromVersionId}/{toVersionId}")
    public Result<Map<String, Object>> compare(@PathVariable Long fromVersionId, @PathVariable Long toVersionId) {
        try {
            User currentUser = getCurrentUser();
            
            ProjectVersion fromVersion = projectVersionService.getById(fromVersionId);
            ProjectVersion toVersion = projectVersionService.getById(toVersionId);
            
            if (fromVersion == null || toVersion == null || 
                !fromVersion.getUserId().equals(currentUser.getId()) || 
                !toVersion.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "版本不存在或无权限访问");
            }
            
            // 使用新的数据库结构比较服务
            Map<String, Object> compareResult = databaseSchemaService.compareVersions(fromVersionId, toVersionId);
            compareResult.put("fromVersion", fromVersion.getVersionName());
            compareResult.put("toVersion", toVersion.getVersionName());
            
            return Result.success(compareResult);
        } catch (Exception e) {
            return Result.error("版本对比失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出版本SQL
     */
    @GetMapping("/export-sql/{id}")
    public Result<Map<String, Object>> exportSql(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            ProjectVersion version = projectVersionService.getById(id);
            if (version == null || !version.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "版本不存在或无权限访问");
            }
            
            // TODO: 基于新的表结构生成SQL脚本
            Map<String, Object> result = new HashMap<>();
            result.put("version", version.getVersionName());
            result.put("sql", "-- 版本 " + version.getVersionName() + " 的SQL脚本\n-- 基于新表结构的SQL导出功能待完善");
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("导出SQL失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出版本差异SQL
     */
    @GetMapping("/export-diff-sql/{fromVersionId}/{toVersionId}")
    public Result<Map<String, Object>> exportDiffSql(@PathVariable Long fromVersionId, @PathVariable Long toVersionId) {
        try {
            User currentUser = getCurrentUser();
            
            ProjectVersion fromVersion = projectVersionService.getById(fromVersionId);
            ProjectVersion toVersion = projectVersionService.getById(toVersionId);
            
            if (fromVersion == null || toVersion == null || 
                !fromVersion.getUserId().equals(currentUser.getId()) || 
                !toVersion.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "版本不存在或无权限访问");
            }
            
            // 生成差异SQL
            String diffSql = databaseSchemaService.generateDiffSql(fromVersionId, toVersionId, 
                fromVersion.getVersionName(), toVersion.getVersionName());
            
            Map<String, Object> result = new HashMap<>();
            result.put("fromVersion", fromVersion.getVersionName());
            result.put("toVersion", toVersion.getVersionName());
            result.put("sql", diffSql);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("导出差异SQL失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取版本完整结构
     */
    @GetMapping("/structure/{id}")
    public Result<Map<String, Object>> getVersionStructure(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            ProjectVersion version = projectVersionService.getById(id);
            if (version == null || !version.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "版本不存在或无权限访问");
            }
            
            Map<String, Object> structure = databaseSchemaService.getVersionCompleteStructure(id);
            structure.put("version", version);
            
            return Result.success(structure);
        } catch (Exception e) {
            return Result.error("获取版本结构失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动捕获数据库结构
     */
    @PostMapping("/capture-schema/{id}")
    public Result<Object> captureSchema(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            ProjectVersion version = projectVersionService.getById(id);
            if (version == null || !version.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "版本不存在或无权限访问");
            }
            
            // 获取项目信息
            Project project = projectService.getById(version.getProjectId());
            if (project == null || project.getDatasourceId() == null) {
                return Result.error(400, "项目未关联数据源");
            }
            
            Datasource datasource = datasourceService.getById(project.getDatasourceId());
            if (datasource == null || !datasource.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "数据源不存在或无权限访问");
            }
            
            boolean success = databaseSchemaService.captureAndSaveDatabaseSchema(
                version.getId(), datasource, currentUser.getId());
            
            if (success) {
                return Result.success("数据库结构捕获成功");
            } else {
                return Result.error("数据库结构捕获失败");
            }
        } catch (Exception e) {
            return Result.error("数据库结构捕获失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
}