-- =====================================================================
-- 沟通咨询模块（FR-009 平台客服 / FR-010 联系商家 / FR-032 客服响应）
-- 适用：已初始化过的库，可重复执行
-- 执行：mysql -h127.0.0.1 -uroot -p123456 nougyou < doc/consult_module_permissions.sql
-- =====================================================================

USE `nougyou`;

-- 1. 会话商品上下文：从商品页发起咨询时记录 product_id（API-067）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_session' AND COLUMN_NAME = 'product_id'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `service_session` ADD COLUMN `product_id` BIGINT DEFAULT NULL COMMENT ''咨询商品ID（从商品页发起时记录）'' AFTER `target_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 买家/农户：我的咨询菜单 + 会话操作按钮
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
VALUES (0, '沟通咨询', 'service:list', 'MENU', 5);

INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '发起咨询', 'service:create', 'BUTTON', 1 FROM `sys_permission` WHERE `code` = 'service:list';

INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '会话消息', 'service:message', 'BUTTON', 2 FROM `sys_permission` WHERE `code` = 'service:list';

INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '发送消息', 'service:send', 'BUTTON', 3 FROM `sys_permission` WHERE `code` = 'service:list';

INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '标记已读', 'service:read', 'BUTTON', 4 FROM `sys_permission` WHERE `code` = 'service:list';

-- 3. 管理员：客服工作台菜单 + 接入/结束按钮
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
VALUES (0, '客服工作台', 'service:adminList', 'MENU', 42);

INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '客服接入会话', 'service:accept', 'BUTTON', 1 FROM `sys_permission` WHERE `code` = 'service:adminList';

INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '客服结束会话', 'service:close', 'BUTTON', 2 FROM `sys_permission` WHERE `code` = 'service:adminList';

-- 4. 授权：管理员获得接入/结束/查看全部会话，以及会话读写能力
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN (
  'service:adminList','service:accept','service:close',
  'service:message','service:send','service:read'
)
WHERE r.`code` = 'ADMIN';

-- 5. 授权：买家与农户商家获得咨询入口与会话收发
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN (
  'service:list','service:create','service:message','service:send','service:read'
)
WHERE r.`code` IN ('BUYER','FARMER');
