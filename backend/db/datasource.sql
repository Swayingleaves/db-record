-- 数据源表
DROP TABLE IF EXISTS `datasource`;
CREATE TABLE `datasource` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '数据源名称',
  `type` VARCHAR(20) NOT NULL DEFAULT 'mysql' COMMENT '数据源类型(mysql,postgresql,kingbase)',
  `host` VARCHAR(100) NOT NULL COMMENT '主机地址',
  `port` INT NOT NULL COMMENT '端口号',
  `database_name` VARCHAR(100) NOT NULL COMMENT '数据库名',
  `username` VARCHAR(100) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `description` TEXT COMMENT '描述',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态(0禁用,1启用)',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建用户ID',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';