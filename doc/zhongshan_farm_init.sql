/* =================================================================
   钟山镇扶贫助农系统 · 数据库初始化脚本
   -----------------------------------------------------------------
   需求依据：钟山镇扶贫助农系统需求表.xlsx
             （功能需求 FR-001 ~ FR-039 / 非功能需求 NFR-008 RBAC）
   技术栈  ：Java + SpringBoot + MyBatis-Plus + MySQL 8.0
   字符集  ：utf8mb4 / InnoDB
   内容    ：27 张业务表 + 1 个统计视图 + 初始化模拟数据
   执行方式：Navicat 中打开本文件直接运行，或
             mysql -u root -p --default-character-set=utf8mb4 < zhongshan_farm_init.sql
   -----------------------------------------------------------------
   表清单与需求映射
     一、权限体系    sys_dept / sys_role / sys_user
                     sys_permission / sys_role_permission      FR-001/029/039, NFR-008
                     （与后端 com.nougyou 现有 RBAC 实现保持一致）
     二、入驻审核    farmer_apply                              FR-013 / FR-033
     三、内容资讯    cms_policy / cms_news                     FR-003/004/015/030
     四、扶贫项目    farm_project                              FR-005/016/034
     五、农产品      product_category / product / cart         FR-006/017
     六、订单交易    orders / order_item                       FR-007/018
     七、售后服务    after_sale                                FR-008/019
     八、沟通咨询    service_session / service_message         FR-009/010/032
     九、短视频      video / video_like / video_comment
                     / video_follow                            FR-011/012/020/021/035
     十、委托销售    entrust_sale / entrust_settlement         FR-022/023/024/025
                     + 视图 v_entrust_sale_stat
     十一、AI助手    ai_chat_session / ai_chat_message
                     / ai_prompt_config / ai_content_rule      FR-026/027/028/036/037/038
   -----------------------------------------------------------------
   说明：不建物理外键，统一用索引 + 应用层校验，便于逻辑删除
         与 MyBatis-Plus 维护（NFR-006 / NFR-009）。
   ================================================================= */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

/* 库与表统一使用字符集默认排序规则，避免跨表 JOIN 出现 collation 混用报错。
   MySQL 8.0 默认 utf8mb4_0900_ai_ci，5.7 默认 utf8mb4_general_ci，均为中文安全。 */
CREATE DATABASE IF NOT EXISTS `nougyou`
  DEFAULT CHARACTER SET utf8mb4;
USE `nougyou`;

/* =================================================================
   一、权限体系（RBAC）
   与后端 com.nougyou 现有实现保持一致：
   sys_dept / sys_role / sys_user / sys_permission / sys_role_permission
   ================================================================= */

DROP TABLE IF EXISTS `sys_role_permission`;
DROP TABLE IF EXISTS `sys_permission`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_dept`;

CREATE TABLE `sys_dept` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父级ID，0为根',
    `name`        VARCHAR(64)  NOT NULL COMMENT '商户名称',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商户表';

CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `code`        VARCHAR(64)  NOT NULL COMMENT '角色编码',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    `nickname`    VARCHAR(64)           DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(512)          DEFAULT NULL COMMENT '头像地址',
    `mobile`      VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    `gender`      TINYINT               DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `dept_id`     BIGINT                DEFAULT NULL COMMENT '商户ID',
    `role_id`     BIGINT                DEFAULT NULL COMMENT '角色ID',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE `sys_permission` (
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

CREATE TABLE `sys_role_permission` (
    `role_id`       BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`role_id`, `permission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限关联表';

/* =================================================================
   二、农户商家入驻申请（FR-013 提交 / FR-033 审核）
   ================================================================= */

DROP TABLE IF EXISTS `farmer_apply`;
CREATE TABLE `farmer_apply` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `apply_no`        VARCHAR(32)  NOT NULL                COMMENT '申请单号',
  `user_id`         BIGINT       NOT NULL                COMMENT '申请用户ID',
  `real_name`       VARCHAR(50)  NOT NULL                COMMENT '真实姓名',
  `id_card`         VARCHAR(20)  NOT NULL                COMMENT '身份证号',
  `phone`           VARCHAR(20)  NOT NULL                COMMENT '联系电话',
  `village`         VARCHAR(50)  NOT NULL                COMMENT '所属村',
  `poverty_card_no` VARCHAR(50)           DEFAULT NULL   COMMENT '建档立卡贫困户编号',
  `poverty_level`   VARCHAR(20)           DEFAULT NULL   COMMENT '贫困属性 一般贫困户/低保户/特困户',
  `household_num`   INT          NOT NULL DEFAULT 1      COMMENT '家庭人口数',
  `main_product`    VARCHAR(120)          DEFAULT NULL   COMMENT '拟经营品类',
  `id_card_front`   VARCHAR(255)          DEFAULT NULL   COMMENT '身份证正面照',
  `id_card_back`    VARCHAR(255)          DEFAULT NULL   COMMENT '身份证反面照',
  `poverty_proof`   VARCHAR(255)          DEFAULT NULL   COMMENT '贫困户证明附件',
  `apply_desc`      VARCHAR(500)          DEFAULT NULL   COMMENT '申请说明',
  `audit_status`    TINYINT      NOT NULL DEFAULT 0      COMMENT '审核状态 0待审核 1通过 2驳回',
  `audit_user_id`   BIGINT                DEFAULT NULL   COMMENT '审核管理员ID',
  `audit_time`      DATETIME              DEFAULT NULL   COMMENT '审核时间',
  `audit_remark`    VARCHAR(255)          DEFAULT NULL   COMMENT '审核意见',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_audit_status` (`audit_status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贫困户入驻申请审核表';

/* =================================================================
   三、扶贫政策 / 扶贫资讯（FR-003 / FR-004 / FR-015 / FR-030）
   ================================================================= */

DROP TABLE IF EXISTS `cms_policy`;
CREATE TABLE `cms_policy` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '政策ID',
  `title`          VARCHAR(200) NOT NULL                COMMENT '政策标题',
  `policy_no`      VARCHAR(60)           DEFAULT NULL   COMMENT '政策文号',
  `issue_dept`     VARCHAR(100)          DEFAULT NULL   COMMENT '发布单位',
  `cover`          VARCHAR(255)          DEFAULT NULL   COMMENT '封面图',
  `summary`        VARCHAR(500)          DEFAULT NULL   COMMENT '摘要',
  `content`        LONGTEXT                              COMMENT '政策正文（富文本）',
  `publish_status` TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 0草稿 1已发布 2已下架',
  `publish_time`   DATETIME              DEFAULT NULL   COMMENT '发布时间',
  `view_count`     INT          NOT NULL DEFAULT 0      COMMENT '浏览量',
  `author_id`      BIGINT                DEFAULT NULL   COMMENT '创建管理员ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`publish_status`,`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扶贫政策表';

DROP TABLE IF EXISTS `cms_news`;
CREATE TABLE `cms_news` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资讯ID',
  `title`          VARCHAR(200) NOT NULL                COMMENT '资讯标题',
  `news_type`      TINYINT      NOT NULL DEFAULT 1      COMMENT '类型 1景点宣传 2旅游推介 3乡镇历史 4地域文化 5人文地理',
  `cover`          VARCHAR(255)          DEFAULT NULL   COMMENT '封面图',
  `summary`        VARCHAR(500)          DEFAULT NULL   COMMENT '摘要',
  `content`        LONGTEXT                              COMMENT '资讯正文（富文本）',
  `location`       VARCHAR(120)          DEFAULT NULL   COMMENT '关联地点（景点/村落）',
  `publish_status` TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 0草稿 1已发布 2已下架',
  `publish_time`   DATETIME              DEFAULT NULL   COMMENT '发布时间',
  `view_count`     INT          NOT NULL DEFAULT 0      COMMENT '浏览量',
  `author_id`      BIGINT                DEFAULT NULL   COMMENT '创建管理员ID',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`news_type`,`publish_status`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扶贫资讯表（景点/旅游/历史/文化/人文地理，FR-004）';

/* =================================================================
   四、扶贫项目（农家乐）：FR-005 浏览 / FR-016 申请 / FR-034 审核
   ================================================================= */

DROP TABLE IF EXISTS `farm_project`;
CREATE TABLE `farm_project` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扶贫项目（农家乐）表';

/* =================================================================
   五、农产品与购物车（FR-006 / FR-017 / FR-030）
   ================================================================= */

DROP TABLE IF EXISTS `product_category`;
CREATE TABLE `product_category` (
  `id`        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`      VARCHAR(50) NOT NULL                COMMENT '分类名称',
  `parent_id` BIGINT      NOT NULL DEFAULT 0      COMMENT '父级ID，0为一级分类',
  `icon`      VARCHAR(255)         DEFAULT NULL   COMMENT '分类图标',
  `sort`      INT         NOT NULL DEFAULT 0      COMMENT '排序',
  `status`    TINYINT     NOT NULL DEFAULT 1      COMMENT '状态 1启用 0禁用',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农产品分类表';

DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `farmer_id`      BIGINT        NOT NULL                COMMENT '所属农户商家用户ID',
  `category_id`    BIGINT        NOT NULL                COMMENT '分类ID',
  `name`           VARCHAR(150)  NOT NULL                COMMENT '商品名称',
  `cover`          VARCHAR(255)           DEFAULT NULL   COMMENT '主图',
  `images`         TEXT                                   COMMENT '图集（逗号分隔URL）',
  `price`          DECIMAL(10,2) NOT NULL                COMMENT '售价（元）',
  `original_price` DECIMAL(10,2)          DEFAULT NULL   COMMENT '原价/划线价',
  `stock`          INT           NOT NULL DEFAULT 0      COMMENT '库存',
  `unit`           VARCHAR(10)   NOT NULL DEFAULT '件'   COMMENT '计量单位',
  `weight`         DECIMAL(8,2)           DEFAULT NULL   COMMENT '单件重量（kg）',
  `origin`         VARCHAR(100)           DEFAULT NULL   COMMENT '产地',
  `is_poverty`     TINYINT       NOT NULL DEFAULT 1      COMMENT '是否扶贫助农产品 1是 0否',
  `description`    TEXT                                   COMMENT '商品详情',
  `sale_count`     INT           NOT NULL DEFAULT 0      COMMENT '累计销量',
  `shelf_status`   TINYINT       NOT NULL DEFAULT 1      COMMENT '1上架 0下架（FR-017）',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_farmer` (`farmer_id`,`shelf_status`),
  KEY `idx_category` (`category_id`,`shelf_status`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农产品表';

DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `buyer_id`    BIGINT   NOT NULL                COMMENT '买家用户ID',
  `product_id`  BIGINT   NOT NULL                COMMENT '商品ID',
  `quantity`    INT      NOT NULL DEFAULT 1      COMMENT '数量',
  `selected`    TINYINT  NOT NULL DEFAULT 1      COMMENT '是否勾选 1是 0否',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_buyer_product` (`buyer_id`,`product_id`),
  KEY `idx_buyer` (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表（在线选购，FR-006）';

/* =================================================================
   六、订单（FR-007 买家管理 / FR-018 商家处理）
   ================================================================= */

DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`         VARCHAR(32)   NOT NULL                COMMENT '订单号',
  `buyer_id`         BIGINT        NOT NULL                COMMENT '买家用户ID',
  `farmer_id`        BIGINT        NOT NULL                COMMENT '卖家（农户商家）用户ID',
  `total_amount`     DECIMAL(10,2) NOT NULL                COMMENT '商品总额',
  `freight`          DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '运费',
  `pay_amount`       DECIMAL(10,2) NOT NULL                COMMENT '应付/实付金额',
  `pay_status`       TINYINT       NOT NULL DEFAULT 0      COMMENT '支付状态 0未支付 1已支付 2已退款',
  `pay_type`         TINYINT                DEFAULT NULL   COMMENT '支付方式 1模拟支付 2微信 3支付宝',
  `pay_time`         DATETIME               DEFAULT NULL   COMMENT '支付时间',
  `order_status`     TINYINT       NOT NULL DEFAULT 0      COMMENT '0待付款 1待发货 2待收货 3已完成 4已取消 5售后中',
  `receiver_name`    VARCHAR(50)   NOT NULL                COMMENT '收货人',
  `receiver_phone`   VARCHAR(20)   NOT NULL                COMMENT '收货电话',
  `receiver_address` VARCHAR(255)  NOT NULL                COMMENT '收货地址',
  `remark`           VARCHAR(255)           DEFAULT NULL   COMMENT '买家备注',
  `logistics_company` VARCHAR(50)           DEFAULT NULL   COMMENT '物流公司',
  `logistics_no`     VARCHAR(60)            DEFAULT NULL   COMMENT '物流单号',
  `ship_time`        DATETIME               DEFAULT NULL   COMMENT '发货时间',
  `finish_time`      DATETIME               DEFAULT NULL   COMMENT '完成时间',
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_buyer` (`buyer_id`,`order_status`),
  KEY `idx_farmer` (`farmer_id`,`order_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表（按买家-商家维度拆单）';

DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id`      BIGINT        NOT NULL                COMMENT '订单ID',
  `product_id`    BIGINT        NOT NULL                COMMENT '商品ID',
  `entrust_id`    BIGINT                 DEFAULT NULL   COMMENT '委托销售关系ID（为空表示商家自营，FR-024/025）',
  `product_name`  VARCHAR(150)  NOT NULL                COMMENT '下单时商品名快照',
  `product_cover` VARCHAR(255)           DEFAULT NULL   COMMENT '下单时主图快照',
  `price`         DECIMAL(10,2) NOT NULL                COMMENT '下单单价',
  `quantity`      INT           NOT NULL                COMMENT '购买数量',
  `subtotal`      DECIMAL(10,2) NOT NULL                COMMENT '小计金额',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_entrust` (`entrust_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

/* =================================================================
   七、售后服务（FR-008 提交查询 / FR-019 处理）
   ================================================================= */

DROP TABLE IF EXISTS `after_sale`;
CREATE TABLE `after_sale` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '售后ID',
  `after_sale_no`  VARCHAR(32)   NOT NULL                COMMENT '售后单号',
  `order_id`       BIGINT        NOT NULL                COMMENT '订单ID',
  `order_item_id`  BIGINT                 DEFAULT NULL   COMMENT '订单明细ID',
  `buyer_id`       BIGINT        NOT NULL                COMMENT '买家ID',
  `farmer_id`      BIGINT        NOT NULL                COMMENT '商家ID',
  `sale_type`      TINYINT       NOT NULL DEFAULT 1      COMMENT '类型 1仅退款 2退货退款 3换货 4咨询',
  `reason`         VARCHAR(100)           DEFAULT NULL   COMMENT '售后原因',
  `description`    VARCHAR(500)           DEFAULT NULL   COMMENT '问题描述',
  `images`         TEXT                                   COMMENT '凭证图片（逗号分隔）',
  `refund_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '退款金额',
  `status`         TINYINT       NOT NULL DEFAULT 0      COMMENT '状态 0待处理 1处理中 2已完成 3已驳回',
  `handle_user_id` BIGINT                 DEFAULT NULL   COMMENT '处理人ID',
  `handle_time`    DATETIME               DEFAULT NULL   COMMENT '处理时间',
  `handle_remark`  VARCHAR(255)           DEFAULT NULL   COMMENT '处理意见',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sale_no` (`after_sale_no`),
  KEY `idx_buyer` (`buyer_id`,`status`),
  KEY `idx_farmer` (`farmer_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后诉求表';

/* =================================================================
   八、沟通咨询（FR-009 平台客服 / FR-010 联系商家 / FR-032 管理员客服）
   用 target_type 一套表兼容「平台客服」与「直接联系商家」两种方案
   ================================================================= */

DROP TABLE IF EXISTS `service_session`;
CREATE TABLE `service_session` (
  `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_no`    VARCHAR(32) NOT NULL                COMMENT '会话编号',
  `user_id`       BIGINT      NOT NULL                COMMENT '发起方用户ID',
  `user_role`     VARCHAR(20) NOT NULL                COMMENT '发起方角色 BUYER/FARMER',
  `target_type`   TINYINT     NOT NULL DEFAULT 1      COMMENT '1平台客服（FR-009） 2农户商家（FR-010）',
  `target_id`     BIGINT               DEFAULT NULL   COMMENT '目标用户ID（联系商家时为商家ID）',
  `product_id`    BIGINT               DEFAULT NULL   COMMENT '咨询商品ID（从商品页发起时记录，FR-010）',
  `admin_id`      BIGINT               DEFAULT NULL   COMMENT '接待管理员ID（FR-032）',
  `status`        TINYINT     NOT NULL DEFAULT 0      COMMENT '0排队中 1进行中 2已结束',
  `last_msg_time` DATETIME             DEFAULT NULL   COMMENT '最后消息时间',
  `start_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time`      DATETIME             DEFAULT NULL   COMMENT '结束时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_user` (`user_id`,`status`),
  KEY `idx_target` (`target_type`,`target_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服/沟通会话表';

DROP TABLE IF EXISTS `service_message`;
CREATE TABLE `service_message` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id`   BIGINT      NOT NULL                COMMENT '会话ID',
  `sender_id`    BIGINT      NOT NULL                COMMENT '发送人ID',
  `sender_role`  VARCHAR(20) NOT NULL                COMMENT '发送人角色 BUYER/FARMER/ADMIN',
  `content_type` TINYINT     NOT NULL DEFAULT 1      COMMENT '内容类型 1文本 2图片',
  `content`      TEXT        NOT NULL                COMMENT '消息内容',
  `is_read`      TINYINT     NOT NULL DEFAULT 0      COMMENT '0未读 1已读',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话消息表';

/* =================================================================
   九、短视频（FR-011 观看 / FR-012 互动 / FR-020 发布 / FR-021、FR-035 审核）
   ================================================================= */

DROP TABLE IF EXISTS `video`;
CREATE TABLE `video` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '视频ID',
  `farmer_id`     BIGINT       NOT NULL                COMMENT '发布农户商家ID',
  `title`         VARCHAR(150) NOT NULL                COMMENT '视频标题',
  `cover`         VARCHAR(255)          DEFAULT NULL   COMMENT '封面图',
  `video_url`     VARCHAR(255) NOT NULL                COMMENT '视频地址',
  `duration`      INT          NOT NULL DEFAULT 0      COMMENT '时长（秒）',
  `description`   VARCHAR(500)          DEFAULT NULL   COMMENT '简介',
  `audit_status`  TINYINT      NOT NULL DEFAULT 0      COMMENT '审核状态 0待审核 1通过 2驳回（FR-021/035）',
  `audit_user_id` BIGINT                DEFAULT NULL   COMMENT '审核管理员ID',
  `audit_time`    DATETIME              DEFAULT NULL   COMMENT '审核时间',
  `audit_remark`  VARCHAR(255)          DEFAULT NULL   COMMENT '审核意见',
  `like_count`    INT          NOT NULL DEFAULT 0      COMMENT '点赞数',
  `comment_count` INT          NOT NULL DEFAULT 0      COMMENT '评论数',
  `view_count`    INT          NOT NULL DEFAULT 0      COMMENT '播放量',
  `publish_time`  DATETIME              DEFAULT NULL   COMMENT '审核通过后的展示时间',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '1正常 0已下架',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_farmer` (`farmer_id`),
  KEY `idx_audit` (`audit_status`),
  KEY `idx_publish` (`status`,`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短视频表';

DROP TABLE IF EXISTS `video_like`;
CREATE TABLE `video_like` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `video_id`    BIGINT   NOT NULL COMMENT '视频ID',
  `user_id`     BIGINT   NOT NULL COMMENT '点赞用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_user` (`video_id`,`user_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短视频点赞表';

DROP TABLE IF EXISTS `video_comment`;
CREATE TABLE `video_comment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `video_id`    BIGINT       NOT NULL                COMMENT '视频ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '评论用户ID',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '父评论ID，0为一级评论',
  `content`     VARCHAR(500) NOT NULL                COMMENT '评论内容',
  `like_count`  INT          NOT NULL DEFAULT 0      COMMENT '评论点赞数',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '1正常 0已删除',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_video` (`video_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短视频评论表';

DROP TABLE IF EXISTS `video_follow`;
CREATE TABLE `video_follow` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL COMMENT '关注人（买家）用户ID',
  `farmer_id`   BIGINT   NOT NULL COMMENT '被关注农户商家ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_farmer` (`user_id`,`farmer_id`),
  KEY `idx_farmer` (`farmer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注农户商家表（FR-012）';

/* =================================================================
   十、委托销售（FR-022 关系建立 / FR-023 分成 / FR-024、FR-025 数据双向）
   ================================================================= */

DROP TABLE IF EXISTS `entrust_sale`;
CREATE TABLE `entrust_sale` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '委托关系ID',
  `entrust_no`      VARCHAR(32)  NOT NULL                COMMENT '委托编号',
  `entrust_user_id` BIGINT       NOT NULL                COMMENT '委托方用户ID（不便自主经营的农户）',
  `agent_user_id`   BIGINT       NOT NULL                COMMENT '受托方用户ID（代销农户商家）',
  `product_id`      BIGINT       NOT NULL                COMMENT '被委托商品ID',
  `split_ratio`     DECIMAL(5,2) NOT NULL DEFAULT 20.00  COMMENT '受托方分成比例（%），FR-023',
  `settle_cycle`    TINYINT      NOT NULL DEFAULT 2      COMMENT '结算周期 1按单 2按月 3按季',
  `agreement_desc`  VARCHAR(500)          DEFAULT NULL   COMMENT '分成方案约定说明',
  `start_date`      DATE                  DEFAULT NULL   COMMENT '生效开始日',
  `end_date`        DATE                  DEFAULT NULL   COMMENT '生效结束日',
  `status`          TINYINT      NOT NULL DEFAULT 0      COMMENT '0待受托方确认 1生效中 2已终止',
  `confirm_time`    DATETIME              DEFAULT NULL   COMMENT '受托方确认时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entrust_no` (`entrust_no`),
  KEY `idx_entrust_user` (`entrust_user_id`,`status`),
  KEY `idx_agent_user` (`agent_user_id`,`status`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委托销售关系表（FR-022）';

DROP TABLE IF EXISTS `entrust_settlement`;
CREATE TABLE `entrust_settlement` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '结算ID',
  `entrust_id`     BIGINT        NOT NULL                COMMENT '委托关系ID',
  `settle_month`   VARCHAR(7)    NOT NULL                COMMENT '结算月份 yyyy-MM',
  `sale_qty`       INT           NOT NULL DEFAULT 0      COMMENT '销量',
  `sale_amount`    DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '销售额',
  `refund_qty`     INT           NOT NULL DEFAULT 0      COMMENT '退单数量',
  `refund_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '退款金额',
  `entrust_income` DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '委托方应得',
  `agent_income`   DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '受托方应得',
  `status`         TINYINT       NOT NULL DEFAULT 0      COMMENT '0待结算 1已结算',
  `settle_time`    DATETIME               DEFAULT NULL   COMMENT '结算时间',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entrust_month` (`entrust_id`,`settle_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委托销售结算表（FR-024/025）';

/* =================================================================
   十一、AI 助手（FR-026 咨询 / FR-027、028 统计与分析 / FR-036~038 管理）
   ================================================================= */

DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '提问用户ID',
  `scene_code`  VARCHAR(30)  NOT NULL DEFAULT 'BIZ_CONSULT' COMMENT '场景 BIZ_CONSULT咨询/SALE_STAT统计/SALE_ANALYSIS可行性分析',
  `title`       VARCHAR(150)          DEFAULT NULL   COMMENT '会话标题（首问摘要）',
  `model`       VARCHAR(50)           DEFAULT NULL   COMMENT '模型标识',
  `msg_count`   INT          NOT NULL DEFAULT 0      COMMENT '消息条数',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`,`create_time`),
  KEY `idx_scene` (`scene_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id`  BIGINT       NOT NULL                COMMENT '会话ID',
  `msg_role`    VARCHAR(20)  NOT NULL                COMMENT '角色 user/assistant/system',
  `content`     LONGTEXT     NOT NULL                COMMENT '消息内容（FR-036 后台可监控）',
  `token_count` INT          NOT NULL DEFAULT 0      COMMENT 'token 消耗',
  `is_flagged`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否命中管控规则 1是（FR-038）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`,`create_time`),
  KEY `idx_flagged` (`is_flagged`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';

DROP TABLE IF EXISTS `ai_prompt_config`;
CREATE TABLE `ai_prompt_config` (
  `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `scene_code`     VARCHAR(30) NOT NULL                COMMENT '场景编码',
  `scene_name`     VARCHAR(50) NOT NULL                COMMENT '场景名称',
  `prompt_content` TEXT        NOT NULL                COMMENT '提示词内容（FR-037）',
  `version`        INT         NOT NULL DEFAULT 1      COMMENT '版本号',
  `is_enable`      TINYINT     NOT NULL DEFAULT 1      COMMENT '1启用 0停用',
  `update_user_id` BIGINT               DEFAULT NULL   COMMENT '最后修改管理员ID',
  `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_version` (`scene_code`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI提示词配置表（版本化，支持回滚）';

DROP TABLE IF EXISTS `ai_content_rule`;
CREATE TABLE `ai_content_rule` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_name`   VARCHAR(80)  NOT NULL                COMMENT '规则名称（FR-038）',
  `rule_type`   TINYINT      NOT NULL DEFAULT 1      COMMENT '1敏感词 2业务边界 3输出格式约束',
  `keywords`    VARCHAR(500)          DEFAULT NULL   COMMENT '关键词/正则，逗号分隔',
  `action`      TINYINT      NOT NULL DEFAULT 1      COMMENT '命中动作 1拦截 2改写 3提示',
  `remark`      VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
  `is_enable`   TINYINT      NOT NULL DEFAULT 1      COMMENT '1启用 0停用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI内容管控规则表';

/* =================================================================
   十二、委托销售统计视图（FR-024 / FR-025 委托双方数据双向可见）
   委托方与受托方查询同一视图，即可看到销量、销售额、退单、库存、分成
   ================================================================= */

DROP VIEW IF EXISTS `v_entrust_sale_stat`;
CREATE VIEW `v_entrust_sale_stat` AS
SELECT
  e.id                                   AS entrust_id,
  e.entrust_no                           AS entrust_no,
  e.entrust_user_id                      AS entrust_user_id,
  eu.nickname                            AS entrust_user_name,
  e.agent_user_id                        AS agent_user_id,
  au.nickname                            AS agent_user_name,
  e.product_id                           AS product_id,
  p.name                                 AS product_name,
  p.stock                                AS stock,
  p.sale_count                           AS total_sale_count,
  e.split_ratio                          AS split_ratio,
  e.status                               AS entrust_status,
  IFNULL(s.sale_qty, 0)                  AS sale_qty,
  IFNULL(s.sale_amount, 0)               AS sale_amount,
  IFNULL(r.refund_qty, 0)                AS refund_qty,
  IFNULL(r.refund_amount, 0)             AS refund_amount,
  ROUND((IFNULL(s.sale_amount, 0) - IFNULL(r.refund_amount, 0)) * (100 - e.split_ratio) / 100, 2) AS entrust_income,
  ROUND((IFNULL(s.sale_amount, 0) - IFNULL(r.refund_amount, 0)) * e.split_ratio / 100, 2)         AS agent_income
FROM `entrust_sale` e
JOIN `product` p         ON p.id = e.product_id
LEFT JOIN `sys_user` eu  ON eu.id = e.entrust_user_id
LEFT JOIN `sys_user` au  ON au.id = e.agent_user_id
LEFT JOIN (
  SELECT oi.entrust_id,
         SUM(oi.quantity) AS sale_qty,
         SUM(oi.subtotal) AS sale_amount
  FROM `order_item` oi
  JOIN `orders` o ON o.id = oi.order_id AND o.order_status <> 4 AND o.pay_status = 1
  WHERE oi.entrust_id IS NOT NULL
  GROUP BY oi.entrust_id
) s ON s.entrust_id = e.id
LEFT JOIN (
  SELECT oi.entrust_id,
         SUM(oi.quantity)     AS refund_qty,
         SUM(a.refund_amount) AS refund_amount
  FROM `after_sale` a
  JOIN `order_item` oi ON oi.id = a.order_item_id
  WHERE a.status = 2 AND oi.entrust_id IS NOT NULL
  GROUP BY oi.entrust_id
) r ON r.entrust_id = e.id;

SET FOREIGN_KEY_CHECKS = 1;


/* ################################################################
   #                                                              #
   #                    初 始 化 模 拟 数 据                        #
   #                                                              #
   ################################################################ */

USE `nougyou`;

/* ------------------ 1. 角色 ------------------ */
/* 与后端 com.nougyou.config.DataInitializer 一致：id 1 管理员 / 2 买家 / 3 农户商家 */
INSERT INTO `sys_role` (`id`,`name`,`code`,`sort`) VALUES
(1, '管理员',   'ADMIN',  1),
(2, '买家',     'BUYER',  2),
(3, '农户商家', 'FARMER', 3);

/* ------------------ 2. 商户与用户 ------------------ */
/* 商户复用 sys_dept，与后端「用户管理-商户」树一致 */
INSERT INTO `sys_dept` (`id`,`parent_id`,`name`,`sort`) VALUES
(1, 0, '商户中心',   0),
(2, 1, '绿野农场',   1),
(3, 1, '鲜果合作社', 2),
(4, 1, '云岭茶庄',   3);

/* ------------------ 3. 用户与权限 ------------------ */
/* 所有账号密码均为 123456，存 BCrypt 密文；若登录校验不通过请用
   new BCryptPasswordEncoder().encode("123456") 重新生成后替换 */
INSERT INTO `sys_user`
(`id`,`username`,`password`,`nickname`,`avatar`,`mobile`,`email`,`gender`,`status`,`dept_id`,`role_id`,`create_time`) VALUES
(1, 'buyer01',  '$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W', '小娟',      '/img/avatar/u1.png', '13905041001', 'chenxj@qq.com',            2, 1, 1, 2, '2026-08-15 10:00:00'),
(2, 'buyer02',  '$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W', '阿强',      '/img/avatar/u2.png', '13905041002', 'linzq@qq.com',             1, 1, 1, 2, '2026-08-20 14:20:00'),
(3, 'buyer03',  '$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W', '丽丽',      '/img/avatar/u3.png', '13905041003', 'huangli@163.com',          2, 1, 1, 2, '2026-09-01 09:30:00'),
(4, 'farmer01', '$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W', '吴叔山货铺', '/img/avatar/u4.png', '13905042001', 'wugp@qq.com',              1, 1, 2, 3, '2026-09-10 09:05:00'),
(5, 'farmer02', '$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W', '秀兰农场',  '/img/avatar/u5.png', '13905042002', 'zhengxl@qq.com',           2, 1, 3, 3, '2026-09-13 14:35:00'),
(6, 'farmer03', '$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W', '蔡伯土产',  '/img/avatar/u6.png', '13905042003', NULL,                       1, 1, 4, 3, '2026-10-06 16:25:00'),
(7, 'admin01',  '$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W', '系统管理员', '/img/avatar/a1.png', '13905043001', 'admin@zhongshan.gov.cn',   1, 1, 1, 1, '2026-08-01 09:00:00'),
(8, 'admin02',  '$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W', '内容审核员', '/img/avatar/a2.png', '13905043002', 'audit@zhongshan.gov.cn',   2, 1, 1, 1, '2026-08-01 09:05:00'),
(9, 'buyer04',  '$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W', '小雨',      '/img/avatar/u9.png', '13905041009', NULL,                       2, 1, 1, 2, '2026-11-18 10:00:00'),
(10,'buyer05',  '$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W', '小伟',      '/img/avatar/u10.png','13905041010', NULL,                       1, 1, 1, 2, '2026-11-20 11:00:00'),
(11,'admin',    '$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W', '管理员',    NULL,                 '13905043000', 'admin@nougyou.local',      1, 1, 1, 1, '2026-08-01 09:00:00');

/* ------------------ 3.1 菜单与权限 ------------------ */
/* 与后端 com.nougyou.config.DataInitializer 保持一致：
   4 个菜单权限 + 19 个接口/按钮权限，共 23 个权限点。
   type：MENU 菜单 / BUTTON 操作 */
INSERT INTO `sys_permission` (`id`,`parent_id`,`name`,`code`,`type`,`sort`) VALUES
(1,  0,  '用户管理',    'sys:user:menu',          'MENU',   1),
(2,  1,  '用户查询',    'sys:user:query',         'BUTTON', 1),
(3,  1,  '用户新增',    'sys:user:create',        'BUTTON', 2),
(4,  1,  '用户编辑',    'sys:user:update',        'BUTTON', 3),
(5,  1,  '用户删除',    'sys:user:delete',        'BUTTON', 4),
(6,  1,  '重置密码',    'sys:user:reset-password','BUTTON', 5),
(7,  0,  '角色权限',    'sys:role:menu',          'MENU',   2),
(8,  7,  '角色查询',    'sys:role:query',         'BUTTON', 1),
(9,  7,  '角色授权',    'sys:role:update',        'BUTTON', 2),
(10, 0,  'AI 对话',     'ai:chat:menu',           'MENU',   3),
(11, 10, 'AI 对话调用', 'ai:chat:use',            'BUTTON', 1),
(12, 0,  '文件上传',    'file:upload:menu',       'MENU',   4),
(13, 12, '文件上传',    'file:upload',            'BUTTON', 1),
(14, 12, '文件删除',    'file:delete',            'BUTTON', 2),
(15, 0,  '商品浏览',    'product:view',           'BUTTON', 5),
(16, 0,  '商品管理',    'product:manage',         'BUTTON', 6),
(17, 0,  '下单',        'order:create',           'BUTTON', 7),
(18, 0,  '订单管理',    'order:manage',           'BUTTON', 8),
(19, 0,  '入驻申请提交', 'farmerApply:submit',     'BUTTON', 9),
(20, 0,  '我的入驻申请', 'farmerApply:my',         'BUTTON', 10),
(21, 0,  '入驻申请列表', 'farmerApply:list',       'BUTTON', 11),
(22, 0,  '入驻申请详情', 'farmerApply:detail',     'BUTTON', 12),
(23, 0,  '入驻申请审核', 'farmerApply:audit',      'BUTTON', 13),
/* 沟通咨询（FR-009 / FR-010 / FR-032） */
(24, 0,  '沟通咨询',    'service:list',           'MENU',   5),
(25, 24, '发起咨询',    'service:create',         'BUTTON', 1),
(26, 24, '会话消息',    'service:message',        'BUTTON', 2),
(27, 24, '发送消息',    'service:send',           'BUTTON', 3),
(28, 24, '标记已读',    'service:read',           'BUTTON', 4),
(29, 0,  '客服工作台',  'service:adminList',      'MENU',   14),
(30, 29, '客服接入会话','service:accept',         'BUTTON', 1),
(31, 29, '客服结束会话','service:close',          'BUTTON', 2);

/* 角色授权：与后端默认配置一致（FR-039） */
INSERT INTO `sys_role_permission` (`role_id`,`permission_id`) VALUES
/* 管理员：全部 23 个权限点 */
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),
(1,7),(1,8),(1,9),
(1,10),(1,11),
(1,12),(1,13),(1,14),
(1,15),(1,16),(1,17),(1,18),
(1,19),(1,20),(1,21),(1,22),(1,23),
/* 管理员：沟通咨询全量 + 客服工作台 */
(1,24),(1,25),(1,26),(1,27),(1,28),(1,29),(1,30),(1,31),
/* 买家：商品浏览、下单、入驻申请提交与查询、沟通咨询 */
(2,15),(2,17),(2,19),(2,20),
(2,24),(2,25),(2,26),(2,27),(2,28),
/* 农户商家：AI、文件、自己的商品、订单、我的入驻申请、沟通咨询（不含平台级 product:manage） */
(3,10),(3,11),
(3,12),(3,13),(3,14),
(3,15),
(3,17),(3,18),(3,20),
(3,24),(3,25),(3,26),(3,27),(3,28);

/* ------------------ 4. 入驻申请（FR-013 / FR-033） ------------------ */
INSERT INTO `farmer_apply`
(`id`,`apply_no`,`user_id`,`real_name`,`id_card`,`phone`,`village`,`poverty_card_no`,`poverty_level`,`household_num`,`main_product`,`id_card_front`,`id_card_back`,`poverty_proof`,`apply_desc`,`audit_status`,`audit_user_id`,`audit_time`,`audit_remark`,`create_time`) VALUES
(1,'SQ202609100001',4,'吴国平','350322197805124517','13905042001','钟山村委会','3503222014001234','一般贫困户',4,'兴化米粉、山茶油','/upload/id/u4_f.jpg','/upload/id/u4_b.jpg','/upload/proof/u4.pdf','本户常年种植水稻、油茶，希望上架自产农产品，已有合作社推荐。',1,7,'2026-09-12 10:20:00','资料齐全，现场核实一致，予以通过','2026-09-10 09:10:00'),
(2,'SQ202609130002',5,'郑秀兰','350322198203086622','13905042002','湖亭村','3503222014005678','低保户',3,'土鸡蛋、土蜂蜜、农家乐','/upload/id/u5_f.jpg','/upload/id/u5_b.jpg','/upload/proof/u5.pdf','养殖土鸡80余只、蜂箱20群，另经营农家乐可接待60人。',1,7,'2026-09-15 15:40:00','现场核实通过，可同时申请农家乐项目','2026-09-13 14:40:00'),
(3,'SQ202610060003',6,'蔡建华','350322195512031234','13905042003','西林村','3503222014009911','特困户',2,'地瓜干','/upload/id/u6_f.jpg','/upload/id/u6_b.jpg','/upload/proof/u6.pdf','本人识字不多、不会操作手机店铺，希望委托同村农户代销地瓜干。',1,8,'2026-10-08 11:05:00','同意入驻；已备注委托销售需求，后续由受托方代运营','2026-10-06 16:30:00'),
(4,'SQ202611180004',9,'林小雨','350322199004152233','13905041009','钟山村委会',NULL,'一般贫困户',3,'笋干','/upload/id/u9_f.jpg','/upload/id/u9_b.jpg','/upload/proof/u9.pdf','希望销售自家竹林晒制的笋干。',0,NULL,NULL,NULL,'2026-11-19 10:11:00'),
(5,'SQ202611200005',10,'张伟','350322199208071111','13905041010','西林村',NULL,NULL,2,'服装、日用品','/upload/id/u10_f.jpg','/upload/id/u10_b.jpg',NULL,'想卖自己批发的小商品。',2,8,'2026-11-20 15:30:00','非建档立卡贫困户且经营品类与扶贫助农无关，不予通过','2026-11-20 11:20:00');

/* ------------------ 5. 扶贫政策（FR-003） ------------------ */
INSERT INTO `cms_policy` (`id`,`title`,`policy_no`,`issue_dept`,`cover`,`summary`,`content`,`publish_status`,`publish_time`,`view_count`,`author_id`) VALUES
(1,'钟山镇2026年产业扶贫到户扶持办法','钟政发〔2026〕7号','钟山镇人民政府','/img/policy/p1.jpg','对发展种养殖的脱贫户给予每户最高5000元产业奖补。','第一条 为巩固拓展脱贫攻坚成果、鼓励脱贫户自主发展生产，结合本镇实际制定本办法。第二条 扶持对象为全镇建档立卡脱贫户与监测对象……（正文略）',1,'2026-03-05 09:00:00',1286,7),
(2,'关于做好扶贫助农电商平台入驻工作的通知','钟政发〔2026〕12号','钟山镇人民政府','/img/policy/p2.jpg','明确贫困户入驻平台的材料清单与审核时限。','一、入驻条件：建档立卡脱贫户、低保户、特困户，有真实自产农产品。二、材料清单：身份证正反面、贫困户证明、拟经营品类说明……（正文略）',1,'2026-04-18 10:30:00',842,7),
(3,'钟山镇消费帮扶产品目录管理办法','钟政发〔2026〕19号','钟山镇乡村振兴办','/img/policy/p3.jpg','规范扶贫助农产品的认定与标识使用。','第一条 为规范消费帮扶产品目录管理，防止非扶贫产品冒用标识……（正文略）',1,'2026-06-02 15:20:00',531,8),
(4,'脱贫人口小额信贷贴息政策解读','钟政发〔2026〕23号','钟山镇人民政府','/img/policy/p4.jpg','5万元以下、3年期以内、免担保免抵押。','一、支持对象：建档立卡脱贫人口及边缘易致贫户。二、贷款额度：单户5万元以下……（正文略）',1,'2026-07-11 09:40:00',977,7),
(5,'钟山镇公益岗位开发与补助实施细则','钟政发〔2026〕28号','钟山镇人社服务中心','/img/policy/p5.jpg','设置保洁、护路、护林等公益性岗位。','一、岗位设置：全镇设置保洁员、护路员、护林员等公益岗62个……（正文略）',1,'2026-08-25 14:10:00',402,8),
(6,'2026年秋季雨露计划助学补助申报公告','钟政发〔2026〕31号','钟山镇人民政府','/img/policy/p6.jpg','脱贫家庭中职、高职学生可申请雨露计划补助。','一、补助对象：脱贫家庭接受中高等职业教育的在校学生……（正文略）',0,NULL,0,8);

/* ------------------ 6. 扶贫资讯（FR-004） ------------------ */
INSERT INTO `cms_news` (`id`,`title`,`news_type`,`cover`,`summary`,`content`,`location`,`publish_status`,`publish_time`,`view_count`,`author_id`) VALUES
(1,'九鲤湖：钟山镇最负盛名的山水胜景',1,'/img/news/n1.jpg','九鲤湖位于钟山镇境内，以飞瀑、祈梦文化著称。','九鲤湖景区地处钟山镇东北部，以九漈飞瀑与祈梦文化闻名……（正文略）','九鲤湖景区',1,'2026-05-06 09:00:00',2310,8),
(2,'钟山镇秋季乡村旅游线路推荐（三日游）',2,'/img/news/n2.jpg','串联九鲤湖、麦斜岩、农家乐采摘园。','第一天：九鲤湖观瀑、祈梦文化体验；第二天：麦斜岩红色研学＋秀兰农家乐……（正文略）','钟山镇全域',1,'2026-09-20 10:00:00',1655,8),
(3,'钟山镇建制沿革：从钟山公社到今日乡镇',3,'/img/news/n3.jpg','梳理钟山镇自宋代以来的行政区划变迁。','据县志记载，钟山宋代属兴化县……1984年改设钟山乡，1992年撤乡建镇……（正文略）','钟山镇',1,'2026-06-15 11:20:00',689,8),
(4,'钟山方言与九鲤湖祈梦民俗',4,'/img/news/n4.jpg','介绍本地莆仙方言特色与祈梦习俗。','钟山镇通行莆仙方言，九鲤湖祈梦习俗已列入省级非物质文化遗产……（正文略）','钟山镇',1,'2026-07-02 16:30:00',1024,8),
(5,'钟山镇地形地貌与气候特征',5,'/img/news/n5.jpg','戴云山余脉丘陵地形，亚热带季风气候。','钟山镇平均海拔约420米，属戴云山余脉丘陵地带，年均气温19.6℃……（正文略）','钟山镇',1,'2026-04-12 08:50:00',458,8),
(6,'麦斜岩景区：红色文化与奇石景观并存',1,'/img/news/n6.jpg','麦斜岩为中国工农红军108团成立地。','麦斜岩位于钟山镇麦斜村，1930年中国工农红军第108团在此成立……（正文略）','麦斜岩景区',1,'2026-08-08 09:30:00',876,8);

/* ------------------ 7. 农家乐项目（FR-005 / FR-016 / FR-034） ------------------ */
INSERT INTO `farm_project`
(`id`,`farmer_id`,`title`,`project_type`,`cover`,`images`,`address`,`intro`,`price_desc`,`capacity`,`contact_phone`,`business_hours`,`audit_status`,`audit_user_id`,`audit_time`,`audit_remark`,`shelf_status`,`view_count`,`create_time`) VALUES
(1,5,'秀兰农家乐·九鲤湖店','农家乐','/img/proj/pr1.jpg','/img/proj/pr1_1.jpg,/img/proj/pr1_2.jpg','钟山镇湖亭村九鲤湖路口12号','依托九鲤湖客流，主打莆田卤面、土鸡汤、山笋炒肉，可同时容纳60人就餐，另有4间民宿客房。','人均80元',60,'13905042002','09:00-21:00',1,7,'2026-09-20 09:15:00','现场核查合格，通过',1,3480,'2026-09-18 10:00:00'),
(2,4,'吴叔山货体验园','农家乐','/img/proj/pr2.jpg','/img/proj/pr2_1.jpg','钟山镇钟山村溪边组8号','农事体验＋山货现采，含兴化米粉制作、山茶油榨制参观，可带走自采农产品。','人均60元',30,'13905042001','08:30-18:00',1,7,'2026-10-02 10:00:00','通过',1,1266,'2026-09-28 15:20:00'),
(3,6,'蔡伯地瓜干晾晒工坊','农家乐','/img/proj/pr3.jpg',NULL,'钟山镇西林村上厝5号','传统手工地瓜干三蒸三晒工艺展示，可预约参观并现场品尝。','免费参观',15,'13905042003','需提前预约',1,8,'2026-10-20 14:25:00','通过，注意安全提示',1,432,'2026-10-18 09:30:00'),
(4,5,'秀兰采摘园（枇杷季）','农家乐','/img/proj/pr4.jpg',NULL,'钟山镇湖亭村后山','枇杷、百香果采摘，季节性开放，采摘期一般为每年4月至5月。','入园30元/人',40,'13905042002','采摘季 08:00-17:00',0,NULL,NULL,NULL,1,0,'2026-11-15 10:00:00'),
(5,6,'蔡伯土产小院（餐饮）','农家乐','/img/proj/pr5.jpg',NULL,'钟山镇西林村上厝5号','计划开办家庭小餐馆，主营地瓜稀饭、番薯丸。','人均30元',12,'13905042003',NULL,2,8,'2026-11-10 11:20:00','缺少食品经营许可与消防材料，补齐后可重新提交',0,0,'2026-11-08 16:00:00');

/* ------------------ 8. 商品分类（FR-006 / FR-017） ------------------ */
INSERT INTO `product_category` (`id`,`name`,`parent_id`,`icon`,`sort`,`status`) VALUES
(1,'粮油米面',0,'/img/cate/c1.png',1,1),
(2,'禽蛋肉类',0,'/img/cate/c2.png',2,1),
(3,'山珍干货',0,'/img/cate/c3.png',3,1),
(4,'时令水果',0,'/img/cate/c4.png',4,1),
(5,'蜂蜜茶叶',0,'/img/cate/c5.png',5,1),
(6,'手工艺品',0,'/img/cate/c6.png',6,1),
(7,'米粉面食',1,NULL,1,1),
(8,'禽蛋腊味',2,NULL,1,1),
(9,'菌菇干货',3,NULL,1,1),
(10,'时令鲜果',4,NULL,1,1),
(11,'土蜂蜜',5,NULL,1,1),
(12,'竹编器物',6,NULL,1,1),
(13,'食用油',1,NULL,2,1),
(14,'笋干野菜',3,NULL,2,1),
(15,'农家干货',3,NULL,3,1),
(16,'茶叶',5,NULL,2,1);

/* ------------------ 9. 农产品（FR-017） ------------------ */
INSERT INTO `product`
(`id`,`farmer_id`,`category_id`,`name`,`cover`,`images`,`price`,`original_price`,`stock`,`unit`,`weight`,`origin`,`is_poverty`,`description`,`sale_count`,`shelf_status`,`create_time`) VALUES
(1,4,7,'钟山手工兴化米粉（2斤装）','/img/goods/g1.jpg','/img/goods/g1_1.jpg,/img/goods/g1_2.jpg',28.00,35.00,180,'袋',1.00,'钟山镇钟山村',1,'本地籼稻手工制作，久煮不糊，适合做卤面、炒米粉。',256,1,'2026-09-16 09:00:00'),
(2,4,13,'钟山农家山茶油（500ml）','/img/goods/g2.jpg','/img/goods/g2_1.jpg',88.00,108.00,60,'瓶',0.50,'钟山镇钟山村',1,'物理压榨，当年新油，酸价低于国标限值。',142,1,'2026-09-16 09:10:00'),
(3,5,8,'散养土鸡蛋（30枚）','/img/goods/g3.jpg',NULL,45.00,52.00,90,'盒',1.80,'钟山镇湖亭村',1,'林下散养，当日捡拾当日发货，破损包补。',318,1,'2026-09-21 08:30:00'),
(4,5,11,'百花土蜂蜜（500g）','/img/goods/g4.jpg','/img/goods/g4_1.jpg',68.00,80.00,45,'瓶',0.50,'钟山镇湖亭村',1,'一年一取，波美度42以上，自然成熟蜜。',205,1,'2026-09-21 08:40:00'),
(5,5,10,'湖亭枇杷（5斤装）','/img/goods/g5.jpg',NULL,59.00,69.00,0,'箱',2.50,'钟山镇湖亭村',1,'当季现摘，顺丰冷链，仅4至5月发货。',486,0,'2026-04-25 10:00:00'),
(6,4,14,'钟山笋干（250g）','/img/goods/g6.jpg',NULL,32.00,38.00,120,'袋',0.25,'钟山镇钟山村',1,'春笋炭火烘干，无硫熏，泡发率高。',167,1,'2026-10-05 09:20:00'),
(7,6,15,'蔡伯手工地瓜干（500g）','/img/goods/g7.jpg','/img/goods/g7_1.jpg',25.00,30.00,75,'袋',0.50,'钟山镇西林村',1,'三蒸三晒，不添加蔗糖与色素，软糯拉丝。',89,1,'2026-10-12 15:00:00'),
(8,5,16,'钟山高山绿茶（250g）','/img/goods/g8.jpg',NULL,76.00,90.00,38,'罐',0.25,'钟山镇湖亭村',1,'海拔600米以上茶园，明前采摘，栗香明显。',54,1,'2026-11-02 11:00:00'),
(9,6,12,'手工竹编收纳篮','/img/goods/g9.jpg',NULL,42.00,50.00,26,'个',0.80,'钟山镇西林村',1,'本地毛竹手工编制，尺寸约30×20×15cm。',31,1,'2026-11-06 10:30:00'),
(10,4,9,'钟山红菇（干货100g）','/img/goods/g10.jpg',NULL,128.00,158.00,15,'袋',0.10,'钟山镇钟山村',1,'野生红菇，雨后采摘，自然晒干。',22,1,'2026-11-10 16:40:00');

/* ------------------ 10. 购物车（FR-006） ------------------ */
INSERT INTO `cart` (`buyer_id`,`product_id`,`quantity`,`selected`,`create_time`) VALUES
(1,3,1,1,'2026-11-21 08:40:00'),
(1,7,2,1,'2026-11-21 08:41:00'),
(1,10,1,0,'2026-11-19 21:10:00'),
(2,1,2,1,'2026-11-20 19:30:00'),
(3,4,1,1,'2026-11-21 12:05:00'),
(3,8,1,1,'2026-11-21 12:06:00');

/* ------------------ 11. 订单（FR-007 / FR-018） ------------------ */
INSERT INTO `orders`
(`id`,`order_no`,`buyer_id`,`farmer_id`,`total_amount`,`freight`,`pay_amount`,`pay_status`,`pay_type`,`pay_time`,`order_status`,`receiver_name`,`receiver_phone`,`receiver_address`,`remark`,`logistics_company`,`logistics_no`,`ship_time`,`finish_time`,`create_time`) VALUES
(1,'ZS202610180001',1,4,144.00,0.00,144.00,1,1,'2026-10-18 10:12:00',3,'陈小娟','13905041001','莆田市城厢区龙桥街道学园路88号','米粉要礼盒装','顺丰速运','SF1234567890','2026-10-18 16:20:00','2026-10-20 11:05:00','2026-10-18 10:08:00'),
(2,'ZS202610180002',2,5,113.00,8.00,121.00,1,2,'2026-10-18 14:25:00',3,'林志强','13905041002','莆田市荔城区拱辰街道延寿路12号',NULL,'中通快递','ZT9876543210','2026-10-19 09:10:00','2026-10-21 15:30:00','2026-10-18 14:20:00'),
(3,'ZS202610250003',1,4,25.00,6.00,31.00,1,1,'2026-10-25 09:30:00',3,'陈小娟','13905041001','莆田市城厢区龙桥街道学园路88号','帮邻居一起买的','顺丰速运','SF1234567892','2026-10-25 15:00:00','2026-10-27 10:20:00','2026-10-25 09:26:00'),
(4,'ZS202611020004',3,5,45.00,0.00,45.00,1,1,'2026-11-02 20:05:00',1,'黄丽','13905041003','莆田市涵江区涵东街道新涵街5号',NULL,NULL,NULL,NULL,NULL,'2026-11-02 20:01:00'),
(5,'ZS202611080005',2,4,176.00,0.00,176.00,1,1,'2026-11-08 11:40:00',5,'林志强','13905041002','莆田市荔城区拱辰街道延寿路12号','两瓶山茶油，请加固包装','顺丰速运','SF1234567893','2026-11-09 08:50:00',NULL,'2026-11-08 11:35:00'),
(6,'ZS202611150006',1,5,68.00,6.00,74.00,0,NULL,NULL,0,'陈小娟','13905041001','莆田市城厢区龙桥街道学园路88号',NULL,NULL,NULL,NULL,NULL,'2026-11-15 19:50:00'),
(7,'ZS202611200007',3,5,50.00,6.00,56.00,1,1,'2026-11-20 09:15:00',3,'黄丽','13905041003','莆田市涵江区涵东街道新涵街5号','一袋有受潮','中通快递','ZT9876543214','2026-11-20 14:00:00','2026-11-21 09:00:00','2026-11-20 09:10:00'),
(8,'ZS202611180008',2,5,76.00,0.00,76.00,0,NULL,NULL,4,'林志强','13905041002','莆田市荔城区拱辰街道延寿路12号','下错单了',NULL,NULL,NULL,NULL,'2026-11-18 22:30:00'),
(9,'ZS202611210009',1,4,32.00,6.00,38.00,1,2,'2026-11-21 08:05:00',2,'陈小娟','13905041001','莆田市城厢区龙桥街道学园路88号',NULL,'顺丰速运','SF1234567895','2026-11-21 15:30:00',NULL,'2026-11-21 08:00:00');

INSERT INTO `order_item` (`id`,`order_id`,`product_id`,`entrust_id`,`product_name`,`product_cover`,`price`,`quantity`,`subtotal`) VALUES
(1, 1, 1, NULL,'钟山手工兴化米粉（2斤装）','/img/goods/g1.jpg',28.00,2, 56.00),
(2, 1, 2, NULL,'钟山农家山茶油（500ml）','/img/goods/g2.jpg',88.00,1, 88.00),
(3, 2, 3, NULL,'散养土鸡蛋（30枚）','/img/goods/g3.jpg',45.00,1, 45.00),
(4, 2, 4, NULL,'百花土蜂蜜（500g）','/img/goods/g4.jpg',68.00,1, 68.00),
(5, 3, 7, 1,   '蔡伯手工地瓜干（500g）','/img/goods/g7.jpg',25.00,1, 25.00),
(6, 4, 3, NULL,'散养土鸡蛋（30枚）','/img/goods/g3.jpg',45.00,1, 45.00),
(7, 5, 2, NULL,'钟山农家山茶油（500ml）','/img/goods/g2.jpg',88.00,2,176.00),
(8, 6, 4, NULL,'百花土蜂蜜（500g）','/img/goods/g4.jpg',68.00,1, 68.00),
(9, 7, 7, 2,   '蔡伯手工地瓜干（500g）','/img/goods/g7.jpg',25.00,2, 50.00),
(10,8, 8, NULL,'钟山高山绿茶（250g）','/img/goods/g8.jpg',76.00,1, 76.00),
(11,9, 6, NULL,'钟山笋干（250g）','/img/goods/g6.jpg',32.00,1, 32.00);

/* ------------------ 12. 售后（FR-008 / FR-019） ------------------ */
INSERT INTO `after_sale`
(`id`,`after_sale_no`,`order_id`,`order_item_id`,`buyer_id`,`farmer_id`,`sale_type`,`reason`,`description`,`images`,`refund_amount`,`status`,`handle_user_id`,`handle_time`,`handle_remark`,`create_time`) VALUES
(1,'SH202611200001',7,9,3,5,2,'商品受潮','收到的一袋地瓜干有受潮发霉现象，申请退货退款。','/upload/as/a1.jpg',25.00,2,5,'2026-11-20 16:40:00','已退款25元，问题商品无需寄回','2026-11-20 09:20:00'),
(2,'SH202610190002',2,3,2,5,1,'少发一件','买的30枚鸡蛋只收到20枚，请核实补发。','/upload/as/a2.jpg',15.00,2,5,'2026-10-19 15:00:00','已补发10枚，运单 ZT9876543219','2026-10-19 11:00:00'),
(3,'SH202611210003',5,7,2,4,3,'瓶身破损','山茶油一瓶瓶盖处渗油，申请换货。',NULL,0.00,1,4,'2026-11-21 09:30:00','已联系买家补拍照片，核实后寄新瓶','2026-11-21 08:50:00'),
(4,'SH202611100004',1,1,1,4,4,'食用方法咨询','米粉泡多久比较合适？顺便问下能不能开发票。',NULL,0.00,0,NULL,NULL,NULL,'2026-11-10 20:10:00');

/* ------------------ 13. 客服会话（FR-009 / FR-010 / FR-032） ------------------ */
INSERT INTO `service_session`
(`id`,`session_no`,`user_id`,`user_role`,`target_type`,`target_id`,`admin_id`,`status`,`last_msg_time`,`start_time`,`end_time`) VALUES
(1,'SV202611180001',1,'BUYER', 1,NULL,7,2,'2026-11-18 10:35:00','2026-11-18 10:28:00','2026-11-18 10:40:00'),
(2,'SV202611190002',3,'BUYER', 2,5,  NULL,2,'2026-11-19 14:22:00','2026-11-19 14:15:00','2026-11-19 14:30:00'),
(3,'SV202611210003',2,'BUYER', 1,NULL,8,1,'2026-11-21 19:12:00','2026-11-21 19:00:00',NULL),
(4,'SV202611210004',4,'FARMER',1,NULL,NULL,0,'2026-11-21 19:20:00','2026-11-21 19:20:00',NULL);

INSERT INTO `service_message` (`session_id`,`sender_id`,`sender_role`,`content_type`,`content`,`is_read`,`create_time`) VALUES
(1,1,'BUYER', 1,'你好，请问订单 ZS202610180001 能开发票吗？',1,'2026-11-18 10:28:00'),
(1,7,'ADMIN', 1,'可以，请在订单详情页填写抬头与税号，我们3个工作日内开出电子发票。',1,'2026-11-18 10:32:00'),
(1,1,'BUYER', 1,'好的，谢谢！',1,'2026-11-18 10:35:00'),
(2,3,'BUYER', 1,'请问枇杷什么时候能再上架？',1,'2026-11-19 14:15:00'),
(2,5,'FARMER',1,'明年4月中旬，到时候会在短视频里提前预告，可以先关注我。',1,'2026-11-19 14:22:00'),
(3,2,'BUYER', 1,'我的订单显示待收货，但物流三天没更新了。',0,'2026-11-21 19:00:00'),
(3,2,'BUYER', 2,'这是运单截图，麻烦帮忙查一下',0,'2026-11-21 19:12:00'),
(4,4,'FARMER',1,'请问委托销售的结算规则在哪里看？',1,'2026-11-21 19:20:00');

/* ------------------ 14. 短视频（FR-011 / FR-012 / FR-020 / FR-021 / FR-035） ------------------ */
INSERT INTO `video`
(`id`,`farmer_id`,`title`,`cover`,`video_url`,`duration`,`description`,`audit_status`,`audit_user_id`,`audit_time`,`audit_remark`,`like_count`,`comment_count`,`view_count`,`publish_time`,`status`,`create_time`) VALUES
(1,4,'兴化米粉手工制作全过程','/img/video/v1.jpg','/upload/video/v1.mp4',96,'从浸米、磨浆到上锅蒸、切丝、晒干，一双手做了三十年。',1,8,'2026-09-25 10:00:00','内容真实、无违规，通过',128,3,2150,'2026-09-25 10:05:00',1,'2026-09-24 20:10:00'),
(2,5,'九鲤湖边的农家乐一日','/img/video/v2.jpg','/upload/video/v2.mp4',135,'带你看看我们家的农家菜、客房和后山果园。',1,8,'2026-10-08 09:20:00','通过',96,2,1780,'2026-10-08 09:30:00',1,'2026-10-07 18:40:00'),
(3,6,'蔡伯的地瓜干为什么这么甜','/img/video/v3.jpg','/upload/video/v3.mp4',58,'三蒸三晒，不加一粒糖，甜是地瓜自己的甜。',1,8,'2026-10-22 15:40:00','审核通过；建议补充字幕便于年长用户观看',64,2,905,'2026-10-22 15:45:00',1,'2026-10-21 16:30:00'),
(4,5,'山茶油开榨啦','/img/video/v4.jpg','/upload/video/v4.mp4',72,'今年第一批茶籽下榨，记录榨油全过程。',0,NULL,NULL,NULL,0,0,0,NULL,1,'2026-11-21 10:20:00'),
(5,4,'米粉里吃出异物，别买别家','/img/video/v5.jpg','/upload/video/v5.mp4',30,'（标题含贬损同行表述）',2,8,'2026-11-10 11:00:00','标题与内容含贬损竞争对手表述，驳回修改后重新提交',0,0,0,NULL,0,'2026-11-09 21:00:00');

INSERT INTO `video_like` (`video_id`,`user_id`,`create_time`) VALUES
(1,1,'2026-09-26 08:10:00'),
(1,2,'2026-09-26 12:30:00'),
(1,3,'2026-09-27 19:00:00'),
(2,1,'2026-10-09 09:15:00'),
(2,3,'2026-10-10 20:40:00'),
(3,2,'2026-10-23 10:00:00'),
(3,9,'2026-10-24 11:20:00');

INSERT INTO `video_comment` (`video_id`,`user_id`,`parent_id`,`content`,`like_count`,`status`,`create_time`) VALUES
(1,1,0,'看着就香，已经下单两袋了。',5,1,'2026-09-26 08:12:00'),
(1,4,1,'谢谢支持，明天一早就发。',2,1,'2026-09-26 08:20:00'),
(1,3,0,'这种手工米粉煮之前要泡多久？',3,1,'2026-09-27 19:02:00'),
(2,3,0,'国庆去过，卤面很好吃！',8,1,'2026-10-10 20:45:00'),
(2,5,4,'谢谢！下次来提前说，给你留靠窗的位子。',1,1,'2026-10-10 21:10:00'),
(3,2,0,'老人家手艺真好，已下单。',4,1,'2026-10-23 10:05:00'),
(3,6,6,'谢谢，欢迎来西林村看看。',1,1,'2026-10-23 11:00:00');

INSERT INTO `video_follow` (`user_id`,`farmer_id`,`create_time`) VALUES
(1,4,'2026-09-26 08:11:00'),
(1,5,'2026-10-09 09:16:00'),
(3,5,'2026-10-10 20:41:00'),
(2,4,'2026-10-23 10:06:00'),
(9,6,'2026-10-24 11:21:00');

/* ------------------ 15. 委托销售关系（FR-022 / FR-023） ------------------ */
/* 蔡建华(6) 识字不多、不便操作线上设备，委托吴国平(4)、郑秀兰(5) 代销地瓜干 */
INSERT INTO `entrust_sale`
(`id`,`entrust_no`,`entrust_user_id`,`agent_user_id`,`product_id`,`split_ratio`,`settle_cycle`,`agreement_desc`,`start_date`,`end_date`,`status`,`confirm_time`,`create_time`) VALUES
(1,'WT202610120001',6,4,7,25.00,2,'蔡伯地瓜干由吴国平在其店铺代运营线上销售，受托方按销售额提取25%作为代销服务费，运费由受托方承担，每月5日前结算上月。','2026-10-15','2027-10-14',1,'2026-10-14 09:30:00','2026-10-12 16:00:00'),
(2,'WT202610120002',6,5,7,20.00,2,'蔡伯地瓜干同时由郑秀兰在秀兰农场店铺代销，分成20%；发生退单时货款由委托方承担，运费由受托方承担。','2026-10-15','2027-10-14',1,'2026-10-14 10:10:00','2026-10-12 16:20:00'),
(3,'WT202611050003',6,4,9,30.00,3,'手工竹编收纳篮委托吴国平代销，因属手工艺品、发货耗时较长，分成提高至30%，按季结算。','2026-11-06','2027-11-05',0,NULL,'2026-11-05 09:00:00');

/* ------------------ 16. 委托销售结算（FR-024 / FR-025） ------------------ */
/* 说明：结算表为按月的汇集结果，订单表中仅保留了部分抽样订单，故金额不逐一对应 */
INSERT INTO `entrust_settlement`
(`entrust_id`,`settle_month`,`sale_qty`,`sale_amount`,`refund_qty`,`refund_amount`,`entrust_income`,`agent_income`,`status`,`settle_time`) VALUES
(1,'2026-10',12,300.00,0,  0.00,225.00,75.00,1,'2026-11-05 10:00:00'),
(2,'2026-10', 6,150.00,0,  0.00,120.00,30.00,1,'2026-11-05 10:05:00'),
(2,'2026-11', 4,100.00,1, 25.00, 60.00,15.00,0,NULL),
(1,'2026-11', 9,225.00,0,  0.00,168.75,56.25,0,NULL);

/* ------------------ 17. AI 助手会话与消息（FR-026 ~ FR-028 / FR-036） ------------------ */
INSERT INTO `ai_chat_session` (`id`,`user_id`,`scene_code`,`title`,`model`,`msg_count`,`create_time`) VALUES
(1,4,'BIZ_CONSULT',  '怎么给山茶油设置活动价？','qwen-plus',4,'2026-11-12 10:00:00'),
(2,5,'SALE_STAT',    '帮我统计上个月土鸡蛋销量','qwen-plus',2,'2026-11-05 09:30:00'),
(3,4,'SALE_ANALYSIS','笋干这个品类还值得继续备货吗？','qwen-plus',4,'2026-11-18 15:20:00'),
(4,5,'BIZ_CONSULT',  '买家申请退货退款我怎么处理？','qwen-plus',2,'2026-11-20 16:20:00');

INSERT INTO `ai_chat_message` (`session_id`,`msg_role`,`content`,`token_count`,`is_flagged`,`create_time`) VALUES
(1,'user',     '怎么给山茶油设置活动价？',26,0,'2026-11-12 10:00:00'),
(1,'assistant','在【商品管理-我的商品】中编辑该商品，把原价填108元、售价填88元，商详页会自动显示划线价；活动结束后改回原价即可，避免长期低价影响利润。',62,0,'2026-11-12 10:00:08'),
(1,'user',     '活动结束要改回来吗？',14,0,'2026-11-12 10:02:00'),
(1,'assistant','建议改回。长期低于成本价销售会拉低该品类的价格锚点，也容易被平台判定为异常低价。',40,0,'2026-11-12 10:02:07'),
(2,'user',     '帮我统计上个月土鸡蛋销量',15,0,'2026-11-05 09:30:00'),
(2,'assistant','2026年10月土鸡蛋共售出318盒，销售额14310元，退单1笔（退款15元），当前库存90盒，按近30天日均10.6盒测算约可支撑8.5天。',78,0,'2026-11-05 09:30:06'),
(3,'user',     '笋干这个品类还值得继续备货吗？',18,0,'2026-11-18 15:20:00'),
(3,'assistant','结论：可以维持现有备货量。依据：近90天售出167袋，月均约56袋，当前库存120袋约支撑2个月，退货率为0。建议：保持现备货量，可小幅提价2至3元试销，观察两周转化率变化。',96,0,'2026-11-18 15:20:09'),
(3,'user',     '能不能帮我找找外面更便宜的进货渠道',20,0,'2026-11-18 15:24:00'),
(3,'assistant','该问题超出平台服务范围，建议咨询本村合作社或镇乡村振兴办。',24,1,'2026-11-18 15:24:05'),
(4,'user',     '买家申请退货退款我怎么处理？',16,0,'2026-11-20 16:20:00'),
(4,'assistant','进入【售后服务】找到该售后单，核对凭证照片与退款金额后点击处理：同意则填写退款说明并确认，系统按原路退回；不同意的须在48小时内填写驳回理由，否则系统默认同意。',72,0,'2026-11-20 16:20:07');

/* ------------------ 18. AI 提示词配置（FR-037） ------------------ */
INSERT INTO `ai_prompt_config` (`scene_code`,`scene_name`,`prompt_content`,`version`,`is_enable`,`update_user_id`,`create_time`) VALUES
('BIZ_CONSULT','经营业务咨询','你是钟山镇扶贫助农平台的经营助手，只回答与本平台商品管理、订单处理、委托销售、售后流程相关的问题；不回答价格谈判、外部进货渠道、法律纠纷类问题；回答控制在150字以内，涉及操作路径须给出具体菜单名称。',1,1,7,'2026-09-01 10:00:00'),
('SALE_STAT','销售数据统计','根据系统提供的销量、销售额、退单量、库存数据生成简短统计结论。不得编造数据，数据缺失时必须明确说明缺什么，不得用行业均值替代。输出不超过120字。',1,1,7,'2026-09-01 10:05:00'),
('SALE_ANALYSIS','销售可行性分析','结合近90天销量趋势与库存周转天数，判断该品类是否值得继续备货。输出结构固定为三行：结论、依据、建议动作。依据必须引用具体数字。',1,0,7,'2026-09-01 10:10:00'),
('SALE_ANALYSIS','销售可行性分析','结合近90天销量趋势与库存周转天数，判断该品类是否值得继续备货。输出结构固定为三行：结论、依据、建议动作，依据必须引用具体数字；若库存周转超过60天，须明确提示压货风险。',2,1,8,'2026-10-15 14:00:00');

/* ------------------ 19. AI 内容管控规则（FR-038） ------------------ */
INSERT INTO `ai_content_rule` (`rule_name`,`rule_type`,`keywords`,`action`,`remark`,`is_enable`) VALUES
('政治敏感词拦截',1,'（示例词表，实际由管理员维护）',1,'命中后直接拒答并记录',1),
('业务边界约束',2,'股票,基金,贷款中介,代开发票,套现',3,'提示超出平台服务范围',1),
('联系方式外流拦截',2,'微信,加我,私聊,手机号是多少',2,'改写为引导站内下单或站内客服',1),
('外部渠道比价拦截',2,'哪里进货更便宜,批发市场,1688',3,'提示仅回答平台内经营问题',1),
('输出格式约束',3,'^.{0,}$',1,'单次回复不超过150字，禁止输出表格',1);

/* ------------------ 20. AI 管理模块补充权限（FR-027 / FR-028 / FR-036 / FR-037） ------------------
   农户商家：销量统计 + 可行性分析；管理员：对话监控 + 提示词版本管理
   与 doc/ai_module_permissions.sql 内容一致，老库直接执行该脚本即可 */
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '销量统计', 'ai:salesStat', 'MENU', 2 FROM `sys_permission` WHERE `code` = 'ai:chat:menu';
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '销售可行性分析', 'ai:analysis', 'BUTTON', 1 FROM `sys_permission` WHERE `code` = 'ai:salesStat';
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
VALUES (0, 'AI 对话监控', 'ai:adminSession', 'MENU', 40);
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, 'AI 会话全文', 'ai:adminMessage', 'BUTTON', 1 FROM `sys_permission` WHERE `code` = 'ai:adminSession';
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
VALUES (0, 'AI 提示词管理', 'ai:promptList', 'MENU', 41);
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '新增提示词版本', 'ai:promptAdd', 'BUTTON', 1 FROM `sys_permission` WHERE `code` = 'ai:promptList';
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`)
SELECT id, '启用指定版本', 'ai:promptEnable', 'BUTTON', 2 FROM `sys_permission` WHERE `code` = 'ai:promptList';

INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN (
  'ai:salesStat','ai:analysis','ai:adminSession','ai:adminMessage',
  'ai:promptList','ai:promptAdd','ai:promptEnable'
)
WHERE r.`code` = 'ADMIN';

INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN ('ai:salesStat','ai:analysis')
WHERE r.`code` = 'FARMER';

/* ------------------ 21. 订单与委托销售补充权限（FR-006 / FR-007 / FR-018 / FR-022 ~ FR-025） ------------------
   与后端 DataInitializer 保持一致；老库可直接执行本段补齐权限点。
   管理端：订单查询 + 委托结算；商家端：商家订单、发货、委托销售；买家端：下单后的支付 / 取消 / 收货 */
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`) VALUES
(0, '我的订单',      'order:list',       'BUTTON', 62),
(0, '订单详情',      'order:detail',     'BUTTON', 63),
(0, '支付订单',      'order:pay',        'BUTTON', 64),
(0, '取消订单',      'order:cancel',     'BUTTON', 65),
(0, '确认收货',      'order:confirm',    'BUTTON', 66),
(0, '商家订单列表',  'order:farmerList', 'BUTTON', 67),
(0, '订单发货',      'order:ship',       'BUTTON', 68),
(0, '后台订单查询',  'order:adminList',  'BUTTON', 69),
(0, '建立委托关系',  'entrust:add',      'BUTTON', 70),
(0, '委托关系列表',  'entrust:list',     'BUTTON', 71),
(0, '委托关系详情',  'entrust:detail',   'BUTTON', 72),
(0, '确认委托',      'entrust:confirm',  'BUTTON', 73),
(0, '终止委托',      'entrust:terminate','BUTTON', 74),
(0, '委托销售统计',  'entrust:stat',     'BUTTON', 75),
(0, '委托结算单',    'entrust:settlement','BUTTON', 76),
(0, '生成结算单',    'entrust:settle',   'BUTTON', 77);

INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN (
  'order:list','order:detail','order:pay','order:cancel','order:confirm',
  'order:farmerList','order:ship','order:adminList',
  'entrust:add','entrust:list','entrust:detail','entrust:confirm',
  'entrust:terminate','entrust:stat','entrust:settlement','entrust:settle'
)
WHERE r.`code` = 'ADMIN';

INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN (
  'order:detail','order:farmerList','order:ship',
  'entrust:add','entrust:list','entrust:detail','entrust:confirm',
  'entrust:terminate','entrust:stat','entrust:settlement'
)
WHERE r.`code` = 'FARMER';

INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` IN (
  'order:list','order:detail','order:pay','order:cancel','order:confirm'
)
WHERE r.`code` = 'BUYER';

/* ------------------ 22. 首页与工作台权限（FR-002 / FR-014） ------------------
   首页聚合接口 /api/home 免登录；商家经营概览 /api/farmer/dashboard 需要 dashboard:view。
   与后端 DataInitializer 保持一致。 */
INSERT IGNORE INTO `sys_permission` (`parent_id`,`name`,`code`,`type`,`sort`) VALUES
(0, '商家经营概览', 'dashboard:view', 'BUTTON', 78);

INSERT IGNORE INTO `sys_role_permission` (`role_id`,`permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
JOIN `sys_permission` p ON p.`code` = 'dashboard:view'
WHERE r.`code` IN ('ADMIN','FARMER');

/* =================================================================
   初始化完成
   -----------------------------------------------------------------
   数据规模：角色 3 / 用户 10 / 权限 37 / 入驻申请 5 / 政策 6
             资讯 6 / 农家乐 5 / 商品 10 / 订单 9 / 售后 4
             客服会话 4+8 / 短视频 5 / 委托关系 3 / AI 会话 4+12
             合计 244 行模拟数据
   登录账号：buyer01~04、farmer01~03、admin01~02，密码均为 123456
   ================================================================= */
