package com.dbrecord.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dbrecord.entity.domain.Project;
import com.dbrecord.entity.domain.Datasource;
import com.dbrecord.service.ProjectService;
import com.dbrecord.service.DatasourceService;
import com.dbrecord.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.dbrecord.entity.domain.User;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 项目管理控制器
 */
@RestController
@RequestMapping("/api/project")
public class ProjectController {
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private DatasourceService datasourceService;
    
    /**
     * 获取项目列表
     */
    @GetMapping("/list")
    public Result<List<Project>> list() {
        try {
            User currentUser = getCurrentUser();
            QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", currentUser.getId());
            queryWrapper.eq("status", 1);
            queryWrapper.orderByDesc("create_time");
            
            List<Project> projects = projectService.list(queryWrapper);
            return Result.success(projects);
        } catch (Exception e) {
            return Result.error("获取项目列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建项目
     */
    @PostMapping("/create")
    public Result<Project> create(@RequestBody Project project) {
        try {
            User currentUser = getCurrentUser();
            project.setUserId(currentUser.getId());
            project.setStatus(1);
            
            boolean success = projectService.save(project);
            if (success) {
                return Result.success(project, "项目创建成功");
            } else {
                return Result.error("项目创建失败");
            }
        } catch (Exception e) {
            return Result.error("项目创建失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新项目
     */
    @PutMapping("/update")
    public Result<Object> update(@RequestBody Project project) {
        try {
            User currentUser = getCurrentUser();
            
            // 检查项目是否属于当前用户
            Project existingProject = projectService.getById(project.getId());
            if (existingProject == null || !existingProject.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "项目不存在或无权限修改");
            }
            
            boolean success = projectService.updateById(project);
            if (success) {
                return Result.success("项目更新成功");
            } else {
                return Result.error("项目更新失败");
            }
        } catch (Exception e) {
            return Result.error("项目更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除项目
     */
    @DeleteMapping("/delete/{id}")
    public Result<Object> delete(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            // 检查项目是否属于当前用户
            Project existingProject = projectService.getById(id);
            if (existingProject == null || !existingProject.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "项目不存在或无权限删除");
            }
            
            // 软删除：设置状态为0
            existingProject.setStatus(0);
            boolean success = projectService.updateById(existingProject);
            
            if (success) {
                return Result.success("项目删除成功");
            } else {
                return Result.error("项目删除失败");
            }
        } catch (Exception e) {
            return Result.error("项目删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取项目详情
     */
    @GetMapping("/detail/{id}")
    public Result<Project> detail(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            Project project = projectService.getById(id);
            if (project == null || !project.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "项目不存在或无权限查看");
            }
            
            return Result.success(project);
        } catch (Exception e) {
            return Result.error("获取项目详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取项目详情（包含关联的数据源信息）
     */
    @GetMapping("/detail-with-datasource/{id}")
    public Result<Map<String, Object>> detailWithDatasource(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            Project project = projectService.getById(id);
            if (project == null || !project.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "项目不存在或无权限查看");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("project", project);
            
            // 获取关联的数据源信息
            if (project.getDatasourceId() != null) {
                Datasource datasource = datasourceService.getById(project.getDatasourceId());
                if (datasource != null && datasource.getUserId().equals(currentUser.getId())) {
                    result.put("datasource", datasource);
                }
            }
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取项目详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 绑定数据源到项目
     */
    @PostMapping("/bind-datasource")
    public Result<Object> bindDatasource(@RequestBody Map<String, Object> params) {
        try {
            User currentUser = getCurrentUser();
            Long projectId = Long.valueOf(params.get("projectId").toString());
            Long datasourceId = Long.valueOf(params.get("datasourceId").toString());
            
            // 检查项目是否属于当前用户
            Project project = projectService.getById(projectId);
            if (project == null || !project.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "项目不存在或无权限修改");
            }
            
            // 检查数据源是否属于当前用户
            Datasource datasource = datasourceService.getById(datasourceId);
            if (datasource == null || !datasource.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "数据源不存在或无权限使用");
            }
            
            // 绑定数据源
            project.setDatasourceId(datasourceId);
            boolean success = projectService.updateById(project);
            
            if (success) {
                return Result.success("数据源绑定成功");
            } else {
                return Result.error("数据源绑定失败");
            }
        } catch (Exception e) {
            return Result.error("数据源绑定失败: " + e.getMessage());
        }
    }
    
    /**
     * 解绑项目的数据源
     */
    @PostMapping("/unbind-datasource/{projectId}")
    public Result<Object> unbindDatasource(@PathVariable Long projectId) {
        try {
            User currentUser = getCurrentUser();
            
            // 检查项目是否属于当前用户
            Project project = projectService.getById(projectId);
            if (project == null || !project.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "项目不存在或无权限修改");
            }
            
            // 解绑数据源
            project.setDatasourceId(null);
            boolean success = projectService.updateById(project);
            
            if (success) {
                return Result.success("数据源解绑成功");
            } else {
                return Result.error("数据源解绑失败");
            }
        } catch (Exception e) {
            return Result.error("数据源解绑失败: " + e.getMessage());
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