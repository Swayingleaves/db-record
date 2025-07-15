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