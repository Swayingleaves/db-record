-- 数据库初始化脚本
-- 按照依赖关系顺序创建表

create database if not exists db_record;

use db_record;


-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '加密密码',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态(0禁用,1启用)',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 数据源表
DROP TABLE IF EXISTS `datasource`;
CREATE TABLE `datasource` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '数据源名称',
  `type` VARCHAR(20) NOT NULL COMMENT '数据源类型(mysql/postgresql/oracle等)',
  `host` VARCHAR(100) NOT NULL COMMENT '主机地址',
  `port` INT NOT NULL COMMENT '端口',
  `database_name` VARCHAR(100) NOT NULL COMMENT '数据库名',
  `username` VARCHAR(100) NOT NULL COMMENT '用户名',
  `password` VARCHAR(200) NOT NULL COMMENT '密码',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态(0禁用,1启用)',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建用户ID',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';

-- 项目表
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '项目名称',
  `description` TEXT COMMENT '项目描述',
  `datasource_id` BIGINT UNSIGNED COMMENT '关联数据源ID',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态(0禁用,1启用)',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建用户ID',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_datasource_id` (`datasource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 项目版本表
DROP TABLE IF EXISTS `project_version`;
CREATE TABLE `project_version` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `version_name` VARCHAR(50) NOT NULL COMMENT '版本名称',
  `description` TEXT COMMENT '版本描述',
  `schema_snapshot` LONGTEXT COMMENT '数据库结构快照(JSON格式)',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态(0禁用,1启用)',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建用户ID',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_version` (`project_id`, `version_name`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目版本表';

-- SQL执行记录表
DROP TABLE IF EXISTS `sql_record`;
CREATE TABLE `sql_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '项目ID',
  `sql_content` LONGTEXT NOT NULL COMMENT 'SQL内容',
  `execute_result` LONGTEXT COMMENT '执行结果',
  `execute_status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '执行状态(0失败,1成功)',
  `execute_time` BIGINT COMMENT '执行时间(毫秒)',
  `error_message` TEXT COMMENT '错误信息',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '执行用户ID',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL执行记录表';

-- 版本数据库结构表
DROP TABLE IF EXISTS `version_database_schema`;
CREATE TABLE `version_database_schema` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_version_id` BIGINT UNSIGNED NOT NULL COMMENT '项目版本ID',
  `database_name` VARCHAR(100) NOT NULL COMMENT '数据库名称',
  `charset` VARCHAR(50) COMMENT '字符集',
  `collation` VARCHAR(50) COMMENT '排序规则',
  `schemas_info` JSON COMMENT 'Schema信息（PostgreSQL专用，存储所有schema的详细信息）',
  `snapshot_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建用户ID',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_database` (`project_version_id`, `database_name`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本数据库结构表';

-- 版本表结构表
DROP TABLE IF EXISTS `version_table_structure`;
CREATE TABLE `version_table_structure` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_version_id` BIGINT UNSIGNED NOT NULL COMMENT '项目版本ID',
  `table_name` VARCHAR(100) NOT NULL COMMENT '表名',
  `table_comment` TEXT COMMENT '表注释',
  `table_type` VARCHAR(50) DEFAULT 'BASE TABLE' COMMENT '表类型',
  `engine` VARCHAR(50) COMMENT '存储引擎',
  `charset` VARCHAR(50) COMMENT '字符集',
  `collation` VARCHAR(50) COMMENT '排序规则',
  `row_format` VARCHAR(50) COMMENT '行格式',
  `table_rows` BIGINT COMMENT '表行数',
  `avg_row_length` BIGINT COMMENT '平均行长度',
  `data_length` BIGINT COMMENT '数据长度',
  `index_length` BIGINT COMMENT '索引长度',
  `auto_increment` BIGINT COMMENT '自增值',
  `schema_name` VARCHAR(100) DEFAULT 'public' COMMENT 'Schema名称（PostgreSQL专用）',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_table_schema` (`project_version_id`, `schema_name`, `table_name`),
  KEY `idx_project_version` (`project_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本表结构表';

-- 版本表字段结构表
DROP TABLE IF EXISTS `version_table_column`;
CREATE TABLE `version_table_column` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `version_table_id` BIGINT UNSIGNED NOT NULL COMMENT '版本表结构ID',
  `column_name` VARCHAR(100) NOT NULL COMMENT '字段名',
  `ordinal_position` INT NOT NULL COMMENT '字段位置',
  `column_default` TEXT COMMENT '默认值',
  `is_nullable` VARCHAR(3) NOT NULL DEFAULT 'YES' COMMENT '是否可空',
  `data_type` VARCHAR(50) NOT NULL COMMENT '数据类型',
  `character_maximum_length` BIGINT COMMENT '字符最大长度',
  `character_octet_length` BIGINT COMMENT '字符字节长度',
  `numeric_precision` INT COMMENT '数值精度',
  `numeric_scale` INT COMMENT '数值小数位',
  `datetime_precision` INT COMMENT '日期时间精度',
  `character_set_name` VARCHAR(50) COMMENT '字符集',
  `collation_name` VARCHAR(50) COMMENT '排序规则',
  `column_type` VARCHAR(200) NOT NULL COMMENT '列类型',
  `column_key` VARCHAR(10) COMMENT '键类型(PRI/UNI/MUL)',
  `extra` VARCHAR(100) COMMENT '额外信息',
  `column_comment` TEXT COMMENT '字段注释',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_column` (`version_table_id`, `column_name`),
  KEY `idx_version_table` (`version_table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本表字段结构表';

-- 版本表索引表
DROP TABLE IF EXISTS `version_table_index`;
CREATE TABLE `version_table_index` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `version_table_id` BIGINT UNSIGNED NOT NULL COMMENT '版本表结构ID',
  `index_name` VARCHAR(100) NOT NULL COMMENT '索引名',
  `index_type` VARCHAR(50) NOT NULL COMMENT '索引类型(BTREE/HASH/FULLTEXT)',
  `is_unique` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否唯一索引',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主键索引',
  `column_names` TEXT NOT NULL COMMENT '索引字段名(JSON数组)',
  `sub_part` TEXT COMMENT '子部分长度(JSON数组)',
  `index_comment` TEXT COMMENT '索引注释',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_index` (`version_table_id`, `index_name`),
  KEY `idx_version_table` (`version_table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本表索引表';

-- 初始化管理员用户
INSERT INTO `user` (`username`, `password`, `status`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7.QdEKLiq', 1, 'ADMIN')
ON DUPLICATE KEY UPDATE `password` = VALUES(`password`); 