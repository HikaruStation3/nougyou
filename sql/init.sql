CREATE DATABASE IF NOT EXISTS `nougyou` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `nougyou`;

CREATE TABLE IF NOT EXISTS `sys_dept` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父级ID，0为根',
    `name`        VARCHAR(64)  NOT NULL COMMENT '部门名称',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门表';

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `code`        VARCHAR(64)  NOT NULL COMMENT '角色编码',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    `nickname`    VARCHAR(64)           DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(512)          DEFAULT NULL COMMENT '头像地址',
    `mobile`      VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    `gender`      TINYINT               DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `dept_id`     BIGINT                DEFAULT NULL COMMENT '部门ID',
    `role_id`     BIGINT                DEFAULT NULL COMMENT '角色ID',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父级权限ID，0为根',
    `name`        VARCHAR(64)  NOT NULL COMMENT '权限名称',
    `code`        VARCHAR(100) NOT NULL COMMENT '权限标识，如 sys:user:query',
    `type`        VARCHAR(16)  NOT NULL DEFAULT 'BUTTON' COMMENT '类型：MENU 菜单 / BUTTON 操作',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '权限点表';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `role_id`       BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`role_id`, `permission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限关联表';

CREATE TABLE IF NOT EXISTS `farm_project` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `farmer_id`      BIGINT       NOT NULL                COMMENT '所属农户商家用户ID',
    `title`          VARCHAR(150) NOT NULL                COMMENT '项目名称',
    `project_type`   VARCHAR(30)  NOT NULL DEFAULT '农家乐' COMMENT '项目类型',
    `cover`          VARCHAR(255)          DEFAULT NULL   COMMENT '封面图',
    `images`         TEXT                                  COMMENT '环境图集（逗号分隔）',
    `address`        VARCHAR(200)          DEFAULT NULL   COMMENT '项目地址',
    `intro`          TEXT                                  COMMENT '项目介绍',
    `price_desc`     VARCHAR(100)          DEFAULT NULL   COMMENT '价格说明，如 人均80元',
    `capacity`       INT                   DEFAULT NULL   COMMENT '可接待人数',
    `contact_phone`  VARCHAR(20)           DEFAULT NULL   COMMENT '联系电话',
    `business_hours` VARCHAR(80)           DEFAULT NULL   COMMENT '营业时间',
    `audit_status`   TINYINT      NOT NULL DEFAULT 0      COMMENT '审核状态 0待审核 1通过 2驳回',
    `audit_user_id`  BIGINT                DEFAULT NULL   COMMENT '审核管理员ID',
    `audit_time`     DATETIME              DEFAULT NULL   COMMENT '审核时间',
    `audit_remark`   VARCHAR(255)          DEFAULT NULL   COMMENT '审核意见',
    `shelf_status`   TINYINT      NOT NULL DEFAULT 1      COMMENT '1已上架 0已下架',
    `view_count`     INT          NOT NULL DEFAULT 0      COMMENT '浏览量',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_farmer` (`farmer_id`),
    KEY `idx_audit` (`audit_status`,`shelf_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '扶贫项目（农家乐）表';
