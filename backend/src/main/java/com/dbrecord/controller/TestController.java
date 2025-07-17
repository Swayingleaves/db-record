package com.dbrecord.controller;

import com.dbrecord.service.impl.DatabaseSchemaServiceImpl;
import com.dbrecord.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 测试控制器
 */
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private DatabaseSchemaServiceImpl databaseSchemaService;
    
    /**
     * 测试数据库结构捕获
     */
    @PostMapping("/capture/{projectVersionId}")
    public Result<Object> testCapture(@PathVariable Long projectVersionId) {
        try {
            boolean success = databaseSchemaService.testCaptureSchema(projectVersionId);
            if (success) {
                return Result.success("数据库结构捕获成功");
            } else {
                return Result.error("数据库结构捕获失败");
            }
        } catch (Exception e) {
            return Result.error("数据库结构捕获异常: " + e.getMessage());
        }
    }
}