package com.dbrecord.service.impl;

import com.dbrecord.entity.domain.Datasource;
import com.dbrecord.service.DatabaseSchemaExtractor;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库结构提取器抽象基类
 */
@Slf4j
public abstract class AbstractDatabaseSchemaExtractor implements DatabaseSchemaExtractor {
    
    @Override
    public Connection getConnection(Datasource datasource) throws Exception {
        String url = buildConnectionUrl(datasource);
        return DriverManager.getConnection(url, datasource.getUsername(), datasource.getPassword());
    }
    
    /**
     * 构建数据库连接URL
     * @param datasource 数据源
     * @return 连接URL
     */
    protected abstract String buildConnectionUrl(Datasource datasource);
    
    /**
     * 执行查询并返回结果
     * @param datasource 数据源
     * @param sql SQL语句
     * @param params 参数
     * @return 查询结果
     */
    protected List<Map<String, Object>> executeQuery(Datasource datasource, String sql, Object... params) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try (Connection connection = getConnection(datasource);
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            // 设置参数
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            
            log.info("执行SQL查询: {} 参数: {}", sql, java.util.Arrays.toString(params));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    
                    result.add(row);
                }
            }
            
            log.info("查询结果数量: {}", result.size());
            if (!result.isEmpty()) {
                log.info("第一行数据: {}", result.get(0));
            }
        } catch (Exception e) {
            log.error("执行查询失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 获取Long类型值
     */
    protected Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 获取Integer类型值
     */
    protected Integer getIntValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 获取Boolean类型值
     */
    protected Boolean getBooleanValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    /**
     * 获取字符串值（处理大小写差异）
     */
    protected String getStringValue(Map<String, Object> data, String upperCaseKey, String lowerCaseKey) {
        String value = (String) data.get(upperCaseKey);
        if (value == null) {
            value = (String) data.get(lowerCaseKey);
        }
        return value;
    }
    
    /**
     * 获取对象值（处理大小写差异）
     */
    protected Object getObjectValue(Map<String, Object> data, String upperCaseKey, String lowerCaseKey) {
        Object value = data.get(upperCaseKey);
        if (value == null) {
            value = data.get(lowerCaseKey);
        }
        return value;
    }
}