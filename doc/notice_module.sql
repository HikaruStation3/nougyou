-- ============================================================
-- 通知公告模块（管理员发布 → 全站/指定用户接收）
-- 依赖：sys_user、sys_permission（权限点由 DataInitializer 启动时自动补齐）
-- 执行：mysql -uroot -p123456 nougyou < doc/notice_module.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS `notice` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `title`          varchar(200) NOT NULL COMMENT '通知标题',
  `content`        longtext              COMMENT '通知内容（富文本 HTML）',
  `type`           tinyint      NOT NULL DEFAULT '1' COMMENT '类型：1通知 2公告 3提醒',
  `level`          varchar(8)   NOT NULL DEFAULT 'L' COMMENT '等级：L普通 M重要 H紧急',
  `target_type`    tinyint      NOT NULL DEFAULT '1' COMMENT '目标：1全体用户 2指定用户',
  `publish_status` tinyint      NOT NULL DEFAULT '0' COMMENT '状态：0未发布 1已发布 -1已撤回',
  `publisher_id`   bigint                DEFAULT NULL COMMENT '发布人（管理员）ID',
  `publish_time`   datetime              DEFAULT NULL COMMENT '发布时间',
  `revoke_time`    datetime              DEFAULT NULL COMMENT '撤回时间',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        tinyint      NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`publish_status`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告';

CREATE TABLE IF NOT EXISTS `notice_target` (
  `id`         bigint NOT NULL AUTO_INCREMENT,
  `notice_id`  bigint NOT NULL COMMENT '通知ID',
  `user_id`    bigint NOT NULL COMMENT '接收用户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知指定接收人（target_type=2 时生效）';

CREATE TABLE IF NOT EXISTS `notice_read` (
  `id`        bigint   NOT NULL AUTO_INCREMENT,
  `notice_id` bigint   NOT NULL COMMENT '通知ID',
  `user_id`   bigint   NOT NULL COMMENT '用户ID',
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知已读记录（无记录=未读）';
