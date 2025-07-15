package com.dbrecord.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dbrecord.entity.domain.Datasource;
import com.dbrecord.entity.domain.User;
import com.dbrecord.service.DatasourceService;
import com.dbrecord.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源管理控制器
 */
@RestController
@RequestMapping("/api/datasource")
public class DatasourceController {
    
    @Autowired
    private DatasourceService datasourceService;
    
    /**
     * 获取数据源列表
     */
    @GetMapping("/list")
    public Result<List<Datasource>> list() {
        try {
            User currentUser = getCurrentUser();
            QueryWrapper<Datasource> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", currentUser.getId());
            queryWrapper.eq("status", 1);
            queryWrapper.orderByDesc("create_time");
            
            List<Datasource> datasources = datasourceService.list(queryWrapper);
            return Result.success(datasources);
        } catch (Exception e) {
            return Result.error("获取数据源列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建数据源
     */
    @PostMapping("/create")
    public Result<Datasource> create(@RequestBody Datasource datasource) {
        try {
            User currentUser = getCurrentUser();
            datasource.setUserId(currentUser.getId());
            datasource.setStatus(1);
            
            boolean success = datasourceService.save(datasource);
            if (success) {
                return Result.success(datasource, "数据源创建成功");
            } else {
                return Result.error("数据源创建失败");
            }
        } catch (Exception e) {
            return Result.error("数据源创建失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新数据源
     */
    @PutMapping("/update")
    public Result<Object> update(@RequestBody Datasource datasource) {
        try {
            User currentUser = getCurrentUser();
            
            // 检查数据源是否属于当前用户
            Datasource existingDatasource = datasourceService.getById(datasource.getId());
            if (existingDatasource == null || !existingDatasource.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "数据源不存在或无权限修改");
            }
            
            boolean success = datasourceService.updateById(datasource);
            if (success) {
                return Result.success("数据源更新成功");
            } else {
                return Result.error("数据源更新失败");
            }
        } catch (Exception e) {
            return Result.error("数据源更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除数据源
     */
    @DeleteMapping("/delete/{id}")
    public Result<Object> delete(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            // 检查数据源是否属于当前用户
            Datasource existingDatasource = datasourceService.getById(id);
            if (existingDatasource == null || !existingDatasource.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "数据源不存在或无权限删除");
            }
            
            // 软删除：设置状态为0
            existingDatasource.setStatus(0);
            boolean success = datasourceService.updateById(existingDatasource);
            
            if (success) {
                return Result.success("数据源删除成功");
            } else {
                return Result.error("数据源删除失败");
            }
        } catch (Exception e) {
            return Result.error("数据源删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取数据源详情
     */
    @GetMapping("/detail/{id}")
    public Result<Datasource> detail(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            Datasource datasource = datasourceService.getById(id);
            if (datasource == null || !datasource.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "数据源不存在或无权限查看");
            }
            
            return Result.success(datasource);
        } catch (Exception e) {
            return Result.error("获取数据源详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试数据源连接
     */
    @PostMapping("/test")
    public Result<Object> testConnection(@RequestBody Datasource datasource) {
        try {
            boolean success = datasourceService.testConnection(datasource);
            if (success) {
                return Result.success("连接测试成功");
            } else {
                return Result.error("连接测试失败，请检查配置");
            }
        } catch (Exception e) {
            return Result.error("连接测试失败: " + e.getMessage());
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