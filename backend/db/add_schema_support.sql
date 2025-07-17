-- 为PostgreSQL添加schema支持的数据库迁移脚本

-- 1. 为version_table_structure表添加schema_name字段
ALTER TABLE `version_table_structure` 
ADD COLUMN `schema_name` VARCHAR(100) DEFAULT 'public' COMMENT 'Schema名称（PostgreSQL专用）' AFTER `table_name`;

-- 2. 更新唯一索引，包含schema_name
ALTER TABLE `version_table_structure` 
DROP INDEX `uk_version_table`;

ALTER TABLE `version_table_structure` 
ADD UNIQUE KEY `uk_version_table_schema` (`project_version_id`, `schema_name`, `table_name`);

-- 3. 为version_database_schema表添加schema信息字段
ALTER TABLE `version_database_schema` 
ADD COLUMN `schemas_info` JSON COMMENT 'Schema信息（PostgreSQL专用，存储所有schema的详细信息）' AFTER `collation`;

-- 4. 为现有数据设置默认schema
UPDATE `version_table_structure` SET `schema_name` = 'public' WHERE `schema_name` IS NULL;