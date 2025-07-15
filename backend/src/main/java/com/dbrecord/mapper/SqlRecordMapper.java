package com.dbrecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbrecord.entity.domain.SqlRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * SQL执行记录表 Mapper接口
 */
@Mapper
public interface SqlRecordMapper extends BaseMapper<SqlRecord> {
    
}