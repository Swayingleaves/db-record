package com.dbrecord.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dbrecord.entity.domain.SqlRecord;
import com.dbrecord.mapper.SqlRecordMapper;
import com.dbrecord.service.SqlRecordService;
import org.springframework.stereotype.Service;

/**
 * SQL执行记录表 服务实现类
 */
@Service
public class SqlRecordServiceImpl extends ServiceImpl<SqlRecordMapper, SqlRecord> implements SqlRecordService {
    
}