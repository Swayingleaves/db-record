package com.dbrecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbrecord.entity.domain.VersionTableColumn;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 版本表字段Mapper
 * @author system
 */
public interface VersionTableColumnMapper extends BaseMapper<VersionTableColumn> {

    /**
     * 根据项目版本ID删除所有相关的表字段记录
     * 使用两步删除避免MySQL子查询限制
     */
    @Delete("DELETE vtc FROM version_table_column vtc " +
            "INNER JOIN version_table_structure vts ON vtc.version_table_id = vts.id " +
            "WHERE vts.project_version_id = #{projectVersionId}")
    int deleteByVersionId(@Param("projectVersionId") Long projectVersionId);

}