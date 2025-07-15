package com.dbrecord.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dbrecord.entity.domain.Project;
import com.dbrecord.mapper.ProjectMapper;
import com.dbrecord.service.ProjectService;
import org.springframework.stereotype.Service;

/**
 * 项目表 服务实现类
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {
    
}