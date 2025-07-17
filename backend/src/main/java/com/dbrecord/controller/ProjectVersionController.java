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
            
            ProjectVersion existingActiveVersion = projectVersionService.getOne(queryWrapper);
            if (existingActiveVersion != null) {
                return Result.error(400, "版本名称已存在");
            }
            
            // 检查是否存在同名的软删除版本，如果存在则删除
            QueryWrapper<ProjectVersion> deletedQueryWrapper = new QueryWrapper<>();
            deletedQueryWrapper.eq("project_id", projectVersion.getProjectId());
            deletedQueryWrapper.eq("version_name", projectVersion.getVersionName());
            deletedQueryWrapper.eq("status", 0);
            
            ProjectVersion deletedVersion = projectVersionService.getOne(deletedQueryWrapper);
            if (deletedVersion != null) {
                // 硬删除软删除的版本记录
                projectVersionService.removeById(deletedVersion.getId());
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
            
            // 获取项目信息以确定数据源类型
            Project project = projectService.getById(version.getProjectId());
            if (project == null) {
                return Result.error("项目不存在");
            }
            
            // 获取数据源类型
            String databaseType = "mysql"; // 默认为MySQL
            if (project.getDatasourceId() != null) {
                Datasource datasource = datasourceService.getById(project.getDatasourceId());
                if (datasource != null) {
                    databaseType = datasource.getType();
                }
            }
            
            // 获取版本的完整数据库结构
            Map<String, Object> completeStructure = databaseSchemaService.getVersionCompleteStructure(id);
            
            if (completeStructure.containsKey("error")) {
                return Result.error("获取数据库结构失败: " + completeStructure.get("error"));
            }
            
            // 根据数据库类型生成SQL脚本
            String sql = generateCompleteStructureSql(id, version.getVersionName(), completeStructure, databaseType);
            
            Map<String, Object> result = new HashMap<>();
            result.put("version", version.getVersionName());
            result.put("sql", sql);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("导出SQL失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成完整的数据库结构SQL
     */
    private String generateCompleteStructureSql(Long versionId, String versionName, Map<String, Object> completeStructure, String databaseType) {
        StringBuilder sql = new StringBuilder();
        
        // 添加头部注释
        sql.append("-- 数据库结构导出\n");
        sql.append("-- 版本: ").append(versionName).append("\n");
        sql.append("-- 版本ID: ").append(versionId).append("\n");
        sql.append("-- 导出时间: ").append(new java.util.Date()).append("\n\n");
        
        // 添加数据库信息
        @SuppressWarnings("unchecked")
        Map<String, Object> databaseInfo = (Map<String, Object>) completeStructure.get("database");
        if (databaseInfo != null) {
            sql.append("-- 数据库信息\n");
            sql.append("-- 数据库名: ").append(databaseInfo.get("databaseName")).append("\n");
            sql.append("-- 字符集: ").append(databaseInfo.get("charset")).append("\n");
            sql.append("-- 排序规则: ").append(databaseInfo.get("collation")).append("\n\n");
        }
        
        // 生成表结构SQL
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) completeStructure.get("tables");
        if (tables != null && !tables.isEmpty()) {
            for (Map<String, Object> table : tables) {
                sql.append(generateCreateTableSql(table, databaseType));
                sql.append("\n");
            }
        } else {
            sql.append("-- 该版本暂无表结构数据\n");
        }
        
        return sql.toString();
    }
    
    /**
     * 将MySQL字段类型转换为PostgreSQL字段类型
     */
    private String convertColumnTypeForPostgreSQL(String mysqlType, String extra) {
        if (mysqlType == null) {
            return "TEXT";
        }
        
        String type = mysqlType.toLowerCase();
        
        // 处理AUTO_INCREMENT
        if (extra != null && extra.toLowerCase().contains("auto_increment")) {
            if (type.contains("int")) {
                if (type.contains("bigint")) {
                    return "BIGSERIAL";
                } else {
                    return "SERIAL";
                }
            }
        }
        
        // 字符串类型
        if (type.startsWith("varchar")) {
            return type.replace("varchar", "VARCHAR");
        }
        if (type.startsWith("char")) {
            return type.replace("char", "CHAR");
        }
        if (type.equals("text") || type.equals("longtext") || type.equals("mediumtext")) {
            return "TEXT";
        }
        if (type.equals("tinytext")) {
            return "TEXT";
        }
        
        // 数值类型
        if (type.equals("tinyint(1)") || type.equals("boolean")) {
            return "BOOLEAN";
        }
        if (type.startsWith("tinyint")) {
            return "SMALLINT";
        }
        if (type.startsWith("smallint")) {
            return "SMALLINT";
        }
        if (type.startsWith("mediumint") || type.startsWith("int")) {
            return "INTEGER";
        }
        if (type.startsWith("bigint")) {
            return "BIGINT";
        }
        if (type.startsWith("decimal") || type.startsWith("numeric")) {
            return type.toUpperCase();
        }
        if (type.startsWith("float")) {
            return "REAL";
        }
        if (type.startsWith("double")) {
            return "DOUBLE PRECISION";
        }
        
        // 日期时间类型
        if (type.equals("datetime")) {
            return "TIMESTAMP";
        }
        if (type.equals("date")) {
            return "DATE";
        }
        if (type.equals("time")) {
            return "TIME";
        }
        if (type.equals("timestamp")) {
            return "TIMESTAMP";
        }
        if (type.equals("year")) {
            return "INTEGER";
        }
        
        // 二进制类型
        if (type.startsWith("blob") || type.equals("longblob") || type.equals("mediumblob")) {
            return "BYTEA";
        }
        if (type.startsWith("binary") || type.startsWith("varbinary")) {
            return "BYTEA";
        }
        
        // JSON类型
        if (type.equals("json")) {
            return "JSON";
        }
        
        // 枚举和集合类型
        if (type.startsWith("enum")) {
            return "VARCHAR(255)";
        }
        if (type.startsWith("set")) {
            return "TEXT";
        }
        
        // 默认返回原类型
        return mysqlType.toUpperCase();
    }
    
    /**
     * 生成创建表的SQL语句
     */
    private String generateCreateTableSql(Map<String, Object> table, String databaseType) {
        StringBuilder sql = new StringBuilder();
        
        String tableName = (String) table.get("tableName");
        String tableComment = (String) table.get("tableComment");
        String engine = (String) table.get("engine");
        String charset = (String) table.get("charset");
        String collation = (String) table.get("collation");
        Object autoIncrement = table.get("autoIncrement");
        
        sql.append("-- 表: ").append(tableName);
        if (tableComment != null && !tableComment.isEmpty()) {
            sql.append(" (").append(tableComment).append(")");
        }
        sql.append("\n");
        
        // 根据数据库类型生成不同的语法
        if ("postgresql".equalsIgnoreCase(databaseType)) {
            sql.append("DROP TABLE IF EXISTS \"").append(tableName).append("\"\n");
            sql.append("CREATE TABLE \"").append(tableName).append("\" (\n");
        } else {
            // MySQL语法
            sql.append("DROP TABLE IF EXISTS `").append(tableName).append("`\n");
            sql.append("CREATE TABLE `").append(tableName).append("` (\n");
        }
        
        // 添加字段定义
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) table.get("columns");
        if (columns != null && !columns.isEmpty()) {
            for (int i = 0; i < columns.size(); i++) {
                Map<String, Object> column = columns.get(i);
                
                if ("postgresql".equalsIgnoreCase(databaseType)) {
                    sql.append("  \"").append(column.get("columnName")).append("\" ");
                    // PostgreSQL字段类型转换
                    sql.append(convertColumnTypeForPostgreSQL((String) column.get("columnType"), (String) column.get("extra")));
                } else {
                    // MySQL语法
                    sql.append("  `").append(column.get("columnName")).append("` ");
                    sql.append(column.get("columnType"));
                }
                
                // 处理NULL约束
                if ("NO".equals(column.get("isNullable"))) {
                    sql.append(" NOT NULL");
                }
                
                // 处理默认值
                Object defaultValue = column.get("columnDefault");
                if (defaultValue != null && !"null".equalsIgnoreCase(defaultValue.toString())) {
                    if ("postgresql".equalsIgnoreCase(databaseType)) {
                        sql.append(" DEFAULT '").append(defaultValue).append("'");
                    } else {
                        sql.append(" DEFAULT '").append(defaultValue).append("'");
                    }
                }
                
                // 处理额外属性（如AUTO_INCREMENT）
                String extra = (String) column.get("extra");
                if (extra != null && !extra.isEmpty() && !"postgresql".equalsIgnoreCase(databaseType)) {
                    // PostgreSQL的AUTO_INCREMENT已在类型转换中处理
                    sql.append(" ").append(extra.toUpperCase());
                }
                
                // 处理字段注释
                String columnComment = (String) column.get("columnComment");
                if (columnComment != null && !columnComment.isEmpty()) {
                    if ("postgresql".equalsIgnoreCase(databaseType)) {
                        // PostgreSQL注释需要单独的COMMENT语句，这里先跳过
                    } else {
                        sql.append(" COMMENT '").append(columnComment.replace("'", "\\'")).append("'");
                    }
                }
                
                if (i < columns.size() - 1) {
                    sql.append(",");
                }
                sql.append("\n");
            }
        }
        
        // 添加索引定义
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> indexes = (List<Map<String, Object>>) table.get("indexes");
        if (indexes != null && !indexes.isEmpty()) {
            for (Map<String, Object> index : indexes) {
                Boolean isPrimary = (Boolean) index.get("isPrimary");
                Boolean isUnique = (Boolean) index.get("isUnique");
                String indexName = (String) index.get("indexName");
                String columnNames = (String) index.get("columnNames");
                
                sql.append(",\n  ");
                
                if (Boolean.TRUE.equals(isPrimary)) {
                    sql.append("PRIMARY KEY (").append(columnNames).append(")");
                } else if (Boolean.TRUE.equals(isUnique)) {
                    if ("postgresql".equalsIgnoreCase(databaseType)) {
                        sql.append("UNIQUE (").append(columnNames).append(")");
                    } else {
                        sql.append("UNIQUE KEY `").append(indexName).append("` (").append(columnNames).append(")");
                    }
                } else {
                    if ("postgresql".equalsIgnoreCase(databaseType)) {
                        // PostgreSQL的普通索引需要在CREATE TABLE外部创建
                        // 这里先跳过，后面单独处理
                    } else {
                        sql.append("KEY `").append(indexName).append("` (").append(columnNames).append(")");
                    }
                }
            }
        }
        
        if ("postgresql".equalsIgnoreCase(databaseType)) {
            sql.append("\n);");
            
            // PostgreSQL表注释
            if (tableComment != null && !tableComment.isEmpty()) {
                sql.append("\nCOMMENT ON TABLE \"").append(tableName).append("\" IS '").append(tableComment.replace("'", "\\'")).append("';");
            }
            
            // PostgreSQL字段注释
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> columnsForComment = (List<Map<String, Object>>) table.get("columns");
            if (columnsForComment != null) {
                for (Map<String, Object> column : columnsForComment) {
                    String columnComment = (String) column.get("columnComment");
                    if (columnComment != null && !columnComment.isEmpty()) {
                        sql.append("\nCOMMENT ON COLUMN \"").append(tableName).append("\".\"").append(column.get("columnName")).append("\" IS '").append(columnComment.replace("'", "\\'")).append("';");
                    }
                }
            }
            
            // PostgreSQL普通索引
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> indexesForCreate = (List<Map<String, Object>>) table.get("indexes");
            if (indexesForCreate != null) {
                for (Map<String, Object> index : indexesForCreate) {
                    Boolean isPrimary = (Boolean) index.get("isPrimary");
                    Boolean isUnique = (Boolean) index.get("isUnique");
                    String indexName = (String) index.get("indexName");
                    String columnNames = (String) index.get("columnNames");
                    
                    if (!Boolean.TRUE.equals(isPrimary) && !Boolean.TRUE.equals(isUnique)) {
                        sql.append("\nCREATE INDEX \"").append(indexName).append("\" ON \"").append(tableName).append("\" (").append(columnNames).append(");");
                    }
                }
            }
        } else {
            // MySQL语法
            sql.append("\n) ENGINE=").append(engine != null ? engine : "InnoDB");
            
            if (autoIncrement != null && !"0".equals(autoIncrement.toString())) {
                sql.append(" AUTO_INCREMENT=").append(autoIncrement);
            }
            
            if (charset != null) {
                sql.append(" DEFAULT CHARSET=").append(charset);
            }
            
            if (collation != null) {
                sql.append(" COLLATE=").append(collation);
            }
            
            if (tableComment != null && !tableComment.isEmpty()) {
                sql.append(" COMMENT='").append(tableComment.replace("'", "\\'")).append("'");
            }
        }
        
        sql.append(";\n");
        
        return sql.toString();
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