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
