package com.dbrecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbrecord.entity.domain.Datasource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源表 Mapper接口
 */
@Mapper
public interface DatasourceMapper extends BaseMapper<Datasource> {
} 