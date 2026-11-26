-- =====================================================================
-- 售后服务模块权限（FR-008 买家提交与查询 / FR-019 商家处理与平台介入）
-- 接口：API-061 ~ API-066
-- 适用：已初始化过的库，可重复执行
-- 执行：mysql -h127.0.0.1 -uroot -p123456 nougyou < doc/after_sale_module_permissions.sql
-- =====================================================================

USE `nougyou`;

-- 1. 权限点
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`) VALUES
(0, '提交售后申请',   'afterSale:add',        'BUTTON', 70),
(0, '我的售后列表',   'afterSale:list',       'BUTTON', 71),
(0, '售后详情',       'afterSale:detail',     'BUTTON', 72),
(0, '商家售后列表',   'afterSale:farmerList', 'BUTTON', 73),
(0, '处理售后',       'afterSale:handle',     'BUTTON', 74),
(0, '平台介入处理',   'afterSale:intervene',  'BUTTON', 75);

-- 2. 角色授权
-- 管理员：查看全部售后 + 平台介入终裁
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN ('afterSale:detail','afterSale:intervene')
WHERE r.`code` = 'ADMIN';

-- 买家：提交申请、查询自己的售后、查看详情
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN ('afterSale:add','afterSale:list','afterSale:detail')
WHERE r.`code` = 'BUYER';

-- 农户商家：本店售后列表、受理/拒绝、查看详情
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN ('afterSale:farmerList','afterSale:handle','afterSale:detail')
WHERE r.`code` = 'FARMER';
