package com.dbrecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbrecord.entity.domain.ProjectVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目版本表 Mapper接口
 */
@Mapper
public interface ProjectVersionMapper extends BaseMapper<ProjectVersion> {
    
}