-- =====================================================================
-- AI 助手管理模块补充权限（FR-027 / FR-028 / FR-036 / FR-037）
-- 适用：已初始化过的库，可重复执行（INSERT IGNORE）
-- 执行：mysql -h127.0.0.1 -uroot -p123456 nougyou < doc/ai_module_permissions.sql
-- =====================================================================

USE `nougyou`;

-- 1. 农户商家：销量统计菜单 + 可行性分析按钮（挂在已有「AI 对话」菜单下）
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '销量统计', 'ai:salesStat', 'MENU', 2 FROM `sys_permission` WHERE `code` = 'ai:chat:menu';

INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '销售可行性分析', 'ai:analysis', 'BUTTON', 1 FROM `sys_permission` WHERE `code` = 'ai:salesStat';

-- 2. 管理员：AI 对话监控
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
VALUES (0, 'AI 对话监控', 'ai:adminSession', 'MENU', 40);

INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, 'AI 会话全文', 'ai:adminMessage', 'BUTTON', 1 FROM `sys_permission` WHERE `code` = 'ai:adminSession';

-- 3. 管理员：AI 提示词管理
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
VALUES (0, 'AI 提示词管理', 'ai:promptList', 'MENU', 41);

INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '新增提示词版本', 'ai:promptAdd', 'BUTTON', 1 FROM `sys_permission` WHERE `code` = 'ai:promptList';

INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '启用指定版本', 'ai:promptEnable', 'BUTTON', 2 FROM `sys_permission` WHERE `code` = 'ai:promptList';

-- 4. 授权：管理员获得全部 7 个新权限点
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN (
  'ai:salesStat','ai:analysis','ai:adminSession','ai:adminMessage',
  'ai:promptList','ai:promptAdd','ai:promptEnable'
)
WHERE r.`code` = 'ADMIN';

-- 5. 授权：农户商家获得销量统计与可行性分析
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN ('ai:salesStat','ai:analysis')
WHERE r.`code` = 'FARMER';
