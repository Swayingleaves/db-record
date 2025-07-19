package com.dbrecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbrecord.entity.domain.VersionTableIndex;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 版本表索引Mapper
 * @author system
 */
public interface VersionTableIndexMapper extends BaseMapper<VersionTableIndex> {

    /**
     * 根据项目版本ID删除所有相关的表索引记录
     * 使用两步删除避免MySQL子查询限制
     */
    @Delete("DELETE vti FROM version_table_index vti " +
            "INNER JOIN version_table_structure vts ON vti.version_table_id = vts.id " +
            "WHERE vts.project_version_id = #{projectVersionId}")
    int deleteByVersionId(@Param("projectVersionId") Long projectVersionId);

}