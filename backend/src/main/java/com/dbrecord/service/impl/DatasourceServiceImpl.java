package com.dbrecord.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dbrecord.entity.domain.Datasource;
import com.dbrecord.mapper.DatasourceMapper;
import com.dbrecord.service.DatasourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * 数据源表 服务实现类
 */
@Slf4j
@Service
public class DatasourceServiceImpl extends ServiceImpl<DatasourceMapper, Datasource> implements DatasourceService {
    
    @Override
    public boolean testConnection(Datasource datasource) {
        try {
            String url = buildJdbcUrl(datasource);
            log.info("测试数据库连接: {}", url);

            // 对于 KingbaseES，尝试加载驱动
            if ("kingbase".equalsIgnoreCase(datasource.getType())) {
                try {
                    Class.forName("com.kingbase8.Driver");
                    log.info("KingbaseES 驱动加载成功");
                } catch (ClassNotFoundException e) {
                    log.warn("KingbaseES 驱动未找到，尝试使用 PostgreSQL 驱动");
                    try {
                        Class.forName("org.postgresql.Driver");
                        // 如果 KingbaseES 驱动不可用，使用 PostgreSQL 驱动和连接格式
                        url = "jdbc:postgresql://" + datasource.getHost() + ":" + datasource.getPort() + "/" + datasource.getDatabaseName();
                        log.info("使用 PostgreSQL 驱动连接 KingbaseES: {}", url);
                    } catch (ClassNotFoundException e2) {
                        log.error("PostgreSQL 驱动也未找到");
                        return false;
                    }
                }
            }

            Connection connection = DriverManager.getConnection(url, datasource.getUsername(), datasource.getPassword());
            connection.close();
            log.info("数据库连接测试成功");
            return true;
        } catch (Exception e) {
            log.error("数据库连接测试失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private String buildJdbcUrl(Datasource datasource) {
        String url;
        switch (datasource.getType().toLowerCase()) {
            case "mysql":
                url = "jdbc:mysql://" + datasource.getHost() + ":" + datasource.getPort() + "/" + datasource.getDatabaseName() +
                      "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC";
                break;
            case "postgresql":
                url = "jdbc:postgresql://" + datasource.getHost() + ":" + datasource.getPort() + "/" + datasource.getDatabaseName();
                break;
            case "kingbase":
                // KingbaseES 连接 URL，添加常用参数
                url = "jdbc:kingbase8://" + datasource.getHost() + ":" + datasource.getPort() + "/" + datasource.getDatabaseName() +
                      "?useUnicode=true&characterEncoding=utf8";
                break;
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + datasource.getType());
        }
        return url;
    }
}