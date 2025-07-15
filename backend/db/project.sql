-- 项目表
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '项目名称',
  `description` TEXT COMMENT '项目描述',
  `datasource_id` BIGINT UNSIGNED NULL COMMENT '数据源ID',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态(0禁用,1启用)',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建用户ID',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_datasource_id` (`datasource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';