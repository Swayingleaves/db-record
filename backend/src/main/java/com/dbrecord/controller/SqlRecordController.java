package com.dbrecord.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dbrecord.entity.domain.SqlRecord;
import com.dbrecord.entity.domain.User;
import com.dbrecord.service.SqlRecordService;
import com.dbrecord.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SQL执行记录控制器
 */
@RestController
@RequestMapping("/api/sql-record")
public class SqlRecordController {
    
    @Autowired
    private SqlRecordService sqlRecordService;
    
    /**
     * 获取SQL执行记录列表
     */
    @GetMapping("/list/{projectId}")
    public Result<List<SqlRecord>> list(@PathVariable Long projectId, 
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "20") Integer size) {
        try {
            User currentUser = getCurrentUser();
            QueryWrapper<SqlRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("project_id", projectId);
            queryWrapper.eq("user_id", currentUser.getId());
            queryWrapper.orderByDesc("create_time");
            
            // 简单分页（实际项目中建议使用MyBatis-Plus的分页插件）
            int offset = (page - 1) * size;
            queryWrapper.last("LIMIT " + offset + ", " + size);
            
            List<SqlRecord> records = sqlRecordService.list(queryWrapper);
            return Result.success(records);
        } catch (Exception e) {
            return Result.error("获取SQL记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行SQL并记录
     */
    @PostMapping("/execute")
    public Result<String> execute(@RequestBody Map<String, Object> request) {
        try {
            User currentUser = getCurrentUser();
            Long projectId = Long.valueOf(request.get("projectId").toString());
            String sqlContent = request.get("sqlContent").toString();
            
            // 创建SQL记录
            SqlRecord sqlRecord = new SqlRecord();
            sqlRecord.setProjectId(projectId);
            sqlRecord.setSqlContent(sqlContent);
            sqlRecord.setUserId(currentUser.getId());
            
            long startTime = System.currentTimeMillis();
            
            try {
                // TODO: 实现SQL执行逻辑
                // 1. 根据projectId获取数据源信息
                // 2. 建立数据库连接
                // 3. 执行SQL语句
                // 4. 返回执行结果
                
                // 模拟SQL执行
                String executeResult = "SQL执行成功（模拟结果）";
                long executeTime = System.currentTimeMillis() - startTime;
                
                sqlRecord.setExecuteResult(executeResult);
                sqlRecord.setExecuteStatus(1); // 成功
                sqlRecord.setExecuteTime(executeTime);
                
                // 保存执行记录
                sqlRecordService.save(sqlRecord);
                
                return Result.success(executeResult, "SQL执行成功");
                
            } catch (Exception e) {
                long executeTime = System.currentTimeMillis() - startTime;
                sqlRecord.setExecuteStatus(0); // 失败
                sqlRecord.setExecuteTime(executeTime);
                sqlRecord.setErrorMessage(e.getMessage());
                
            // 保存执行记录
            sqlRecordService.save(sqlRecord);
                
                return Result.error("SQL执行失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            return Result.error("SQL执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取SQL记录详情
     */
    @GetMapping("/detail/{id}")
    public Result<SqlRecord> detail(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            SqlRecord record = sqlRecordService.getById(id);
            if (record == null || !record.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "记录不存在或无权限查看");
            }
            
            return Result.success(record);
        } catch (Exception e) {
            return Result.error("获取记录详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除SQL记录
     */
    @DeleteMapping("/delete/{id}")
    public Result<Object> delete(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            SqlRecord record = sqlRecordService.getById(id);
            if (record == null || !record.getUserId().equals(currentUser.getId())) {
                return Result.error(403, "记录不存在或无权限删除");
            }
            
            boolean success = sqlRecordService.removeById(id);
            if (success) {
                return Result.success("记录删除成功");
            } else {
                return Result.error("记录删除失败");
            }
        } catch (Exception e) {
            return Result.error("记录删除失败: " + e.getMessage());
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