package com.dbrecord.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dbrecord.entity.domain.*;
import com.dbrecord.enums.DatabaseType;
import com.dbrecord.mapper.*;
import com.dbrecord.service.DatabaseSchemaExtractor;
import com.dbrecord.service.DatabaseSchemaExtractorFactory;
import com.dbrecord.service.DatabaseSchemaService;
import com.dbrecord.strategy.SqlGenerationStrategy;
import com.dbrecord.strategy.SqlGenerationStrategyFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据库结构服务实现类
 */
@Slf4j
@Service
public class DatabaseSchemaServiceImpl implements DatabaseSchemaService {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaServiceImpl.class);
    
    @Autowired
    private VersionDatabaseSchemaMapper versionDatabaseSchemaMapper;
    
    @Autowired
    private VersionTableStructureMapper versionTableStructureMapper;
    
    @Autowired
    private VersionTableColumnMapper versionTableColumnMapper;
    
    @Autowired
    private VersionTableIndexMapper versionTableIndexMapper;
    
    @Autowired
    private DatabaseSchemaExtractorFactory extractorFactory;
    
    @Autowired
    private ProjectVersionMapper projectVersionMapper;
    
    @Autowired
    private ProjectMapper projectMapper;
    
    @Autowired
    private DatasourceMapper datasourceMapper;
    
    @Autowired
    private SqlGenerationStrategyFactory sqlGenerationStrategyFactory;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    @Transactional
    public boolean captureAndSaveDatabaseSchema(Long projectVersionId, Datasource datasource, Long userId) {
        try {
            // 1. 检查数据库类型是否支持
            if (!extractorFactory.isSupported(datasource.getType())) {
                log.error("不支持的数据库类型: {}", datasource.getType());
                return false;
            }
            
            // 2. 获取对应的数据库结构提取器
            DatabaseSchemaExtractor extractor = extractorFactory.getExtractor(datasource.getType());
            
            // 3. 删除该版本已存在的数据库结构信息（如果有的话）
            deleteExistingVersionData(projectVersionId);
            
            // 4. 获取数据库基本信息
            Map<String, Object> databaseInfo = extractor.getDatabaseInfo(datasource);
            
            // 5. 保存数据库结构信息
            VersionDatabaseSchema versionDatabaseSchema = new VersionDatabaseSchema();
            versionDatabaseSchema.setProjectVersionId(projectVersionId);
            versionDatabaseSchema.setDatabaseName(datasource.getDatabaseName());
            versionDatabaseSchema.setCharset((String) databaseInfo.get("charset"));
            versionDatabaseSchema.setCollation((String) databaseInfo.get("collation"));
            
            // 处理PostgreSQL的schema信息
            if (databaseInfo.containsKey("schemas_info")) {
                try {
                    String schemasInfoJson = objectMapper.writeValueAsString(databaseInfo.get("schemas_info"));
                    versionDatabaseSchema.setSchemasInfo(schemasInfoJson);
                } catch (JsonProcessingException e) {
                    log.error("序列化schema信息失败: {}", e.getMessage());
                }
            }
            
            versionDatabaseSchema.setSnapshotTime(LocalDateTime.now());
            versionDatabaseSchema.setUserId(userId);
            versionDatabaseSchemaMapper.insert(versionDatabaseSchema);
            
            // 6. 获取所有表的结构信息
            List<Map<String, Object>> tablesStructure = extractor.getTablesStructure(datasource);
            
            // 4. 保存每个表的结构信息
            for (Map<String, Object> tableInfo : tablesStructure) {
                // PostgreSQL返回小写字段名，MySQL返回大写字段名
                String tableName = (String) tableInfo.get("TABLE_NAME");
                if (tableName == null) {
                    tableName = (String) tableInfo.get("table_name");
                }
                
                // 跳过表名为空的记录
                if (tableName == null || tableName.trim().isEmpty()) {
                    log.warn("跳过表名为空的表记录: {}", tableInfo);
                    continue;
                }
                
                // 保存表结构信息
                VersionTableStructure versionTableStructure = new VersionTableStructure();
                versionTableStructure.setProjectVersionId(projectVersionId);
                versionTableStructure.setTableName(tableName);
                
                // 处理schema信息（PostgreSQL专用）
                String schemaName = (String) tableInfo.get("SCHEMA_NAME");
                if (schemaName == null) schemaName = (String) tableInfo.get("schema_name");
                if (schemaName == null) schemaName = "public"; // 默认schema
                versionTableStructure.setSchemaName(schemaName);
                
                // 处理字段名大小写差异
                String tableComment = (String) tableInfo.get("TABLE_COMMENT");
                if (tableComment == null) tableComment = (String) tableInfo.get("table_comment");
                
                String tableType = (String) tableInfo.get("TABLE_TYPE");
                if (tableType == null) tableType = (String) tableInfo.get("table_type");
                
                String engine = (String) tableInfo.get("ENGINE");
                if (engine == null) engine = (String) tableInfo.get("engine");
                
                Object tableRows = tableInfo.get("TABLE_ROWS");
                if (tableRows == null) tableRows = tableInfo.get("table_rows");
                
                Object dataLength = tableInfo.get("DATA_LENGTH");
                if (dataLength == null) dataLength = tableInfo.get("data_length");
                
                Object indexLength = tableInfo.get("INDEX_LENGTH");
                if (indexLength == null) indexLength = tableInfo.get("index_length");
                
                versionTableStructure.setTableComment(tableComment);
                versionTableStructure.setTableType(tableType);
                versionTableStructure.setEngine(engine);
                versionTableStructure.setCharset((String) tableInfo.get("TABLE_COLLATION"));
                versionTableStructure.setCollation((String) tableInfo.get("TABLE_COLLATION"));
                versionTableStructure.setRowFormat((String) tableInfo.get("ROW_FORMAT"));
                versionTableStructure.setTableRows(getLongValue(tableRows));
                versionTableStructure.setAvgRowLength(getLongValue(tableInfo.get("AVG_ROW_LENGTH")));
                versionTableStructure.setDataLength(getLongValue(dataLength));
                versionTableStructure.setIndexLength(getLongValue(indexLength));
                versionTableStructure.setAutoIncrement(getLongValue(tableInfo.get("AUTO_INCREMENT")));
                versionTableStructureMapper.insert(versionTableStructure);
                
                Long versionTableId = versionTableStructure.getId();
                
                // 保存表字段信息
                List<Map<String, Object>> columns = extractor.getTableColumns(datasource, schemaName, tableName);
                for (Map<String, Object> columnInfo : columns) {
                    VersionTableColumn versionTableColumn = new VersionTableColumn();
                    versionTableColumn.setVersionTableId(versionTableId);
                    
                    // 处理字段名大小写差异
                    String columnName = (String) columnInfo.get("COLUMN_NAME");
                    if (columnName == null) columnName = (String) columnInfo.get("column_name");
                    
                    Object ordinalPosition = columnInfo.get("ORDINAL_POSITION");
                    if (ordinalPosition == null) ordinalPosition = columnInfo.get("ordinal_position");
                    
                    String columnDefault = (String) columnInfo.get("COLUMN_DEFAULT");
                    if (columnDefault == null) columnDefault = (String) columnInfo.get("column_default");
                    
                    String isNullable = (String) columnInfo.get("IS_NULLABLE");
                    if (isNullable == null) isNullable = (String) columnInfo.get("is_nullable");
                    
                    String dataType = (String) columnInfo.get("DATA_TYPE");
                    if (dataType == null) dataType = (String) columnInfo.get("data_type");
                    
                    Object characterMaxLength = columnInfo.get("CHARACTER_MAXIMUM_LENGTH");
                    if (characterMaxLength == null) characterMaxLength = columnInfo.get("character_maximum_length");
                    
                    Object characterOctetLength = columnInfo.get("CHARACTER_OCTET_LENGTH");
                    if (characterOctetLength == null) characterOctetLength = columnInfo.get("character_octet_length");
                    
                    Object numericPrecision = columnInfo.get("NUMERIC_PRECISION");
                    if (numericPrecision == null) numericPrecision = columnInfo.get("numeric_precision");
                    
                    Object numericScale = columnInfo.get("NUMERIC_SCALE");
                    if (numericScale == null) numericScale = columnInfo.get("numeric_scale");
                    
                    Object datetimePrecision = columnInfo.get("DATETIME_PRECISION");
                    if (datetimePrecision == null) datetimePrecision = columnInfo.get("datetime_precision");
                    
                    String characterSetName = (String) columnInfo.get("CHARACTER_SET_NAME");
                    if (characterSetName == null) characterSetName = (String) columnInfo.get("character_set_name");
                    
                    String collationName = (String) columnInfo.get("COLLATION_NAME");
                    if (collationName == null) collationName = (String) columnInfo.get("collation_name");
                    
                    String columnType = (String) columnInfo.get("COLUMN_TYPE");
                    if (columnType == null) columnType = (String) columnInfo.get("column_type");
                    
                    String columnKey = (String) columnInfo.get("COLUMN_KEY");
                    if (columnKey == null) columnKey = (String) columnInfo.get("column_key");
                    
                    String extra = (String) columnInfo.get("EXTRA");
                    if (extra == null) extra = (String) columnInfo.get("extra");
                    
                    String columnComment = (String) columnInfo.get("COLUMN_COMMENT");
                    if (columnComment == null) columnComment = (String) columnInfo.get("column_comment");
                    
                    versionTableColumn.setColumnName(columnName);
                    versionTableColumn.setOrdinalPosition(getIntValue(ordinalPosition));
                    versionTableColumn.setColumnDefault(columnDefault);
                    versionTableColumn.setIsNullable(isNullable);
                    versionTableColumn.setDataType(dataType);
                    versionTableColumn.setCharacterMaximumLength(getLongValue(characterMaxLength));
                    versionTableColumn.setCharacterOctetLength(getLongValue(characterOctetLength));
                    versionTableColumn.setNumericPrecision(getIntValue(numericPrecision));
                    versionTableColumn.setNumericScale(getIntValue(numericScale));
                    versionTableColumn.setDatetimePrecision(getIntValue(datetimePrecision));
                    versionTableColumn.setCharacterSetName(characterSetName);
                    versionTableColumn.setCollationName(collationName);
                    versionTableColumn.setColumnType(columnType);
                    versionTableColumn.setColumnKey(columnKey);
                    versionTableColumn.setExtra(extra);
                    versionTableColumn.setColumnComment(columnComment);

                    // 检查是否已存在相同的字段记录，避免重复插入
                    QueryWrapper<VersionTableColumn> columnCheckWrapper = new QueryWrapper<>();
                    columnCheckWrapper.eq("version_table_id", versionTableId);
                    columnCheckWrapper.eq("column_name", columnName);
                    VersionTableColumn existingColumn = versionTableColumnMapper.selectOne(columnCheckWrapper);

                    if (existingColumn == null) {
                        versionTableColumnMapper.insert(versionTableColumn);
                    } else {
                        log.warn("字段 {} 在表 {} 中已存在，跳过插入", columnName, versionTableId);
                    }
                }
                
                // 保存表索引信息
                List<Map<String, Object>> indexes = extractor.getTableIndexes(datasource, schemaName, tableName);
                Map<String, List<Map<String, Object>>> indexGroups = groupIndexesByName(indexes);
                
                for (Map.Entry<String, List<Map<String, Object>>> entry : indexGroups.entrySet()) {
                    String indexName = entry.getKey();
                    List<Map<String, Object>> indexColumns = entry.getValue();
                    
                    // 跳过空的索引名
                    if (indexName == null || indexName.trim().isEmpty()) {
                        log.warn("跳过空的索引名，表: {}", tableName);
                        continue;
                    }
                    
                    if (!indexColumns.isEmpty()) {
                        Map<String, Object> firstIndex = indexColumns.get(0);
                        
                        VersionTableIndex versionTableIndex = new VersionTableIndex();
                        versionTableIndex.setVersionTableId(versionTableId);
                        versionTableIndex.setIndexName(indexName);
                        
                        // 处理字段名大小写差异
                        String indexType = (String) firstIndex.get("INDEX_TYPE");
                        if (indexType == null) indexType = (String) firstIndex.get("index_type");
                        
                        Object nonUnique = firstIndex.get("NON_UNIQUE");
                        if (nonUnique == null) nonUnique = firstIndex.get("non_unique");
                        
                        String indexComment = (String) firstIndex.get("INDEX_COMMENT");
                        if (indexComment == null) indexComment = (String) firstIndex.get("index_comment");
                        
                        versionTableIndex.setIndexType(indexType);
                        versionTableIndex.setIsUnique(!getBooleanValue(nonUnique));
                        versionTableIndex.setIsPrimary("PRIMARY".equals(indexName));
                        
                        // 构建字段名数组
                        List<String> columnNames = new ArrayList<>();
                        List<String> subParts = new ArrayList<>();
                        
                        for (Map<String, Object> indexColumn : indexColumns) {
                            String columnName = (String) indexColumn.get("COLUMN_NAME");
                            if (columnName == null) columnName = (String) indexColumn.get("column_name");
                            columnNames.add(columnName);
                            
                            Object subPart = indexColumn.get("SUB_PART");
                            if (subPart == null) subPart = indexColumn.get("sub_part");
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
                        
                        versionTableIndex.setIndexComment(indexComment);
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

            // 按schema分组
            Map<String, List<VersionTableStructure>> fromSchemaMap = groupTablesBySchema(fromTables);
            Map<String, List<VersionTableStructure>> toSchemaMap = groupTablesBySchema(toTables);

            // 找出新增、删除和修改的schema
            List<Map<String, Object>> addedSchemas = new ArrayList<>();
            List<Map<String, Object>> removedSchemas = new ArrayList<>();
            List<Map<String, Object>> modifiedSchemas = new ArrayList<>();

            // 1. 找出新增的schema
            for (String schemaName : toSchemaMap.keySet()) {
                if (!fromSchemaMap.containsKey(schemaName)) {
                    Map<String, Object> schemaInfo = new HashMap<>();
                    schemaInfo.put("schemaName", schemaName);
                    schemaInfo.put("tables", toSchemaMap.get(schemaName).stream()
                        .map(t -> Map.of("tableName", t.getTableName(), "tableComment", t.getTableComment()))
                        .collect(java.util.stream.Collectors.toList()));
                    addedSchemas.add(schemaInfo);
                }
            }

            // 2. 找出删除的schema
            for (String schemaName : fromSchemaMap.keySet()) {
                if (!toSchemaMap.containsKey(schemaName)) {
                    Map<String, Object> schemaInfo = new HashMap<>();
                    schemaInfo.put("schemaName", schemaName);
                    schemaInfo.put("tables", fromSchemaMap.get(schemaName).stream()
                        .map(t -> Map.of("tableName", t.getTableName(), "tableComment", t.getTableComment()))
                        .collect(java.util.stream.Collectors.toList()));
                    removedSchemas.add(schemaInfo);
                }
            }

            // 3. 找出共有的schema并比较其内部差异
            for (String schemaName : fromSchemaMap.keySet()) {
                if (toSchemaMap.containsKey(schemaName)) {
                    Map<String, Object> schemaChanges = compareSchemaDetails(
                        fromSchemaMap.get(schemaName),
                        toSchemaMap.get(schemaName)
                    );
                    if (!schemaChanges.isEmpty()) {
                        schemaChanges.put("schemaName", schemaName);
                        modifiedSchemas.add(schemaChanges);
                    }
                }
            }

            result.put("addedSchemas", addedSchemas);
            result.put("removedSchemas", removedSchemas);
            result.put("modifiedSchemas", modifiedSchemas);
            
        } catch (Exception e) {
            log.error("版本比较失败: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 按schema对表进行分组
     */
    private Map<String, List<VersionTableStructure>> groupTablesBySchema(List<VersionTableStructure> tables) {
        Map<String, List<VersionTableStructure>> schemaMap = new HashMap<>();
        for (VersionTableStructure table : tables) {
            String schemaName = table.getSchemaName() != null ? table.getSchemaName() : "public";
            schemaMap.computeIfAbsent(schemaName, k -> new ArrayList<>()).add(table);
        }
        return schemaMap;
    }

    /**
     * 比较同一个schema内部的表差异
     */
    private Map<String, Object> compareSchemaDetails(List<VersionTableStructure> fromTables, List<VersionTableStructure> toTables) {
        Map<String, Object> changes = new HashMap<>();
        
        Map<String, VersionTableStructure> fromTableMap = fromTables.stream()
            .collect(java.util.stream.Collectors.toMap(VersionTableStructure::getTableName, t -> t));
        Map<String, VersionTableStructure> toTableMap = toTables.stream()
            .collect(java.util.stream.Collectors.toMap(VersionTableStructure::getTableName, t -> t));

        // 找出新增的表
        List<Map<String, Object>> addedTables = new ArrayList<>();
        for (String tableName : toTableMap.keySet()) {
            if (!fromTableMap.containsKey(tableName)) {
                VersionTableStructure table = toTableMap.get(tableName);
                Map<String, Object> tableInfo = new HashMap<>();
                tableInfo.put("tableName", table.getTableName());
                tableInfo.put("tableComment", table.getTableComment());
                addedTables.add(tableInfo);
            }
        }

        // 找出删除的表
        List<Map<String, Object>> removedTables = new ArrayList<>();
        for (String tableName : fromTableMap.keySet()) {
            if (!toTableMap.containsKey(tableName)) {
                VersionTableStructure table = fromTableMap.get(tableName);
                Map<String, Object> tableInfo = new HashMap<>();
                tableInfo.put("tableName", table.getTableName());
                tableInfo.put("tableComment", table.getTableComment());
                removedTables.add(tableInfo);
            }
        }

        // 找出修改的表
        List<Map<String, Object>> modifiedTables = new ArrayList<>();
        for (String tableName : fromTableMap.keySet()) {
            if (toTableMap.containsKey(tableName)) {
                VersionTableStructure fromTable = fromTableMap.get(tableName);
                VersionTableStructure toTable = toTableMap.get(tableName);
                
                Map<String, Object> tableChanges = compareTableDetails(fromTable, toTable);
                if (!tableChanges.isEmpty()) {
                    tableChanges.put("tableName", tableName);
                    tableChanges.put("tableComment", toTable.getTableComment());
                    modifiedTables.add(tableChanges);
                }
            }
        }

        if (!addedTables.isEmpty()) {
            changes.put("addedTables", addedTables);
        }
        if (!removedTables.isEmpty()) {
            changes.put("removedTables", removedTables);
        }
        if (!modifiedTables.isEmpty()) {
            changes.put("modifiedTables", modifiedTables);
        }

        return changes;
    }
    
    /**
     * 比较两个表的详细差异
     */
    private Map<String, Object> compareTableDetails(VersionTableStructure fromTable, VersionTableStructure toTable) {
        Map<String, Object> changes = new HashMap<>();
        
        try {
            // 获取字段信息
            List<VersionTableColumn> fromColumns = getTableColumns(fromTable.getId());
            List<VersionTableColumn> toColumns = getTableColumns(toTable.getId());
            
            // 获取索引信息
            List<VersionTableIndex> fromIndexes = getTableIndexes(fromTable.getId());
            List<VersionTableIndex> toIndexes = getTableIndexes(toTable.getId());
            
            // 比较字段
            Map<String, Object> columnChanges = compareColumns(fromColumns, toColumns);
            if (!columnChanges.isEmpty()) {
                changes.putAll(columnChanges);
            }
            
            // 比较索引
            Map<String, Object> indexChanges = compareIndexes(fromIndexes, toIndexes);
            if (!indexChanges.isEmpty()) {
                changes.putAll(indexChanges);
            }
            
            // 比较表注释
            if (!Objects.equals(fromTable.getTableComment(), toTable.getTableComment())) {
                changes.put("commentChanged", true);
                changes.put("oldComment", fromTable.getTableComment());
                changes.put("newComment", toTable.getTableComment());
            }
            
        } catch (Exception e) {
            log.error("比较表详细信息失败: {}", e.getMessage(), e);
        }
        
        return changes;
    }
    
    /**
     * 获取表的字段信息
     */
    private List<VersionTableColumn> getTableColumns(Long tableId) {
        QueryWrapper<VersionTableColumn> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version_table_id", tableId);
        queryWrapper.orderByAsc("ordinal_position");
        return versionTableColumnMapper.selectList(queryWrapper);
    }
    
    /**
     * 获取表的索引信息
     */
    private List<VersionTableIndex> getTableIndexes(Long tableId) {
        QueryWrapper<VersionTableIndex> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("version_table_id", tableId);
        queryWrapper.orderByAsc("index_name");
        return versionTableIndexMapper.selectList(queryWrapper);
    }
    
    /**
     * 比较字段变化
     */
    private Map<String, Object> compareColumns(List<VersionTableColumn> fromColumns, List<VersionTableColumn> toColumns) {
        Map<String, Object> changes = new HashMap<>();
        
        // 转换为Map便于比较
        Map<String, VersionTableColumn> fromColumnMap = new HashMap<>();
        Map<String, VersionTableColumn> toColumnMap = new HashMap<>();
        
        for (VersionTableColumn column : fromColumns) {
            fromColumnMap.put(column.getColumnName(), column);
        }
        for (VersionTableColumn column : toColumns) {
            toColumnMap.put(column.getColumnName(), column);
        }
        
        // 新增字段
        List<Map<String, Object>> addedColumns = new ArrayList<>();
        for (String columnName : toColumnMap.keySet()) {
            if (!fromColumnMap.containsKey(columnName)) {
                VersionTableColumn column = toColumnMap.get(columnName);
                Map<String, Object> columnInfo = new HashMap<>();
                columnInfo.put("columnName", column.getColumnName());
                columnInfo.put("columnType", column.getColumnType());
                columnInfo.put("columnComment", column.getColumnComment());
                addedColumns.add(columnInfo);
            }
        }
        
        // 删除字段
        List<Map<String, Object>> removedColumns = new ArrayList<>();
        for (String columnName : fromColumnMap.keySet()) {
            if (!toColumnMap.containsKey(columnName)) {
                VersionTableColumn column = fromColumnMap.get(columnName);
                Map<String, Object> columnInfo = new HashMap<>();
                columnInfo.put("columnName", column.getColumnName());
                columnInfo.put("columnType", column.getColumnType());
                columnInfo.put("columnComment", column.getColumnComment());
                removedColumns.add(columnInfo);
            }
        }
        
        // 修改字段
        List<Map<String, Object>> modifiedColumns = new ArrayList<>();
        for (String columnName : fromColumnMap.keySet()) {
            if (toColumnMap.containsKey(columnName)) {
                VersionTableColumn fromColumn = fromColumnMap.get(columnName);
                VersionTableColumn toColumn = toColumnMap.get(columnName);
                
                if (!isColumnEqual(fromColumn, toColumn)) {
                    Map<String, Object> columnInfo = new HashMap<>();
                    columnInfo.put("columnName", columnName);
                    columnInfo.put("oldType", fromColumn.getColumnType());
                    columnInfo.put("newType", toColumn.getColumnType());
                    columnInfo.put("oldComment", fromColumn.getColumnComment());
                    columnInfo.put("newComment", toColumn.getColumnComment());
                    modifiedColumns.add(columnInfo);
                }
            }
        }
        
        if (!addedColumns.isEmpty()) {
            changes.put("addedColumns", addedColumns);
        }
        if (!removedColumns.isEmpty()) {
            changes.put("removedColumns", removedColumns);
        }
        if (!modifiedColumns.isEmpty()) {
            changes.put("modifiedColumns", modifiedColumns);
        }
        
        return changes;
    }
    
    /**
     * 比较索引变化
     */
    private Map<String, Object> compareIndexes(List<VersionTableIndex> fromIndexes, List<VersionTableIndex> toIndexes) {
        Map<String, Object> changes = new HashMap<>();
        
        // 转换为Map便于比较
        Map<String, VersionTableIndex> fromIndexMap = new HashMap<>();
        Map<String, VersionTableIndex> toIndexMap = new HashMap<>();
        
        for (VersionTableIndex index : fromIndexes) {
            fromIndexMap.put(index.getIndexName(), index);
        }
        for (VersionTableIndex index : toIndexes) {
            toIndexMap.put(index.getIndexName(), index);
        }
        
        // 新增索引
        List<Map<String, Object>> addedIndexes = new ArrayList<>();
        for (String indexName : toIndexMap.keySet()) {
            if (!fromIndexMap.containsKey(indexName)) {
                VersionTableIndex index = toIndexMap.get(indexName);
                Map<String, Object> indexInfo = new HashMap<>();
                indexInfo.put("indexName", index.getIndexName());
                indexInfo.put("indexType", index.getIndexType());
                indexInfo.put("columnNames", index.getColumnNames());
                indexInfo.put("isUnique", index.getIsUnique());
                addedIndexes.add(indexInfo);
            }
        }
        
        // 删除索引
        List<Map<String, Object>> removedIndexes = new ArrayList<>();
        for (String indexName : fromIndexMap.keySet()) {
            if (!toIndexMap.containsKey(indexName)) {
                VersionTableIndex index = fromIndexMap.get(indexName);
                Map<String, Object> indexInfo = new HashMap<>();
                indexInfo.put("indexName", index.getIndexName());
                indexInfo.put("indexType", index.getIndexType());
                indexInfo.put("columnNames", index.getColumnNames());
                indexInfo.put("isUnique", index.getIsUnique());
                removedIndexes.add(indexInfo);
            }
        }
        
        // 修改索引
        List<Map<String, Object>> modifiedIndexes = new ArrayList<>();
        for (String indexName : fromIndexMap.keySet()) {
            if (toIndexMap.containsKey(indexName)) {
                VersionTableIndex fromIndex = fromIndexMap.get(indexName);
                VersionTableIndex toIndex = toIndexMap.get(indexName);
                
                if (!isIndexEqual(fromIndex, toIndex)) {
                    Map<String, Object> indexInfo = new HashMap<>();
                    indexInfo.put("indexName", indexName);
                    indexInfo.put("oldType", fromIndex.getIndexType());
                    indexInfo.put("newType", toIndex.getIndexType());
                    indexInfo.put("oldColumns", fromIndex.getColumnNames());
                    indexInfo.put("newColumns", toIndex.getColumnNames());
                    modifiedIndexes.add(indexInfo);
                }
            }
        }
        
        if (!addedIndexes.isEmpty()) {
            changes.put("addedIndexes", addedIndexes);
        }
        if (!removedIndexes.isEmpty()) {
            changes.put("removedIndexes", removedIndexes);
        }
        if (!modifiedIndexes.isEmpty()) {
            changes.put("modifiedIndexes", modifiedIndexes);
        }
        
        return changes;
    }
    
    /**
     * 判断两个字段是否相等
     */
    private boolean isColumnEqual(VersionTableColumn col1, VersionTableColumn col2) {
        return Objects.equals(col1.getColumnType(), col2.getColumnType()) &&
               Objects.equals(col1.getIsNullable(), col2.getIsNullable()) &&
               Objects.equals(col1.getColumnDefault(), col2.getColumnDefault()) &&
               Objects.equals(col1.getColumnComment(), col2.getColumnComment()) &&
               Objects.equals(col1.getColumnKey(), col2.getColumnKey()) &&
               Objects.equals(col1.getExtra(), col2.getExtra());
    }
    
    /**
     * 判断两个索引是否相等
     */
    private boolean isIndexEqual(VersionTableIndex idx1, VersionTableIndex idx2) {
        return Objects.equals(idx1.getIndexType(), idx2.getIndexType()) &&
               Objects.equals(idx1.getColumnNames(), idx2.getColumnNames()) &&
               Objects.equals(idx1.getIsUnique(), idx2.getIsUnique()) &&
               Objects.equals(idx1.getIsPrimary(), idx2.getIsPrimary());
    }
    
    @Override
    public String generateDiffSql(Long fromVersionId, Long toVersionId, String fromVersionName, String toVersionName) {
        // 获取数据源类型
        String datasourceType = getDatasourceTypeByVersionId(toVersionId);
        if (datasourceType == null) {
            datasourceType = "mysql"; // 默认使用MySQL
        }
        
        // 获取对应的SQL生成策略
        SqlGenerationStrategy sqlStrategy = sqlGenerationStrategyFactory.getStrategy(datasourceType);
        if (sqlStrategy == null) {
            throw new RuntimeException("不支持的数据库类型: " + datasourceType);
        }
        
        // 获取版本对比结果
        Map<String, Object> compareResult = compareVersions(fromVersionId, toVersionId);
        
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("-- 版本差异SQL: ").append(fromVersionName)
                 .append(" -> ").append(toVersionName).append("\n");
        sqlBuilder.append("-- 数据库类型: ").append(datasourceType.toUpperCase()).append("\n");
        sqlBuilder.append("-- 生成时间: ").append(new java.util.Date()).append("\n\n");
        
        // 处理新增的Schema
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> addedSchemas = (List<Map<String, Object>>) compareResult.get("addedSchemas");
        if (addedSchemas != null && !addedSchemas.isEmpty()) {
            sqlBuilder.append("-- 新增的Schema及其表\n");
            for (Map<String, Object> schema : addedSchemas) {
                String schemaName = (String) schema.get("schemaName");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tables = (List<Map<String, Object>>) schema.get("tables");
                
                sqlBuilder.append("-- Schema: ").append(schemaName).append("\n");
                if (tables != null) {
                    for (Map<String, Object> table : tables) {
                        String createTableSql = generateCreateTableSql(toVersionId, (String) table.get("tableName"));
                        sqlBuilder.append(createTableSql).append("\n\n");
                    }
                }
            }
        }
        
        // 处理删除的Schema
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> removedSchemas = (List<Map<String, Object>>) compareResult.get("removedSchemas");
        if (removedSchemas != null && !removedSchemas.isEmpty()) {
            sqlBuilder.append("-- 删除的Schema及其表\n");
            for (Map<String, Object> schema : removedSchemas) {
                String schemaName = (String) schema.get("schemaName");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tables = (List<Map<String, Object>>) schema.get("tables");
                
                sqlBuilder.append("-- Schema: ").append(schemaName).append("\n");
                if (tables != null) {
                    for (Map<String, Object> table : tables) {
                        String dropTableSql = sqlStrategy.generateDropTableSql((String) table.get("tableName"));
                        sqlBuilder.append(dropTableSql).append("\n");
                    }
                }
                sqlBuilder.append("\n");
            }
        }
        
        // 处理修改的Schema
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modifiedSchemas = (List<Map<String, Object>>) compareResult.get("modifiedSchemas");
        if (modifiedSchemas != null && !modifiedSchemas.isEmpty()) {
            for (Map<String, Object> schema : modifiedSchemas) {
                String schemaName = (String) schema.get("schemaName");
                sqlBuilder.append("-- 修改的Schema: ").append(schemaName).append("\n");
                
                // 处理新增的表
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> addedTables = (List<Map<String, Object>>) schema.get("addedTables");
                if (addedTables != null && !addedTables.isEmpty()) {
                    sqlBuilder.append("-- 新增的表\n");
                    for (Map<String, Object> table : addedTables) {
                        String createTableSql = generateCreateTableSql(toVersionId, (String) table.get("tableName"));
                        sqlBuilder.append(createTableSql).append("\n\n");
                    }
                }
                
                // 处理删除的表
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> removedTables = (List<Map<String, Object>>) schema.get("removedTables");
                if (removedTables != null && !removedTables.isEmpty()) {
                    sqlBuilder.append("-- 删除的表\n");
                    for (Map<String, Object> table : removedTables) {
                        String dropTableSql = sqlStrategy.generateDropTableSql((String) table.get("tableName"));
                        sqlBuilder.append(dropTableSql).append("\n");
                    }
                    sqlBuilder.append("\n");
                }
                
                // 处理修改的表
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> modifiedTables = (List<Map<String, Object>>) schema.get("modifiedTables");
                if (modifiedTables != null && !modifiedTables.isEmpty()) {
                    sqlBuilder.append("-- 修改的表\n");
                    for (Map<String, Object> table : modifiedTables) {
                        String alterTableSql = generateAlterTableSql(toVersionId, (String) table.get("tableName"), table);
                        if (!alterTableSql.trim().isEmpty()) {
                            sqlBuilder.append(alterTableSql).append("\n");
                        }
                    }
                    sqlBuilder.append("\n");
                }
            }
        }
        
        // 如果没有任何差异，添加提示信息
        if (sqlBuilder.length() <= 100) { // 只有头部注释的情况
            sqlBuilder.append("-- 两个版本之间没有发现任何差异\n");
        }
        
        return sqlBuilder.toString();
    }
    
    /**
     * 根据版本ID获取数据源类型
     */
    private String getDatasourceTypeByVersionId(Long versionId) {
        try {
            // 获取项目版本信息
            ProjectVersion projectVersion = projectVersionMapper.selectById(versionId);
            if (projectVersion == null) {
                return null;
            }
            
            // 获取项目信息
            Project project = projectMapper.selectById(projectVersion.getProjectId());
            if (project == null || project.getDatasourceId() == null) {
                return null;
            }
            
            // 获取数据源信息
            Datasource datasource = datasourceMapper.selectById(project.getDatasourceId());
            if (datasource == null) {
                return null;
            }
            
            return datasource.getType();
        } catch (Exception e) {
            log.error("获取数据源类型失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private String generateCreateTableSql(Long versionId, String tableName) {
        try {
            // 获取数据源类型
            String datasourceType = getDatasourceTypeByVersionId(versionId);
            if (datasourceType == null) {
                datasourceType = "mysql"; // 默认使用MySQL
            }
            
            // 获取对应的SQL生成策略
            SqlGenerationStrategy sqlStrategy = sqlGenerationStrategyFactory.getStrategy(datasourceType);
            if (sqlStrategy == null) {
                return "-- CREATE TABLE " + tableName + "; -- 不支持的数据库类型: " + datasourceType;
            }
            
            // 获取表结构信息
            List<VersionTableStructure> tables = versionTableStructureMapper.selectList(
                new QueryWrapper<VersionTableStructure>()
                    .eq("project_version_id", versionId)
                    .eq("table_name", tableName)
            );
            
            if (tables.isEmpty()) {
                return "-- CREATE TABLE " + tableName + "; -- 表结构信息未找到";
            }
            
            VersionTableStructure table = tables.get(0);
            
            // 获取字段信息
            List<VersionTableColumn> columns = getTableColumns(table.getId());
            
            // 获取索引信息
            List<VersionTableIndex> indexes = getTableIndexes(table.getId());
            
            // 使用策略生成CREATE TABLE SQL
            return sqlStrategy.generateCreateTableSql(table, columns, indexes);
        } catch (Exception e) {
            return "-- CREATE TABLE " + tableName + "; -- 生成失败: " + e.getMessage();
        }
    }
    
    private String generateAlterTableSql(Long versionId, String tableName, Map<String, Object> tableChanges) {
        try {
            // 获取数据源类型
            String datasourceType = getDatasourceTypeByVersionId(versionId);
            if (datasourceType == null) {
                datasourceType = "mysql"; // 默认使用MySQL
            }
            
            // 获取对应的SQL生成策略
            SqlGenerationStrategy sqlStrategy = sqlGenerationStrategyFactory.getStrategy(datasourceType);
            if (sqlStrategy == null) {
                return "-- ALTER TABLE " + tableName + "; -- 不支持的数据库类型: " + datasourceType;
            }
            
            return sqlStrategy.generateAlterTableSql(tableName, tableChanges);
        } catch (Exception e) {
            return "-- ALTER TABLE " + tableName + "; -- 生成失败: " + e.getMessage();
        }
    }
    
    private String generateAlterTableSqlOld(String tableName, Map<String, Object> tableChanges) {
        StringBuilder alterSql = new StringBuilder();
        
        // 处理新增字段
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> addedColumns = (List<Map<String, Object>>) tableChanges.get("addedColumns");
        if (addedColumns != null && !addedColumns.isEmpty()) {
            for (Map<String, Object> column : addedColumns) {
                alterSql.append("ALTER TABLE `").append(tableName).append("` ADD COLUMN `")
                       .append(column.get("columnName")).append("` ")
                       .append(column.get("columnType"));
                       
                if ("NO".equals(column.get("isNullable"))) {
                    alterSql.append(" NOT NULL");
                }
                
                if (column.get("columnDefault") != null) {
                    alterSql.append(" DEFAULT '").append(column.get("columnDefault")).append("'");
                }
                
                if (column.get("columnComment") != null && !column.get("columnComment").toString().isEmpty()) {
                    alterSql.append(" COMMENT '").append(column.get("columnComment")).append("'");
                }
                
                alterSql.append(";\n");
            }
        }
        
        // 处理删除字段
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> removedColumns = (List<Map<String, Object>>) tableChanges.get("removedColumns");
        if (removedColumns != null && !removedColumns.isEmpty()) {
            for (Map<String, Object> column : removedColumns) {
                alterSql.append("ALTER TABLE `").append(tableName).append("` DROP COLUMN `")
                       .append(column.get("columnName")).append("`;").append("\n");
            }
        }
        
        // 处理修改字段
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modifiedColumns = (List<Map<String, Object>>) tableChanges.get("modifiedColumns");
        if (modifiedColumns != null && !modifiedColumns.isEmpty()) {
            for (Map<String, Object> column : modifiedColumns) {
                alterSql.append("ALTER TABLE `").append(tableName).append("` MODIFY COLUMN `")
                       .append(column.get("columnName")).append("` ")
                       .append(column.get("newType"));
                       
                if ("NO".equals(column.get("isNullable"))) {
                    alterSql.append(" NOT NULL");
                }
                
                if (column.get("columnDefault") != null) {
                    alterSql.append(" DEFAULT '").append(column.get("columnDefault")).append("'");
                }
                
                if (column.get("newComment") != null && !column.get("newComment").toString().isEmpty()) {
                    alterSql.append(" COMMENT '").append(column.get("newComment")).append("'");
                }
                
                alterSql.append(";\n");
            }
        }
        
        // 处理新增索引
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> addedIndexes = (List<Map<String, Object>>) tableChanges.get("addedIndexes");
        if (addedIndexes != null && !addedIndexes.isEmpty()) {
            for (Map<String, Object> index : addedIndexes) {
                if (Boolean.TRUE.equals(index.get("isPrimary"))) {
                    alterSql.append("ALTER TABLE `").append(tableName).append("` ADD PRIMARY KEY (")
                           .append(index.get("columnNames")).append(");\n");
                } else {
                    String indexType = Boolean.TRUE.equals(index.get("isUnique")) ? "UNIQUE INDEX" : "INDEX";
                    alterSql.append("ALTER TABLE `").append(tableName).append("` ADD ").append(indexType)
                           .append(" `").append(index.get("indexName")).append("` (")
                           .append(index.get("columnNames")).append(");\n");
                }
            }
        }
        
        // 处理删除索引
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> removedIndexes = (List<Map<String, Object>>) tableChanges.get("removedIndexes");
        if (removedIndexes != null && !removedIndexes.isEmpty()) {
            for (Map<String, Object> index : removedIndexes) {
                if (Boolean.TRUE.equals(index.get("isPrimary"))) {
                    alterSql.append("ALTER TABLE `").append(tableName).append("` DROP PRIMARY KEY;\n");
                } else {
                    alterSql.append("ALTER TABLE `").append(tableName).append("` DROP INDEX `")
                           .append(index.get("indexName")).append("`;").append("\n");
                }
            }
        }
        
        return alterSql.toString();
    }
    
    @Override
    public Map<String, Object> getVersionCompleteStructure(Long projectVersionId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 获取项目版本信息
            ProjectVersion projectVersion = projectVersionMapper.selectById(projectVersionId);
            if (projectVersion == null) {
                result.put("error", "项目版本不存在");
                return result;
            }

            // 2. 获取项目信息
            Project project = projectMapper.selectById(projectVersion.getProjectId());
            String datasourceType = null;
            if (project != null && project.getDatasourceId() != null) {
                // 3. 获取数据源信息
                Datasource datasource = datasourceMapper.selectById(project.getDatasourceId());
                if (datasource != null) {
                    datasourceType = datasource.getType();
                }
            }

            // 4. 获取数据库结构信息
            VersionDatabaseSchema databaseSchema = getVersionDatabaseSchema(projectVersionId);
            if (databaseSchema != null) {
                Map<String, Object> databaseInfo = new HashMap<>();
                databaseInfo.put("databaseName", databaseSchema.getDatabaseName());
                databaseInfo.put("charset", databaseSchema.getCharset());
                databaseInfo.put("collation", databaseSchema.getCollation());
                databaseInfo.put("snapshotTime", databaseSchema.getSnapshotTime());
                databaseInfo.put("schemasInfo", databaseSchema.getSchemasInfo());
                result.put("database", databaseInfo);
            }

            // 5. 添加数据源类型信息
            result.put("datasourceType", datasourceType);

            // 6. 获取表结构列表
            List<VersionTableStructure> tableStructures = getVersionTableStructures(projectVersionId);
            List<Map<String, Object>> tables = new ArrayList<>();
            
            for (VersionTableStructure tableStructure : tableStructures) {
                Map<String, Object> tableInfo = new HashMap<>();
                tableInfo.put("id", tableStructure.getId());
                tableInfo.put("tableName", tableStructure.getTableName());
                tableInfo.put("schemaName", tableStructure.getSchemaName());
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
                
                // 7. 获取表的字段信息
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
                
                // 8. 获取表的索引信息
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
    
    /**
     * 删除指定版本的已存在数据
     */
    private void deleteExistingVersionData(Long projectVersionId) {
        try {
            log.info("开始删除项目版本 {} 的已存在数据库结构信息", projectVersionId);

            // 检查删除前的数据量
            QueryWrapper<VersionTableStructure> checkWrapper = new QueryWrapper<>();
            checkWrapper.eq("project_version_id", projectVersionId);
            List<VersionTableStructure> existingTables = versionTableStructureMapper.selectList(checkWrapper);
            log.info("删除前，项目版本 {} 有 {} 个表结构", projectVersionId, existingTables.size());

            // 使用原生SQL强制删除，确保数据完全清理
            // 1. 先删除字段和索引信息（通过JOIN删除）
            int deletedColumns = versionTableColumnMapper.deleteByVersionId(projectVersionId);
            log.info("删除了 {} 个表字段记录", deletedColumns);

            int deletedIndexes = versionTableIndexMapper.deleteByVersionId(projectVersionId);
            log.info("删除了 {} 个表索引记录", deletedIndexes);

            // 2. 删除表结构信息
            QueryWrapper<VersionTableStructure> tableQueryWrapper = new QueryWrapper<>();
            tableQueryWrapper.eq("project_version_id", projectVersionId);
            int deletedTables = versionTableStructureMapper.delete(tableQueryWrapper);
            log.info("删除了 {} 个表结构记录", deletedTables);

            // 3. 删除版本数据库结构信息
            QueryWrapper<VersionDatabaseSchema> databaseQueryWrapper = new QueryWrapper<>();
            databaseQueryWrapper.eq("project_version_id", projectVersionId);
            int deletedDatabases = versionDatabaseSchemaMapper.delete(databaseQueryWrapper);
            log.info("删除了 {} 个数据库结构记录", deletedDatabases);

            // 4. 强制刷新事务，确保删除操作立即生效
            try {
                // 等待一小段时间确保删除操作完成
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 5. 再次检查是否还有残留数据
            List<VersionTableStructure> remainingTables = versionTableStructureMapper.selectList(checkWrapper);
            if (!remainingTables.isEmpty()) {
                log.warn("删除后仍有 {} 个表结构记录残留，将强制删除", remainingTables.size());
                for (VersionTableStructure table : remainingTables) {
                    versionTableStructureMapper.deleteById(table.getId());
                }
            }

            log.info("已完成删除项目版本 {} 的已存在数据库结构信息", projectVersionId);
        } catch (Exception e) {
            log.error("删除已存在版本数据时出现异常: {}", e.getMessage(), e);
            throw new RuntimeException("删除已存在版本数据失败: " + e.getMessage(), e);
        }
    }
}