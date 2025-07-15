package com.dbrecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbrecord.entity.domain.Project;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目表 Mapper接口
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
    
}