package com.dbrecord.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dbrecord.entity.domain.ProjectVersion;
import com.dbrecord.mapper.ProjectVersionMapper;
import com.dbrecord.service.ProjectVersionService;
import org.springframework.stereotype.Service;

/**
 * 项目版本表 服务实现类
 */
@Service
public class ProjectVersionServiceImpl extends ServiceImpl<ProjectVersionMapper, ProjectVersion> implements ProjectVersionService {
    
}