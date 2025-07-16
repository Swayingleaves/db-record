package com.dbrecord.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dbrecord.entity.domain.*;
import com.dbrecord.mapper.*;
import com.dbrecord.service.DatabaseSchemaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据库结构服务实现类
 */
@Slf4j
@Service
public class DatabaseSchemaServiceImpl implements DatabaseSchemaService {
    
    @Autowired
    private VersionDatabaseSchemaMapper versionDatabaseSchemaMapper;
    
    @Autowired
    private VersionTableStructureMapper versionTableStructureMapper;
    
    @Autowired
    private VersionTableColumnMapper versionTableColumnMapper;
    
    @Autowired
    private VersionTableIndexMapper versionTableIndexMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    @Transactional
    public boolean captureAndSaveDatabaseSchema(Long projectVersionId, Datasource datasource, Long userId) {
        try {
            // 1. 获取数据库基本信息
            Map<String, Object> databaseInfo = getDatabaseInfo(datasource);
            
            // 2. 保存数据库结构信息
            VersionDatabaseSchema versionDatabaseSchema = new VersionDatabaseSchema();
            versionDatabaseSchema.setProjectVersionId(projectVersionId);
            versionDatabaseSchema.setDatabaseName(datasource.getDatabaseName());
            versionDatabaseSchema.setCharset((String) databaseInfo.get("charset"));
            versionDatabaseSchema.setCollation((String) databaseInfo.get("collation"));
            versionDatabaseSchema.setSnapshotTime(LocalDateTime.now());
            versionDatabaseSchema.setUserId(userId);
            versionDatabaseSchemaMapper.insert(versionDatabaseSchema);
            
            // 3. 获取所有表的结构信息
            List<Map<String, Object>> tablesStructure = getTablesStructure(datasource);
            
            // 4. 保存每个表的结构信息
            for (Map<String, Object> tableInfo : tablesStructure) {
                String tableName = (String) tableInfo.get("TABLE_NAME");
                
                // 保存表结构信息
                VersionTableStructure versionTableStructure = new VersionTableStructure();
                versionTableStructure.setProjectVersionId(projectVersionId);
                versionTableStructure.setTableName(tableName);
                versionTableStructure.setTableComment((String) tableInfo.get("TABLE_COMMENT"));
                versionTableStructure.setTableType((String) tableInfo.get("TABLE_TYPE"));
                versionTableStructure.setEngine((String) tableInfo.get("ENGINE"));
                versionTableStructure.setCharset((String) tableInfo.get("TABLE_COLLATION"));
                versionTableStructure.setCollation((String) tableInfo.get("TABLE_COLLATION"));
                versionTableStructure.setRowFormat((String) tableInfo.get("ROW_FORMAT"));
                versionTableStructure.setTableRows(getLongValue(tableInfo.get("TABLE_ROWS")));
                versionTableStructure.setAvgRowLength(getLongValue(tableInfo.get("AVG_ROW_LENGTH")));
                versionTableStructure.setDataLength(getLongValue(tableInfo.get("DATA_LENGTH")));
                versionTableStructure.setIndexLength(getLongValue(tableInfo.get("INDEX_LENGTH")));
                versionTableStructure.setAutoIncrement(getLongValue(tableInfo.get("AUTO_INCREMENT")));
                versionTableStructureMapper.insert(versionTableStructure);
                
                Long versionTableId = versionTableStructure.getId();
                
                // 保存表字段信息
                List<Map<String, Object>> columns = getTableColumns(datasource, tableName);
                for (Map<String, Object> columnInfo : columns) {
                    VersionTableColumn versionTableColumn = new VersionTableColumn();
                    versionTableColumn.setVersionTableId(versionTableId);
                    versionTableColumn.setColumnName((String) columnInfo.get("COLUMN_NAME"));
                    versionTableColumn.setOrdinalPosition(getIntValue(columnInfo.get("ORDINAL_POSITION")));
                    versionTableColumn.setColumnDefault((String) columnInfo.get("COLUMN_DEFAULT"));
                    versionTableColumn.setIsNullable((String) columnInfo.get("IS_NULLABLE"));
                    versionTableColumn.setDataType((String) columnInfo.get("DATA_TYPE"));
                    versionTableColumn.setCharacterMaximumLength(getLongValue(columnInfo.get("CHARACTER_MAXIMUM_LENGTH")));
                    versionTableColumn.setCharacterOctetLength(getLongValue(columnInfo.get("CHARACTER_OCTET_LENGTH")));
                    versionTableColumn.setNumericPrecision(getIntValue(columnInfo.get("NUMERIC_PRECISION")));
                    versionTableColumn.setNumericScale(getIntValue(columnInfo.get("NUMERIC_SCALE")));
                    versionTableColumn.setDatetimePrecision(getIntValue(columnInfo.get("DATETIME_PRECISION")));
                    versionTableColumn.setCharacterSetName((String) columnInfo.get("CHARACTER_SET_NAME"));
                    versionTableColumn.setCollationName((String) columnInfo.get("COLLATION_NAME"));
                    versionTableColumn.setColumnType((String) columnInfo.get("COLUMN_TYPE"));
                    versionTableColumn.setColumnKey((String) columnInfo.get("COLUMN_KEY"));
                    versionTableColumn.setExtra((String) columnInfo.get("EXTRA"));
                    versionTableColumn.setColumnComment((String) columnInfo.get("COLUMN_COMMENT"));
                    versionTableColumnMapper.insert(versionTableColumn);
                }
                
                // 保存表索引信息
                List<Map<String, Object>> indexes = getTableIndexes(datasource, tableName);
                Map<String, List<Map<String, Object>>> indexGroups = groupIndexesByName(indexes);
                
                for (Map.Entry<String, List<Map<String, Object>>> entry : indexGroups.entrySet()) {
                    String indexName = entry.getKey();
                    List<Map<String, Object>> indexColumns = entry.getValue();
                    
                    if (!indexColumns.isEmpty()) {
                        Map<String, Object> firstIndex = indexColumns.get(0);
                        
                        VersionTableIndex versionTableIndex = new VersionTableIndex();
                        versionTableIndex.setVersionTableId(versionTableId);
                        versionTableIndex.setIndexName(indexName);
                        versionTableIndex.setIndexType((String) firstIndex.get("INDEX_TYPE"));
                        versionTableIndex.setIsUnique(!getBooleanValue(firstIndex.get("NON_UNIQUE")));
                        versionTableIndex.setIsPrimary("PRIMARY".equals(indexName));
                        
                        // 构建字段名数组
                        List<String> columnNames = new ArrayList<>();
                        List<String> subParts = new ArrayList<>();
                        
                        for (Map<String, Object> indexColumn : indexColumns) {
                            columnNames.add((String) indexColumn.get("COLUMN_NAME"));
                            Object subPart = indexColumn.get("SUB_PART");
                            subParts.add(subPart != null ? subPart.toString() : null);
                        }
                        
                        try {
                            versionTableIndex.setColumnNames(objectMapper.writeValueAsString(columnNames));
                            versionTableIndex.setSubPart(objectMapper.writeValueAsString(subParts));
                        } catch (JsonProcessingException e) {
                            log.error("序列化索引信息失败: {}", e.getMessage());
                            versionTableIndex.setColumnNames(columnNames.toString());
                            versionTableIndex.setSubPart(subParts.toString());
                        }
                        
                        versionTableIndex.setIndexComment((String) firstIndex.get("INDEX_COMMENT"));
                        versionTableIndexMapper.insert(versionTableIndex);
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("捕获数据库结构失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getDatabaseInfo(Datasource datasource) {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection connection = getConnection(datasource)) {
            // 获取数据库基本信息
            String sql = "SELECT DEFAULT_CHARACTER_SET_NAME as charset, DEFAULT_COLLATION_NAME as collation " +
                        "FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, datasource.getDatabaseName());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        result.put("charset", rs.getString("charset"));
                        result.put("collation", rs.getString("collation"));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取数据库信息失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getTablesStructure(Datasource datasource) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try (Connection connection = getConnection(datasource)) {
            String sql = "SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, datasource.getDatabaseName());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> tableInfo = new HashMap<>();
                        ResultSetMetaData metaData = rs.getMetaData();
                        
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            tableInfo.put(columnName, value);
                        }
                        
                        result.add(tableInfo);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取表结构失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getTableColumns(Datasource datasource, String tableName) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try (Connection connection = getConnection(datasource)) {
            String sql = "SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, datasource.getDatabaseName());
                stmt.setString(2, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> columnInfo = new HashMap<>();
                        ResultSetMetaData metaData = rs.getMetaData();
                        
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            columnInfo.put(columnName, value);
                        }
                        
                        result.add(columnInfo);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取表字段失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getTableIndexes(Datasource datasource, String tableName) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try (Connection connection = getConnection(datasource)) {
            String sql = "SELECT * FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY INDEX_NAME, SEQ_IN_INDEX";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, datasource.getDatabaseName());
                stmt.setString(2, tableName);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> indexInfo = new HashMap<>();
                        ResultSetMetaData metaData = rs.getMetaData();
                        
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            indexInfo.put(columnName, value);
                        }
                        
                        result.add(indexInfo);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取表索引失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    @Override
    public VersionDatabaseSchema getVersionDatabaseSchema(Long projectVersionId) {
        QueryWrapper<VersionDatabaseSchema> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_version_id", projectVersionId);
        return versionDatabaseSchemaMapper.selectOne(queryWrapper);
    }
    
    @Override
    public List<VersionTableStructure> getVersionTableStructures(Long projectVersionId) {
        QueryWrapper<VersionTableStructure> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_version_id", projectVersionId);
        queryWrapper.orderByAsc("table_name");
        return versionTableStructureMapper.selectList(queryWrapper);
    }
    
    @Override
    public Map<String, Object> compareVersions(Long fromVersionId, Long toVersionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取两个版本的表结构
            List<VersionTableStructure> fromTables = getVersionTableStructures(fromVersionId);
            List<VersionTableStructure> toTables = getVersionTableStructures(toVersionId);
            
            // 转换为Map便于比较
            Map<String, VersionTableStructure> fromTableMap = new HashMap<>();
            Map<String, VersionTableStructure> toTableMap = new HashMap<>();
            
            for (VersionTableStructure table : fromTables) {
                fromTableMap.put(table.getTableName(), table);
            }
            for (VersionTableStructure table : toTables) {
                toTableMap.put(table.getTableName(), table);
            }
            
            // 找出新增的表
            List<String> addedTables = new ArrayList<>();
            for (String tableName : toTableMap.keySet()) {
                if (!fromTableMap.containsKey(tableName)) {
                    addedTables.add(tableName);
                }
            }
            
            // 找出删除的表
            List<String> removedTables = new ArrayList<>();
            for (String tableName : fromTableMap.keySet()) {
                if (!toTableMap.containsKey(tableName)) {
                    removedTables.add(tableName);
                }
            }
            
            // 找出修改的表
            List<Map<String, Object>> modifiedTables = new ArrayList<>();
            for (String tableName : fromTableMap.keySet()) {
                if (toTableMap.containsKey(tableName)) {
                    VersionTableStructure fromTable = fromTableMap.get(tableName);
                    VersionTableStructure toTable = toTableMap.get(tableName);
                    
                    // 简单比较表注释
                    if (!Objects.equals(fromTable.getTableComment(), toTable.getTableComment())) {
                        Map<String, Object> change = new HashMap<>();
                        change.put("tableName", tableName);
                        change.put("changeType", "COMMENT_CHANGED");
                        change.put("oldComment", fromTable.getTableComment());
                        change.put("newComment", toTable.getTableComment());
                        modifiedTables.add(change);
                    }
                    
                    // TODO: 详细的字段和索引比较
                }
            }
            
            result.put("addedTables", addedTables);
            result.put("removedTables", removedTables);
            result.put("modifiedTables", modifiedTables);
            
        } catch (Exception e) {
            log.error("版本比较失败: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> getVersionCompleteStructure(Long projectVersionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 获取数据库结构信息
            VersionDatabaseSchema databaseSchema = getVersionDatabaseSchema(projectVersionId);
            if (databaseSchema != null) {
                Map<String, Object> databaseInfo = new HashMap<>();
                databaseInfo.put("databaseName", databaseSchema.getDatabaseName());
                databaseInfo.put("charset", databaseSchema.getCharset());
                databaseInfo.put("collation", databaseSchema.getCollation());
                databaseInfo.put("snapshotTime", databaseSchema.getSnapshotTime());
                result.put("database", databaseInfo);
            }
            
            // 2. 获取表结构列表
            List<VersionTableStructure> tableStructures = getVersionTableStructures(projectVersionId);
            List<Map<String, Object>> tables = new ArrayList<>();
            
            for (VersionTableStructure tableStructure : tableStructures) {
                Map<String, Object> tableInfo = new HashMap<>();
                tableInfo.put("id", tableStructure.getId());
                tableInfo.put("tableName", tableStructure.getTableName());
                tableInfo.put("tableComment", tableStructure.getTableComment());
                tableInfo.put("tableType", tableStructure.getTableType());
                tableInfo.put("engine", tableStructure.getEngine());
                tableInfo.put("charset", tableStructure.getCharset());
                tableInfo.put("collation", tableStructure.getCollation());
                tableInfo.put("rowFormat", tableStructure.getRowFormat());
                tableInfo.put("tableRows", tableStructure.getTableRows());
                tableInfo.put("avgRowLength", tableStructure.getAvgRowLength());
                tableInfo.put("dataLength", tableStructure.getDataLength());
                tableInfo.put("indexLength", tableStructure.getIndexLength());
                tableInfo.put("autoIncrement", tableStructure.getAutoIncrement());
                
                // 3. 获取表的字段信息
                QueryWrapper<VersionTableColumn> columnQueryWrapper = new QueryWrapper<>();
                columnQueryWrapper.eq("version_table_id", tableStructure.getId());
                columnQueryWrapper.orderByAsc("ordinal_position");
                List<VersionTableColumn> columns = versionTableColumnMapper.selectList(columnQueryWrapper);
                
                List<Map<String, Object>> columnList = new ArrayList<>();
                for (VersionTableColumn column : columns) {
                    Map<String, Object> columnInfo = new HashMap<>();
                    columnInfo.put("columnName", column.getColumnName());
                    columnInfo.put("ordinalPosition", column.getOrdinalPosition());
                    columnInfo.put("columnDefault", column.getColumnDefault());
                    columnInfo.put("isNullable", column.getIsNullable());
                    columnInfo.put("dataType", column.getDataType());
                    columnInfo.put("characterMaximumLength", column.getCharacterMaximumLength());
                    columnInfo.put("characterOctetLength", column.getCharacterOctetLength());
                    columnInfo.put("numericPrecision", column.getNumericPrecision());
                    columnInfo.put("numericScale", column.getNumericScale());
                    columnInfo.put("datetimePrecision", column.getDatetimePrecision());
                    columnInfo.put("characterSetName", column.getCharacterSetName());
                    columnInfo.put("collationName", column.getCollationName());
                    columnInfo.put("columnType", column.getColumnType());
                    columnInfo.put("columnKey", column.getColumnKey());
                    columnInfo.put("extra", column.getExtra());
                    columnInfo.put("columnComment", column.getColumnComment());
                    columnList.add(columnInfo);
                }
                tableInfo.put("columns", columnList);
                
                // 4. 获取表的索引信息
                QueryWrapper<VersionTableIndex> indexQueryWrapper = new QueryWrapper<>();
                indexQueryWrapper.eq("version_table_id", tableStructure.getId());
                indexQueryWrapper.orderByAsc("index_name");
                List<VersionTableIndex> indexes = versionTableIndexMapper.selectList(indexQueryWrapper);
                
                List<Map<String, Object>> indexList = new ArrayList<>();
                for (VersionTableIndex index : indexes) {
                    Map<String, Object> indexInfo = new HashMap<>();
                    indexInfo.put("indexName", index.getIndexName());
                    indexInfo.put("indexType", index.getIndexType());
                    indexInfo.put("isUnique", index.getIsUnique());
                    indexInfo.put("isPrimary", index.getIsPrimary());
                    indexInfo.put("columnNames", index.getColumnNames());
                    indexInfo.put("subPart", index.getSubPart());
                    indexInfo.put("indexComment", index.getIndexComment());
                    indexList.add(indexInfo);
                }
                tableInfo.put("indexes", indexList);
                
                tables.add(tableInfo);
            }
            
            result.put("tables", tables);
            result.put("tableCount", tables.size());
            
        } catch (Exception e) {
            log.error("获取版本完整结构失败: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取数据库连接
     */
    private Connection getConnection(Datasource datasource) throws SQLException {
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&characterEncoding=utf8",
                datasource.getHost(), datasource.getPort(), datasource.getDatabaseName());
        return DriverManager.getConnection(url, datasource.getUsername(), datasource.getPassword());
    }
    
    /**
     * 按索引名分组索引信息
     */
    private Map<String, List<Map<String, Object>>> groupIndexesByName(List<Map<String, Object>> indexes) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        
        for (Map<String, Object> index : indexes) {
            String indexName = (String) index.get("INDEX_NAME");
            result.computeIfAbsent(indexName, k -> new ArrayList<>()).add(index);
        }
        
        return result;
    }
    
    /**
     * 安全获取Long值
     */
    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 安全获取Integer值
     */
    private Integer getIntValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 安全获取Boolean值
     */
    private Boolean getBooleanValue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }
}