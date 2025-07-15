package com.dbrecord.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dbrecord.entity.domain.Datasource;
import com.dbrecord.mapper.DatasourceMapper;
import com.dbrecord.service.DatasourceService;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * 数据源表 服务实现类
 */
@Service
public class DatasourceServiceImpl extends ServiceImpl<DatasourceMapper, Datasource> implements DatasourceService {
    
    @Override
    public boolean testConnection(Datasource datasource) {
        try {
            String url = buildJdbcUrl(datasource);
            Connection connection = DriverManager.getConnection(url, datasource.getUsername(), datasource.getPassword());
            connection.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String buildJdbcUrl(Datasource datasource) {
        String url;
        switch (datasource.getType().toLowerCase()) {
            case "mysql":
                url = "jdbc:mysql://" + datasource.getHost() + ":" + datasource.getPort() + "/" + datasource.getDatabaseName();
                break;
            case "postgresql":
                url = "jdbc:postgresql://" + datasource.getHost() + ":" + datasource.getPort() + "/" + datasource.getDatabaseName();
                break;
            case "kingbase":
                url = "jdbc:kingbase8://" + datasource.getHost() + ":" + datasource.getPort() + "/" + datasource.getDatabaseName();
                break;
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + datasource.getType());
        }
        return url;
    }
}