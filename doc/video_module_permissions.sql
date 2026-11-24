-- =====================================================================
-- 短视频模块（FR-011 观看 / FR-012 点赞评论关注 / FR-020 发布 /
--            FR-021 审核进度 / FR-035 管理员审核）
-- 适用：已初始化过的库，可重复执行
-- 执行：mysql -h127.0.0.1 -uroot -p123456 nougyou < doc/video_module_permissions.sql
-- =====================================================================

USE `nougyou`;

-- 1. 权限点：观看类接口对游客开放，权限点区分「登录后可互动」与「发布 / 审核」
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`) VALUES
(0, '助农短视频',        'video:list',          'MENU',   80),
(0, '短视频详情',        'video:detail',        'BUTTON', 81),
(0, '短视频点赞',        'video:like',          'BUTTON', 82),
(0, '短视频评论列表',    'video:commentList',   'BUTTON', 83),
(0, '发表短视频评论',    'video:comment',       'BUTTON', 84),
(0, '删除短视频评论',    'video:commentDelete', 'BUTTON', 85),
(0, '关注商家',          'video:follow',        'BUTTON', 86),
(0, '商家主页',          'video:farmerProfile', 'BUTTON', 87),
(0, '发布短视频',        'video:add',           'BUTTON', 88),
(0, '我的短视频',        'video:my',            'BUTTON', 89),
(0, '删除短视频',        'video:delete',        'BUTTON', 90),
(0, '短视频列表(管理端)', 'video:adminList',     'BUTTON', 91),
(0, '短视频审核',        'video:audit',         'BUTTON', 92);

-- 2. 角色授权
INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN (
  'video:list','video:detail','video:like','video:commentList','video:comment',
  'video:commentDelete','video:follow','video:farmerProfile',
  'video:add','video:my','video:delete','video:adminList','video:audit'
)
WHERE r.`code` = 'ADMIN';

INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN (
  'video:list','video:detail','video:like','video:commentList','video:comment',
  'video:commentDelete','video:follow','video:farmerProfile',
  'video:add','video:my','video:delete'
)
WHERE r.`code` = 'FARMER';

INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN (
  'video:list','video:detail','video:like','video:commentList','video:comment',
  'video:commentDelete','video:follow','video:farmerProfile'
)
WHERE r.`code` = 'BUYER';

-- 3. 演示素材：把种子数据里占位的 /upload/video/* 换成随包提供的演示视频，
--    已有真实上传记录（地址不是 /upload/video/ 开头）不会被改动。
UPDATE `video` SET `cover` = '/api/v1/media/cover/v1.svg', `video_url` = '/api/v1/media/video/v1.mp4', `duration` = 10
WHERE `id` = 1 AND `video_url` LIKE '/upload/video/%';
UPDATE `video` SET `cover` = '/api/v1/media/cover/v2.svg', `video_url` = '/api/v1/media/video/v2.mp4', `duration` = 10
WHERE `id` = 2 AND `video_url` LIKE '/upload/video/%';
UPDATE `video` SET `cover` = '/api/v1/media/cover/v3.svg', `video_url` = '/api/v1/media/video/v3.mp4', `duration` = 5
WHERE `id` = 3 AND `video_url` LIKE '/upload/video/%';
UPDATE `video` SET `cover` = '/api/v1/media/cover/v4.svg', `video_url` = '/api/v1/media/video/v4.mp4', `duration` = 6
WHERE `id` = 4 AND `video_url` LIKE '/upload/video/%';
UPDATE `video` SET `cover` = '/api/v1/media/cover/v6.svg', `video_url` = '/api/v1/media/video/v5.mp4', `duration` = 10
WHERE `id` = 5 AND `video_url` LIKE '/upload/video/%';
