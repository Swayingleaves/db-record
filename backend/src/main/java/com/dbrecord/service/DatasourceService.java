package com.dbrecord.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dbrecord.entity.domain.Datasource;

/**
 * 数据源表 服务类
 */
public interface DatasourceService extends IService<Datasource> {
    
    /**
     * 测试数据源连接
     * @param datasource 数据源信息
     * @return 连接是否成功
     */
    boolean testConnection(Datasource datasource);
} 