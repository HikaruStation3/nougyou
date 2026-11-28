-- MySQL dump 10.13  Distrib 8.4.13, for Linux (x86_64)
--
-- Host: localhost    Database: nougyou
-- ------------------------------------------------------
-- Server version	8.4.13

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `after_sale`
--

DROP TABLE IF EXISTS `after_sale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `after_sale` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '售后ID',
  `after_sale_no` varchar(32) NOT NULL COMMENT '售后单号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_item_id` bigint DEFAULT NULL COMMENT '订单明细ID',
  `buyer_id` bigint NOT NULL COMMENT '买家ID',
  `farmer_id` bigint NOT NULL COMMENT '商家ID',
  `sale_type` tinyint NOT NULL DEFAULT '1' COMMENT '类型 1仅退款 2退货退款 3换货 4咨询',
  `reason` varchar(100) DEFAULT NULL COMMENT '售后原因',
  `description` varchar(500) DEFAULT NULL COMMENT '问题描述',
  `images` text COMMENT '凭证图片（逗号分隔）',
  `refund_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '退款金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0待处理 1处理中 2已完成 3已驳回',
  `handle_user_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(255) DEFAULT NULL COMMENT '处理意见',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sale_no` (`after_sale_no`),
  KEY `idx_buyer` (`buyer_id`,`status`),
  KEY `idx_farmer` (`farmer_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售后诉求表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `after_sale`
--

LOCK TABLES `after_sale` WRITE;
/*!40000 ALTER TABLE `after_sale` DISABLE KEYS */;
INSERT INTO `after_sale` VALUES (1,'SH202611200001',7,9,3,5,2,'商品受潮','收到的一袋地瓜干有受潮发霉现象，申请退货退款。','/upload/as/a1.jpg',25.00,2,5,'2026-11-20 16:40:00','已退款25元，问题商品无需寄回','2026-11-20 09:20:00','2026-11-28 23:44:15'),(2,'SH202610190002',2,3,2,5,1,'少发一件','买的30枚鸡蛋只收到20枚，请核实补发。','/upload/as/a2.jpg',15.00,2,5,'2026-10-19 15:00:00','已补发10枚，运单 ZT9876543219','2026-10-19 11:00:00','2026-11-28 23:44:15'),(3,'SH202611210003',5,7,2,4,3,'瓶身破损','山茶油一瓶瓶盖处渗油，申请换货。',NULL,0.00,1,4,'2026-11-21 09:30:00','已联系买家补拍照片，核实后寄新瓶','2026-11-21 08:50:00','2026-11-28 23:44:15'),(4,'SH202611100004',1,1,1,4,4,'食用方法咨询','米粉泡多久比较合适？顺便问下能不能开发票。',NULL,0.00,0,NULL,NULL,NULL,'2026-11-10 20:10:00','2026-11-28 23:44:15');
/*!40000 ALTER TABLE `after_sale` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_message`
--

DROP TABLE IF EXISTS `ai_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `msg_role` varchar(20) NOT NULL COMMENT '角色 user/assistant/system',
  `content` longtext NOT NULL COMMENT '消息内容（FR-036 后台可监控）',
  `token_count` int NOT NULL DEFAULT '0' COMMENT 'token 消耗',
  `is_flagged` tinyint NOT NULL DEFAULT '0' COMMENT '是否命中管控规则 1是（FR-038）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`,`create_time`),
  KEY `idx_flagged` (`is_flagged`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_message`
--

LOCK TABLES `ai_chat_message` WRITE;
/*!40000 ALTER TABLE `ai_chat_message` DISABLE KEYS */;
INSERT INTO `ai_chat_message` VALUES (1,1,'user','怎么给山茶油设置活动价？',26,0,'2026-11-12 10:00:00'),(2,1,'assistant','在【商品管理-我的商品】中编辑该商品，把原价填108元、售价填88元，商详页会自动显示划线价；活动结束后改回原价即可，避免长期低价影响利润。',62,0,'2026-11-12 10:00:08'),(3,1,'user','活动结束要改回来吗？',14,0,'2026-11-12 10:02:00'),(4,1,'assistant','建议改回。长期低于成本价销售会拉低该品类的价格锚点，也容易被平台判定为异常低价。',40,0,'2026-11-12 10:02:07'),(5,2,'user','帮我统计上个月土鸡蛋销量',15,0,'2026-11-05 09:30:00'),(6,2,'assistant','2026年10月土鸡蛋共售出318盒，销售额14310元，退单1笔（退款15元），当前库存90盒，按近30天日均10.6盒测算约可支撑8.5天。',78,0,'2026-11-05 09:30:06'),(7,3,'user','笋干这个品类还值得继续备货吗？',18,0,'2026-11-18 15:20:00'),(8,3,'assistant','结论：可以维持现有备货量。依据：近90天售出167袋，月均约56袋，当前库存120袋约支撑2个月，退货率为0。建议：保持现备货量，可小幅提价2至3元试销，观察两周转化率变化。',96,0,'2026-11-18 15:20:09'),(9,3,'user','能不能帮我找找外面更便宜的进货渠道',20,0,'2026-11-18 15:24:00'),(10,3,'assistant','该问题超出平台服务范围，建议咨询本村合作社或镇乡村振兴办。',24,1,'2026-11-18 15:24:05'),(11,4,'user','买家申请退货退款我怎么处理？',16,0,'2026-11-20 16:20:00'),(12,4,'assistant','进入【售后服务】找到该售后单，核对凭证照片与退款金额后点击处理：同意则填写退款说明并确认，系统按原路退回；不同意的须在48小时内填写驳回理由，否则系统默认同意。',72,0,'2026-11-20 16:20:07'),(13,5,'user','我的地瓜干卖不动，有什么建议？',0,0,'2026-11-29 00:02:01');
/*!40000 ALTER TABLE `ai_chat_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_chat_session`
--

DROP TABLE IF EXISTS `ai_chat_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '提问用户ID',
  `scene_code` varchar(30) NOT NULL DEFAULT 'BIZ_CONSULT' COMMENT '场景 BIZ_CONSULT咨询/SALE_STAT统计/SALE_ANALYSIS可行性分析',
  `title` varchar(150) DEFAULT NULL COMMENT '会话标题（首问摘要）',
  `model` varchar(50) DEFAULT NULL COMMENT '模型标识',
  `msg_count` int NOT NULL DEFAULT '0' COMMENT '消息条数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`,`create_time`),
  KEY `idx_scene` (`scene_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_chat_session`
--

LOCK TABLES `ai_chat_session` WRITE;
/*!40000 ALTER TABLE `ai_chat_session` DISABLE KEYS */;
INSERT INTO `ai_chat_session` VALUES (1,4,'BIZ_CONSULT','怎么给山茶油设置活动价？','qwen-plus',4,'2026-11-12 10:00:00','2026-11-28 23:44:15'),(2,5,'SALE_STAT','帮我统计上个月土鸡蛋销量','qwen-plus',2,'2026-11-05 09:30:00','2026-11-28 23:44:15'),(3,4,'SALE_ANALYSIS','笋干这个品类还值得继续备货吗？','qwen-plus',4,'2026-11-18 15:20:00','2026-11-28 23:44:15'),(4,5,'BIZ_CONSULT','买家申请退货退款我怎么处理？','qwen-plus',2,'2026-11-20 16:20:00','2026-11-28 23:44:15'),(5,11,'BIZ_CONSULT','我的地瓜干卖不动，有什么建议？','glm-4-flash',0,'2026-11-29 00:02:01','2026-11-29 00:02:01');
/*!40000 ALTER TABLE `ai_chat_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_content_rule`
--

DROP TABLE IF EXISTS `ai_content_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_content_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_name` varchar(80) NOT NULL COMMENT '规则名称（FR-038）',
  `rule_type` tinyint NOT NULL DEFAULT '1' COMMENT '1敏感词 2业务边界 3输出格式约束',
  `keywords` varchar(500) DEFAULT NULL COMMENT '关键词/正则，逗号分隔',
  `action` tinyint NOT NULL DEFAULT '1' COMMENT '命中动作 1拦截 2改写 3提示',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `is_enable` tinyint NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI内容管控规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_content_rule`
--

LOCK TABLES `ai_content_rule` WRITE;
/*!40000 ALTER TABLE `ai_content_rule` DISABLE KEYS */;
INSERT INTO `ai_content_rule` VALUES (1,'政治敏感词拦截',1,'（示例词表，实际由管理员维护）',1,'命中后直接拒答并记录',1,'2026-11-28 23:44:15','2026-11-28 23:44:15'),(2,'业务边界约束',2,'股票,基金,贷款中介,代开发票,套现',3,'提示超出平台服务范围',1,'2026-11-28 23:44:15','2026-11-28 23:44:15'),(3,'联系方式外流拦截',2,'微信,加我,私聊,手机号是多少',2,'改写为引导站内下单或站内客服',1,'2026-11-28 23:44:15','2026-11-28 23:44:15'),(4,'外部渠道比价拦截',2,'哪里进货更便宜,批发市场,1688',3,'提示仅回答平台内经营问题',1,'2026-11-28 23:44:15','2026-11-28 23:44:15'),(5,'输出格式约束',3,'^.{0,}$',1,'单次回复不超过150字，禁止输出表格',1,'2026-11-28 23:44:15','2026-11-28 23:44:15');
/*!40000 ALTER TABLE `ai_content_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_prompt_config`
--

DROP TABLE IF EXISTS `ai_prompt_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_prompt_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `scene_code` varchar(30) NOT NULL COMMENT '场景编码',
  `scene_name` varchar(50) NOT NULL COMMENT '场景名称',
  `prompt_content` text NOT NULL COMMENT '提示词内容（FR-037）',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `is_enable` tinyint NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `update_user_id` bigint DEFAULT NULL COMMENT '最后修改管理员ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_version` (`scene_code`,`version`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI提示词配置表（版本化，支持回滚）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_prompt_config`
--

LOCK TABLES `ai_prompt_config` WRITE;
/*!40000 ALTER TABLE `ai_prompt_config` DISABLE KEYS */;
INSERT INTO `ai_prompt_config` VALUES (1,'BIZ_CONSULT','经营业务咨询','你是钟山镇扶贫助农平台的经营助手，只回答与本平台商品管理、订单处理、委托销售、售后流程相关的问题；不回答价格谈判、外部进货渠道、法律纠纷类问题；回答控制在150字以内，涉及操作路径须给出具体菜单名称。',1,1,7,'2026-09-01 10:00:00','2026-11-28 23:44:15'),(2,'SALE_STAT','销售数据统计','根据系统提供的销量、销售额、退单量、库存数据生成简短统计结论。不得编造数据，数据缺失时必须明确说明缺什么，不得用行业均值替代。输出不超过120字。',1,1,7,'2026-09-01 10:05:00','2026-11-28 23:44:15'),(3,'SALE_ANALYSIS','销售可行性分析','结合近90天销量趋势与库存周转天数，判断该品类是否值得继续备货。输出结构固定为三行：结论、依据、建议动作。依据必须引用具体数字。',1,0,7,'2026-09-01 10:10:00','2026-11-28 23:44:15'),(4,'SALE_ANALYSIS','销售可行性分析','结合近90天销量趋势与库存周转天数，判断该品类是否值得继续备货。输出结构固定为三行：结论、依据、建议动作，依据必须引用具体数字；若库存周转超过60天，须明确提示压货风险。',2,1,8,'2026-10-15 14:00:00','2026-11-28 23:44:15');
/*!40000 ALTER TABLE `ai_prompt_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cart`
--

DROP TABLE IF EXISTS `cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `buyer_id` bigint NOT NULL COMMENT '买家用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `selected` tinyint NOT NULL DEFAULT '1' COMMENT '是否勾选 1是 0否',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_buyer_product` (`buyer_id`,`product_id`),
  KEY `idx_buyer` (`buyer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车表（在线选购，FR-006）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart`
--

LOCK TABLES `cart` WRITE;
/*!40000 ALTER TABLE `cart` DISABLE KEYS */;
INSERT INTO `cart` VALUES (1,1,3,1,1,'2026-11-21 08:40:00','2026-11-28 23:44:15'),(2,1,7,2,1,'2026-11-21 08:41:00','2026-11-28 23:44:15'),(3,1,10,1,0,'2026-11-19 21:10:00','2026-11-28 23:44:15'),(4,2,1,2,1,'2026-11-20 19:30:00','2026-11-28 23:44:15'),(5,3,4,1,1,'2026-11-21 12:05:00','2026-11-28 23:44:15'),(6,3,8,1,1,'2026-11-21 12:06:00','2026-11-28 23:44:15');
/*!40000 ALTER TABLE `cart` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cms_news`
--

DROP TABLE IF EXISTS `cms_news`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cms_news` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资讯ID',
  `title` varchar(200) NOT NULL COMMENT '资讯标题',
  `news_type` tinyint NOT NULL DEFAULT '1' COMMENT '类型 1景点宣传 2旅游推介 3乡镇历史 4地域文化 5人文地理',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图',
  `summary` varchar(500) DEFAULT NULL COMMENT '摘要',
  `content` longtext COMMENT '资讯正文（富文本）',
  `location` varchar(120) DEFAULT NULL COMMENT '关联地点（景点/村落）',
  `publish_status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0草稿 1已发布 2已下架',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
  `author_id` bigint DEFAULT NULL COMMENT '创建管理员ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`news_type`,`publish_status`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='扶贫资讯表（景点/旅游/历史/文化/人文地理，FR-004）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cms_news`
--

LOCK TABLES `cms_news` WRITE;
/*!40000 ALTER TABLE `cms_news` DISABLE KEYS */;
INSERT INTO `cms_news` VALUES (1,'九鲤湖：钟山镇最负盛名的山水胜景',1,'/img/news/n1.jpg','九鲤湖位于钟山镇境内，以飞瀑、祈梦文化著称。','九鲤湖景区地处钟山镇东北部，以九漈飞瀑与祈梦文化闻名……（正文略）','九鲤湖景区',1,'2026-05-06 09:00:00',2310,8,'2026-11-28 23:44:15','2026-11-28 23:44:15',0),(2,'钟山镇秋季乡村旅游线路推荐（三日游）',2,'/img/news/n2.jpg','串联九鲤湖、麦斜岩、农家乐采摘园。','第一天：九鲤湖观瀑、祈梦文化体验；第二天：麦斜岩红色研学＋秀兰农家乐……（正文略）','钟山镇全域',1,'2026-09-20 10:00:00',1655,8,'2026-11-28 23:44:15','2026-11-28 23:44:15',0),(3,'钟山镇建制沿革：从钟山公社到今日乡镇',3,'/img/news/n3.jpg','梳理钟山镇自宋代以来的行政区划变迁。','据县志记载，钟山宋代属兴化县……1984年改设钟山乡，1992年撤乡建镇……（正文略）','钟山镇',1,'2026-06-15 11:20:00',689,8,'2026-11-28 23:44:15','2026-11-28 23:44:15',0),(4,'钟山方言与九鲤湖祈梦民俗',4,'/img/news/n4.jpg','介绍本地莆仙方言特色与祈梦习俗。','钟山镇通行莆仙方言，九鲤湖祈梦习俗已列入省级非物质文化遗产……（正文略）','钟山镇',1,'2026-07-02 16:30:00',1024,8,'2026-11-28 23:44:15','2026-11-28 23:44:15',0),(5,'钟山镇地形地貌与气候特征',5,'/img/news/n5.jpg','戴云山余脉丘陵地形，亚热带季风气候。','钟山镇平均海拔约420米，属戴云山余脉丘陵地带，年均气温19.6℃……（正文略）','钟山镇',1,'2026-04-12 08:50:00',458,8,'2026-11-28 23:44:15','2026-11-28 23:44:15',0),(6,'麦斜岩景区：红色文化与奇石景观并存',1,'/img/news/n6.jpg','麦斜岩为中国工农红军108团成立地。','麦斜岩位于钟山镇麦斜村，1930年中国工农红军第108团在此成立……（正文略）','麦斜岩景区',1,'2026-08-08 09:30:00',876,8,'2026-11-28 23:44:15','2026-11-28 23:44:15',0);
/*!40000 ALTER TABLE `cms_news` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cms_policy`
--

DROP TABLE IF EXISTS `cms_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cms_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '政策ID',
  `title` varchar(200) NOT NULL COMMENT '政策标题',
  `policy_no` varchar(60) DEFAULT NULL COMMENT '政策文号',
  `issue_dept` varchar(100) DEFAULT NULL COMMENT '发布单位',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图',
  `summary` varchar(500) DEFAULT NULL COMMENT '摘要',
  `content` longtext COMMENT '政策正文（富文本）',
  `publish_status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0草稿 1已发布 2已下架',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
  `author_id` bigint DEFAULT NULL COMMENT '创建管理员ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`publish_status`,`publish_time`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='扶贫政策表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cms_policy`
--

LOCK TABLES `cms_policy` WRITE;
/*!40000 ALTER TABLE `cms_policy` DISABLE KEYS */;
INSERT INTO `cms_policy` VALUES (1,'钟山镇2026年产业扶贫到户扶持办法','钟政发〔2026〕7号','钟山镇人民政府','/img/policy/p1.jpg','对发展种养殖的脱贫户给予每户最高5000元产业奖补。','第一条 为巩固拓展脱贫攻坚成果、鼓励脱贫户自主发展生产，结合本镇实际制定本办法。第二条 扶持对象为全镇建档立卡脱贫户与监测对象……（正文略）',1,'2026-03-05 09:00:00',1286,7,'2026-11-28 23:44:15','2026-11-28 23:44:15',0),(2,'关于做好扶贫助农电商平台入驻工作的通知','钟政发〔2026〕12号','钟山镇人民政府','/img/policy/p2.jpg','明确贫困户入驻平台的材料清单与审核时限。','一、入驻条件：建档立卡脱贫户、低保户、特困户，有真实自产农产品。二、材料清单：身份证正反面、贫困户证明、拟经营品类说明……（正文略）',1,'2026-04-18 10:30:00',842,7,'2026-11-28 23:44:15','2026-11-28 23:44:15',0),(3,'钟山镇消费帮扶产品目录管理办法','钟政发〔2026〕19号','钟山镇乡村振兴办','/img/policy/p3.jpg','规范扶贫助农产品的认定与标识使用。','第一条 为规范消费帮扶产品目录管理，防止非扶贫产品冒用标识……（正文略）',1,'2026-06-02 15:20:00',531,8,'2026-11-28 23:44:15','2026-11-28 23:44:15',0),(4,'脱贫人口小额信贷贴息政策解读','钟政发〔2026〕23号','钟山镇人民政府','/img/policy/p4.jpg','5万元以下、3年期以内、免担保免抵押。','一、支持对象：建档立卡脱贫人口及边缘易致贫户。二、贷款额度：单户5万元以下……（正文略）',1,'2026-07-11 09:40:00',977,7,'2026-11-28 23:44:15','2026-11-28 23:44:15',0),(5,'钟山镇公益岗位开发与补助实施细则','钟政发〔2026〕28号','钟山镇人社服务中心','/img/policy/p5.jpg','设置保洁、护路、护林等公益性岗位。','一、岗位设置：全镇设置保洁员、护路员、护林员等公益岗62个……（正文略）',1,'2026-08-25 14:10:00',402,8,'2026-11-28 23:44:15','2026-11-28 23:44:15',0),(6,'2026年秋季雨露计划助学补助申报公告','钟政发〔2026〕31号','钟山镇人民政府','/img/policy/p6.jpg','脱贫家庭中职、高职学生可申请雨露计划补助。','一、补助对象：脱贫家庭接受中高等职业教育的在校学生……（正文略）',0,NULL,0,8,'2026-11-28 23:44:15','2026-11-28 23:44:15',0);
/*!40000 ALTER TABLE `cms_policy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `entrust_sale`
--

DROP TABLE IF EXISTS `entrust_sale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entrust_sale` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '委托关系ID',
  `entrust_no` varchar(32) NOT NULL COMMENT '委托编号',
  `entrust_user_id` bigint NOT NULL COMMENT '委托方用户ID（不便自主经营的农户）',
  `agent_user_id` bigint NOT NULL COMMENT '受托方用户ID（代销农户商家）',
  `product_id` bigint NOT NULL COMMENT '被委托商品ID',
  `split_ratio` decimal(5,2) NOT NULL DEFAULT '20.00' COMMENT '受托方分成比例（%），FR-023',
  `settle_cycle` tinyint NOT NULL DEFAULT '2' COMMENT '结算周期 1按单 2按月 3按季',
  `agreement_desc` varchar(500) DEFAULT NULL COMMENT '分成方案约定说明',
  `start_date` date DEFAULT NULL COMMENT '生效开始日',
  `end_date` date DEFAULT NULL COMMENT '生效结束日',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待受托方确认 1生效中 2已终止',
  `confirm_time` datetime DEFAULT NULL COMMENT '受托方确认时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entrust_no` (`entrust_no`),
  KEY `idx_entrust_user` (`entrust_user_id`,`status`),
  KEY `idx_agent_user` (`agent_user_id`,`status`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='委托销售关系表（FR-022）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `entrust_sale`
--

LOCK TABLES `entrust_sale` WRITE;
/*!40000 ALTER TABLE `entrust_sale` DISABLE KEYS */;
INSERT INTO `entrust_sale` VALUES (1,'WT202610120001',6,4,7,25.00,2,'蔡伯地瓜干由吴国平在其店铺代运营线上销售，受托方按销售额提取25%作为代销服务费，运费由受托方承担，每月5日前结算上月。','2026-10-15','2027-10-14',1,'2026-10-14 09:30:00','2026-10-12 16:00:00','2026-11-28 23:44:15'),(2,'WT202610120002',6,5,7,20.00,2,'蔡伯地瓜干同时由郑秀兰在秀兰农场店铺代销，分成20%；发生退单时货款由委托方承担，运费由受托方承担。','2026-10-15','2027-10-14',1,'2026-10-14 10:10:00','2026-10-12 16:20:00','2026-11-28 23:44:15'),(3,'WT202611050003',6,4,9,30.00,3,'手工竹编收纳篮委托吴国平代销，因属手工艺品、发货耗时较长，分成提高至30%，按季结算。','2026-11-06','2027-11-05',0,NULL,'2026-11-05 09:00:00','2026-11-28 23:44:15');
/*!40000 ALTER TABLE `entrust_sale` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `entrust_settlement`
--

DROP TABLE IF EXISTS `entrust_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entrust_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '结算ID',
  `entrust_id` bigint NOT NULL COMMENT '委托关系ID',
  `settle_month` varchar(7) NOT NULL COMMENT '结算月份 yyyy-MM',
  `sale_qty` int NOT NULL DEFAULT '0' COMMENT '销量',
  `sale_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '销售额',
  `refund_qty` int NOT NULL DEFAULT '0' COMMENT '退单数量',
  `refund_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '退款金额',
  `entrust_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '委托方应得',
  `agent_income` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '受托方应得',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待结算 1已结算',
  `settle_time` datetime DEFAULT NULL COMMENT '结算时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entrust_month` (`entrust_id`,`settle_month`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='委托销售结算表（FR-024/025）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `entrust_settlement`
--

LOCK TABLES `entrust_settlement` WRITE;
/*!40000 ALTER TABLE `entrust_settlement` DISABLE KEYS */;
INSERT INTO `entrust_settlement` VALUES (1,1,'2026-10',12,300.00,0,0.00,225.00,75.00,1,'2026-11-05 10:00:00','2026-11-28 23:44:15'),(2,2,'2026-10',6,150.00,0,0.00,120.00,30.00,1,'2026-11-05 10:05:00','2026-11-28 23:44:15'),(3,2,'2026-11',4,100.00,1,25.00,60.00,15.00,0,NULL,'2026-11-28 23:44:15'),(4,1,'2026-11',9,225.00,0,0.00,168.75,56.25,0,NULL,'2026-11-28 23:44:15');
/*!40000 ALTER TABLE `entrust_settlement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `farm_project`
--

DROP TABLE IF EXISTS `farm_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `farm_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `farmer_id` bigint NOT NULL COMMENT '所属农户商家用户ID',
  `title` varchar(150) NOT NULL COMMENT '项目名称',
  `project_type` varchar(30) NOT NULL DEFAULT '农家乐' COMMENT '项目类型',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图',
  `images` text COMMENT '环境图集（逗号分隔）',
  `address` varchar(200) DEFAULT NULL COMMENT '项目地址',
  `intro` text COMMENT '项目介绍',
  `price_desc` varchar(100) DEFAULT NULL COMMENT '价格说明，如 人均80元',
  `capacity` int DEFAULT NULL COMMENT '可接待人数',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `business_hours` varchar(80) DEFAULT NULL COMMENT '营业时间',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态 0待审核 1通过 2驳回',
  `audit_user_id` bigint DEFAULT NULL COMMENT '审核管理员ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `shelf_status` tinyint NOT NULL DEFAULT '1' COMMENT '1已上架 0已下架',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_farmer` (`farmer_id`),
  KEY `idx_audit` (`audit_status`,`shelf_status`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='扶贫项目（农家乐）表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `farm_project`
--

LOCK TABLES `farm_project` WRITE;
/*!40000 ALTER TABLE `farm_project` DISABLE KEYS */;
INSERT INTO `farm_project` VALUES (1,5,'秀兰农家乐·九鲤湖店','农家乐','/img/proj/pr1.jpg','/img/proj/pr1_1.jpg,/img/proj/pr1_2.jpg','钟山镇湖亭村九鲤湖路口12号','依托九鲤湖客流，主打莆田卤面、土鸡汤、山笋炒肉，可同时容纳60人就餐，另有4间民宿客房。','人均80元',60,'13905042002','09:00-21:00',1,7,'2026-09-20 09:15:00','现场核查合格，通过',1,3483,'2026-09-18 10:00:00','2026-11-28 23:44:15',0),(2,4,'吴叔山货体验园','农家乐','/img/proj/pr2.jpg','/img/proj/pr2_1.jpg','钟山镇钟山村溪边组8号','农事体验＋山货现采，含兴化米粉制作、山茶油榨制参观，可带走自采农产品。','人均60元',30,'13905042001','08:30-18:00',1,7,'2026-10-02 10:00:00','通过',1,1266,'2026-09-28 15:20:00','2026-11-28 23:44:15',0),(3,6,'蔡伯地瓜干晾晒工坊','农家乐','/img/proj/pr3.jpg',NULL,'钟山镇西林村上厝5号','传统手工地瓜干三蒸三晒工艺展示，可预约参观并现场品尝。','免费参观',15,'13905042003','需提前预约',1,8,'2026-10-20 14:25:00','通过，注意安全提示',1,432,'2026-10-18 09:30:00','2026-11-28 23:44:15',0),(4,5,'秀兰采摘园（枇杷季）','农家乐','/img/proj/pr4.jpg',NULL,'钟山镇湖亭村后山','枇杷、百香果采摘，季节性开放，采摘期一般为每年4月至5月。','入园30元/人',40,'13905042002','采摘季 08:00-17:00',0,NULL,NULL,NULL,1,0,'2026-11-15 10:00:00','2026-11-28 23:44:15',0),(5,6,'蔡伯土产小院（餐饮）','农家乐','/img/proj/pr5.jpg',NULL,'钟山镇西林村上厝5号','计划开办家庭小餐馆，主营地瓜稀饭、番薯丸。','人均30元',12,'13905042003',NULL,2,8,'2026-11-10 11:20:00','缺少食品经营许可与消防材料，补齐后可重新提交',0,0,'2026-11-08 16:00:00','2026-11-28 23:44:15',0),(6,4,'213123123','农家乐','http://localhost:9000/nougyou/product/2026/11/f892ae56-a4ae-4865-a241-ae547ef6d1c1.jpg','http://localhost:9000/nougyou/product/2026/11/68c79620-4672-4eb4-85d9-c9fe0c129ebb.jpg','12354','','',NULL,'13545678901','',0,NULL,NULL,NULL,1,0,'2026-11-29 00:04:55','2026-11-29 00:04:55',0);
/*!40000 ALTER TABLE `farm_project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `farmer_apply`
--

DROP TABLE IF EXISTS `farmer_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `farmer_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `apply_no` varchar(32) NOT NULL COMMENT '申请单号',
  `user_id` bigint NOT NULL COMMENT '申请用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `id_card` varchar(20) NOT NULL COMMENT '身份证号',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `village` varchar(50) NOT NULL COMMENT '所属村',
  `poverty_card_no` varchar(50) DEFAULT NULL COMMENT '建档立卡贫困户编号',
  `poverty_level` varchar(20) DEFAULT NULL COMMENT '贫困属性 一般贫困户/低保户/特困户',
  `household_num` int NOT NULL DEFAULT '1' COMMENT '家庭人口数',
  `main_product` varchar(120) DEFAULT NULL COMMENT '拟经营品类',
  `id_card_front` varchar(255) DEFAULT NULL COMMENT '身份证正面照',
  `id_card_back` varchar(255) DEFAULT NULL COMMENT '身份证反面照',
  `poverty_proof` varchar(255) DEFAULT NULL COMMENT '贫困户证明附件',
  `apply_desc` varchar(500) DEFAULT NULL COMMENT '申请说明',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态 0待审核 1通过 2驳回',
  `audit_user_id` bigint DEFAULT NULL COMMENT '审核管理员ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_audit_status` (`audit_status`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='贫困户入驻申请审核表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `farmer_apply`
--

LOCK TABLES `farmer_apply` WRITE;
/*!40000 ALTER TABLE `farmer_apply` DISABLE KEYS */;
INSERT INTO `farmer_apply` VALUES (1,'SQ202609100001',4,'吴国平','350322197805124517','13905042001','钟山村委会','3503222014001234','一般贫困户',4,'兴化米粉、山茶油','/upload/id/u4_f.jpg','/upload/id/u4_b.jpg','/upload/proof/u4.pdf','本户常年种植水稻、油茶，希望上架自产农产品，已有合作社推荐。',1,7,'2026-09-12 10:20:00','资料齐全，现场核实一致，予以通过','2026-09-10 09:10:00','2026-11-28 23:44:15'),(2,'SQ202609130002',5,'郑秀兰','350322198203086622','13905042002','湖亭村','3503222014005678','低保户',3,'土鸡蛋、土蜂蜜、农家乐','/upload/id/u5_f.jpg','/upload/id/u5_b.jpg','/upload/proof/u5.pdf','养殖土鸡80余只、蜂箱20群，另经营农家乐可接待60人。',1,7,'2026-09-15 15:40:00','现场核实通过，可同时申请农家乐项目','2026-09-13 14:40:00','2026-11-28 23:44:15'),(3,'SQ202610060003',6,'蔡建华','350322195512031234','13905042003','西林村','3503222014009911','特困户',2,'地瓜干','/upload/id/u6_f.jpg','/upload/id/u6_b.jpg','/upload/proof/u6.pdf','本人识字不多、不会操作手机店铺，希望委托同村农户代销地瓜干。',1,8,'2026-10-08 11:05:00','同意入驻；已备注委托销售需求，后续由受托方代运营','2026-10-06 16:30:00','2026-11-28 23:44:15'),(4,'SQ202611180004',9,'林小雨','350322199004152233','13905041009','钟山村委会',NULL,'一般贫困户',3,'笋干','/upload/id/u9_f.jpg','/upload/id/u9_b.jpg','/upload/proof/u9.pdf','希望销售自家竹林晒制的笋干。',0,NULL,NULL,NULL,'2026-11-19 10:11:00','2026-11-28 23:44:15'),(5,'SQ202611200005',10,'张伟','350322199208071111','13905041010','西林村',NULL,NULL,2,'服装、日用品','/upload/id/u10_f.jpg','/upload/id/u10_b.jpg',NULL,'想卖自己批发的小商品。',2,8,'2026-11-20 15:30:00','非建档立卡贫困户且经营品类与扶贫助农无关，不予通过','2026-11-20 11:20:00','2026-11-28 23:44:15');
/*!40000 ALTER TABLE `farmer_apply` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_item`
--

DROP TABLE IF EXISTS `order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `entrust_id` bigint DEFAULT NULL COMMENT '委托销售关系ID（为空表示商家自营，FR-024/025）',
  `product_name` varchar(150) NOT NULL COMMENT '下单时商品名快照',
  `product_cover` varchar(255) DEFAULT NULL COMMENT '下单时主图快照',
  `price` decimal(10,2) NOT NULL COMMENT '下单单价',
  `quantity` int NOT NULL COMMENT '购买数量',
  `subtotal` decimal(10,2) NOT NULL COMMENT '小计金额',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_entrust` (`entrust_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_item`
--

LOCK TABLES `order_item` WRITE;
/*!40000 ALTER TABLE `order_item` DISABLE KEYS */;
INSERT INTO `order_item` VALUES (1,1,1,NULL,'钟山手工兴化米粉（2斤装）','/img/goods/g1.jpg',28.00,2,56.00),(2,1,2,NULL,'钟山农家山茶油（500ml）','/img/goods/g2.jpg',88.00,1,88.00),(3,2,3,NULL,'散养土鸡蛋（30枚）','/img/goods/g3.jpg',45.00,1,45.00),(4,2,4,NULL,'百花土蜂蜜（500g）','/img/goods/g4.jpg',68.00,1,68.00),(5,3,7,1,'蔡伯手工地瓜干（500g）','/img/goods/g7.jpg',25.00,1,25.00),(6,4,3,NULL,'散养土鸡蛋（30枚）','/img/goods/g3.jpg',45.00,1,45.00),(7,5,2,NULL,'钟山农家山茶油（500ml）','/img/goods/g2.jpg',88.00,2,176.00),(8,6,4,NULL,'百花土蜂蜜（500g）','/img/goods/g4.jpg',68.00,1,68.00),(9,7,7,2,'蔡伯手工地瓜干（500g）','/img/goods/g7.jpg',25.00,2,50.00),(10,8,8,NULL,'钟山高山绿茶（250g）','/img/goods/g8.jpg',76.00,1,76.00),(11,9,6,NULL,'钟山笋干（250g）','/img/goods/g6.jpg',32.00,1,32.00);
/*!40000 ALTER TABLE `order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `buyer_id` bigint NOT NULL COMMENT '买家用户ID',
  `farmer_id` bigint NOT NULL COMMENT '卖家（农户商家）用户ID',
  `total_amount` decimal(10,2) NOT NULL COMMENT '商品总额',
  `freight` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '运费',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '应付/实付金额',
  `pay_status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态 0未支付 1已支付 2已退款',
  `pay_type` tinyint DEFAULT NULL COMMENT '支付方式 1模拟支付 2微信 3支付宝',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `order_status` tinyint NOT NULL DEFAULT '0' COMMENT '0待付款 1待发货 2待收货 3已完成 4已取消 5售后中',
  `receiver_name` varchar(50) NOT NULL COMMENT '收货人',
  `receiver_phone` varchar(20) NOT NULL COMMENT '收货电话',
  `receiver_address` varchar(255) NOT NULL COMMENT '收货地址',
  `remark` varchar(255) DEFAULT NULL COMMENT '买家备注',
  `logistics_company` varchar(50) DEFAULT NULL COMMENT '物流公司',
  `logistics_no` varchar(60) DEFAULT NULL COMMENT '物流单号',
  `ship_time` datetime DEFAULT NULL COMMENT '发货时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_buyer` (`buyer_id`,`order_status`),
  KEY `idx_farmer` (`farmer_id`,`order_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单主表（按买家-商家维度拆单）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (1,'ZS202610180001',1,4,144.00,0.00,144.00,1,1,'2026-10-18 10:12:00',3,'陈小娟','13905041001','莆田市城厢区龙桥街道学园路88号','米粉要礼盒装','顺丰速运','SF1234567890','2026-10-18 16:20:00','2026-10-20 11:05:00','2026-10-18 10:08:00','2026-11-28 23:44:15',0),(2,'ZS202610180002',2,5,113.00,8.00,121.00,1,2,'2026-10-18 14:25:00',3,'林志强','13905041002','莆田市荔城区拱辰街道延寿路12号',NULL,'中通快递','ZT9876543210','2026-10-19 09:10:00','2026-10-21 15:30:00','2026-10-18 14:20:00','2026-11-28 23:44:15',0),(3,'ZS202610250003',1,4,25.00,6.00,31.00,1,1,'2026-10-25 09:30:00',3,'陈小娟','13905041001','莆田市城厢区龙桥街道学园路88号','帮邻居一起买的','顺丰速运','SF1234567892','2026-10-25 15:00:00','2026-10-27 10:20:00','2026-10-25 09:26:00','2026-11-28 23:44:15',0),(4,'ZS202611020004',3,5,45.00,0.00,45.00,1,1,'2026-11-02 20:05:00',1,'黄丽','13905041003','莆田市涵江区涵东街道新涵街5号',NULL,NULL,NULL,NULL,NULL,'2026-11-02 20:01:00','2026-11-28 23:44:15',0),(5,'ZS202611080005',2,4,176.00,0.00,176.00,1,1,'2026-11-08 11:40:00',5,'林志强','13905041002','莆田市荔城区拱辰街道延寿路12号','两瓶山茶油，请加固包装','顺丰速运','SF1234567893','2026-11-09 08:50:00',NULL,'2026-11-08 11:35:00','2026-11-28 23:44:15',0),(6,'ZS202611150006',1,5,68.00,6.00,74.00,0,NULL,NULL,0,'陈小娟','13905041001','莆田市城厢区龙桥街道学园路88号',NULL,NULL,NULL,NULL,NULL,'2026-11-15 19:50:00','2026-11-28 23:44:15',0),(7,'ZS202611200007',3,5,50.00,6.00,56.00,1,1,'2026-11-20 09:15:00',3,'黄丽','13905041003','莆田市涵江区涵东街道新涵街5号','一袋有受潮','中通快递','ZT9876543214','2026-11-20 14:00:00','2026-11-21 09:00:00','2026-11-20 09:10:00','2026-11-28 23:44:15',0),(8,'ZS202611180008',2,5,76.00,0.00,76.00,0,NULL,NULL,4,'林志强','13905041002','莆田市荔城区拱辰街道延寿路12号','下错单了',NULL,NULL,NULL,NULL,'2026-11-18 22:30:00','2026-11-28 23:44:15',0),(9,'ZS202611210009',1,4,32.00,6.00,38.00,1,2,'2026-11-21 08:05:00',2,'陈小娟','13905041001','莆田市城厢区龙桥街道学园路88号',NULL,'顺丰速运','SF1234567895','2026-11-21 15:30:00',NULL,'2026-11-21 08:00:00','2026-11-28 23:44:15',0);
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `farmer_id` bigint NOT NULL COMMENT '所属农户商家用户ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `name` varchar(150) NOT NULL COMMENT '商品名称',
  `cover` varchar(255) DEFAULT NULL COMMENT '主图',
  `images` text COMMENT '图集（逗号分隔URL）',
  `price` decimal(10,2) NOT NULL COMMENT '售价（元）',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价/划线价',
  `stock` int NOT NULL DEFAULT '0' COMMENT '库存',
  `unit` varchar(10) NOT NULL DEFAULT '件' COMMENT '计量单位',
  `weight` decimal(8,2) DEFAULT NULL COMMENT '单件重量（kg）',
  `origin` varchar(100) DEFAULT NULL COMMENT '产地',
  `is_poverty` tinyint NOT NULL DEFAULT '1' COMMENT '是否扶贫助农产品 1是 0否',
  `description` text COMMENT '商品详情',
  `sale_count` int NOT NULL DEFAULT '0' COMMENT '累计销量',
  `shelf_status` tinyint NOT NULL DEFAULT '1' COMMENT '1上架 0下架（FR-017）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_farmer` (`farmer_id`,`shelf_status`),
  KEY `idx_category` (`category_id`,`shelf_status`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农产品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product`
--

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (1,4,7,'钟山手工兴化米粉（2斤装）','/img/goods/g1.jpg','/img/goods/g1_1.jpg,/img/goods/g1_2.jpg',28.00,35.00,180,'袋',1.00,'钟山镇钟山村',1,'本地籼稻手工制作，久煮不糊，适合做卤面、炒米粉。',256,1,'2026-09-16 09:00:00','2026-11-28 23:44:15',0),(2,4,13,'钟山农家山茶油（500ml）','/img/goods/g2.jpg','/img/goods/g2_1.jpg',88.00,108.00,60,'瓶',0.50,'钟山镇钟山村',1,'物理压榨，当年新油，酸价低于国标限值。',142,1,'2026-09-16 09:10:00','2026-11-28 23:44:15',0),(3,5,8,'散养土鸡蛋（30枚）','/img/goods/g3.jpg',NULL,45.00,52.00,90,'盒',1.80,'钟山镇湖亭村',1,'林下散养，当日捡拾当日发货，破损包补。',318,1,'2026-09-21 08:30:00','2026-11-28 23:44:15',0),(4,5,11,'百花土蜂蜜（500g）','/img/goods/g4.jpg','/img/goods/g4_1.jpg',68.00,80.00,45,'瓶',0.50,'钟山镇湖亭村',1,'一年一取，波美度42以上，自然成熟蜜。',205,1,'2026-09-21 08:40:00','2026-11-28 23:44:15',0),(5,5,10,'湖亭枇杷（5斤装）','/img/goods/g5.jpg',NULL,59.00,69.00,0,'箱',2.50,'钟山镇湖亭村',1,'当季现摘，顺丰冷链，仅4至5月发货。',486,0,'2026-04-25 10:00:00','2026-11-28 23:44:15',0),(6,4,14,'钟山笋干（250g）','/img/goods/g6.jpg',NULL,32.00,38.00,120,'袋',0.25,'钟山镇钟山村',1,'春笋炭火烘干，无硫熏，泡发率高。',167,1,'2026-10-05 09:20:00','2026-11-28 23:44:15',0),(7,6,15,'蔡伯手工地瓜干（500g）','/img/goods/g7.jpg','/img/goods/g7_1.jpg',25.00,30.00,75,'袋',0.50,'钟山镇西林村',1,'三蒸三晒，不添加蔗糖与色素，软糯拉丝。',89,1,'2026-10-12 15:00:00','2026-11-28 23:44:15',0),(8,5,16,'钟山高山绿茶（250g）','/img/goods/g8.jpg',NULL,76.00,90.00,38,'罐',0.25,'钟山镇湖亭村',1,'海拔600米以上茶园，明前采摘，栗香明显。',54,1,'2026-11-02 11:00:00','2026-11-28 23:44:15',0),(9,6,12,'手工竹编收纳篮','/img/goods/g9.jpg',NULL,42.00,50.00,26,'个',0.80,'钟山镇西林村',1,'本地毛竹手工编制，尺寸约30×20×15cm。',31,1,'2026-11-06 10:30:00','2026-11-28 23:44:15',0),(10,4,9,'钟山红菇（干货100g）','/img/goods/g10.jpg',NULL,128.00,158.00,15,'袋',0.10,'钟山镇钟山村',1,'野生红菇，雨后采摘，自然晒干。',22,1,'2026-11-10 16:40:00','2026-11-28 23:44:15',0);
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_category`
--

DROP TABLE IF EXISTS `product_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级ID，0为一级分类',
  `icon` varchar(255) DEFAULT NULL COMMENT '分类图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1启用 0禁用',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农产品分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_category`
--

LOCK TABLES `product_category` WRITE;
/*!40000 ALTER TABLE `product_category` DISABLE KEYS */;
INSERT INTO `product_category` VALUES (1,'粮油米面',0,'/img/cate/c1.png',1,1),(2,'禽蛋肉类',0,'/img/cate/c2.png',2,1),(3,'山珍干货',0,'/img/cate/c3.png',3,1),(4,'时令水果',0,'/img/cate/c4.png',4,1),(5,'蜂蜜茶叶',0,'/img/cate/c5.png',5,1),(6,'手工艺品',0,'/img/cate/c6.png',6,1),(7,'米粉面食',1,NULL,1,1),(8,'禽蛋腊味',2,NULL,1,1),(9,'菌菇干货',3,NULL,1,1),(10,'时令鲜果',4,NULL,1,1),(11,'土蜂蜜',5,NULL,1,1),(12,'竹编器物',6,NULL,1,1),(13,'食用油',1,NULL,2,1),(14,'笋干野菜',3,NULL,2,1),(15,'农家干货',3,NULL,3,1),(16,'茶叶',5,NULL,2,1);
/*!40000 ALTER TABLE `product_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_message`
--

DROP TABLE IF EXISTS `service_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送人ID',
  `sender_role` varchar(20) NOT NULL COMMENT '发送人角色 BUYER/FARMER/ADMIN',
  `content_type` tinyint NOT NULL DEFAULT '1' COMMENT '内容类型 1文本 2图片',
  `content` text NOT NULL COMMENT '消息内容',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '0未读 1已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_message`
--

LOCK TABLES `service_message` WRITE;
/*!40000 ALTER TABLE `service_message` DISABLE KEYS */;
INSERT INTO `service_message` VALUES (1,1,1,'BUYER',1,'你好，请问订单 ZS202610180001 能开发票吗？',1,'2026-11-18 10:28:00'),(2,1,7,'ADMIN',1,'可以，请在订单详情页填写抬头与税号，我们3个工作日内开出电子发票。',1,'2026-11-18 10:32:00'),(3,1,1,'BUYER',1,'好的，谢谢！',1,'2026-11-18 10:35:00'),(4,2,3,'BUYER',1,'请问枇杷什么时候能再上架？',1,'2026-11-19 14:15:00'),(5,2,5,'FARMER',1,'明年4月中旬，到时候会在短视频里提前预告，可以先关注我。',1,'2026-11-19 14:22:00'),(6,3,2,'BUYER',1,'我的订单显示待收货，但物流三天没更新了。',0,'2026-11-21 19:00:00'),(7,3,2,'BUYER',2,'这是运单截图，麻烦帮忙查一下',0,'2026-11-21 19:12:00'),(8,4,4,'FARMER',1,'请问委托销售的结算规则在哪里看？',1,'2026-11-21 19:20:00');
/*!40000 ALTER TABLE `service_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_session`
--

DROP TABLE IF EXISTS `service_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_no` varchar(32) NOT NULL COMMENT '会话编号',
  `user_id` bigint NOT NULL COMMENT '发起方用户ID',
  `user_role` varchar(20) NOT NULL COMMENT '发起方角色 BUYER/FARMER',
  `target_type` tinyint NOT NULL DEFAULT '1' COMMENT '1平台客服（FR-009） 2农户商家（FR-010）',
  `target_id` bigint DEFAULT NULL COMMENT '目标用户ID（联系商家时为商家ID）',
  `product_id` bigint DEFAULT NULL COMMENT '咨询商品ID（从商品页发起时记录，FR-010）',
  `admin_id` bigint DEFAULT NULL COMMENT '接待管理员ID（FR-032）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0排队中 1进行中 2已结束',
  `last_msg_time` datetime DEFAULT NULL COMMENT '最后消息时间',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_user` (`user_id`,`status`),
  KEY `idx_target` (`target_type`,`target_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服/沟通会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_session`
--

LOCK TABLES `service_session` WRITE;
/*!40000 ALTER TABLE `service_session` DISABLE KEYS */;
INSERT INTO `service_session` VALUES (1,'SV202611180001',1,'BUYER',1,NULL,NULL,7,2,'2026-11-18 10:35:00','2026-11-18 10:28:00','2026-11-18 10:40:00'),(2,'SV202611190002',3,'BUYER',2,5,NULL,NULL,2,'2026-11-19 14:22:00','2026-11-19 14:15:00','2026-11-19 14:30:00'),(3,'SV202611210003',2,'BUYER',1,NULL,NULL,8,1,'2026-11-21 19:12:00','2026-11-21 19:00:00',NULL),(4,'SV202611210004',4,'FARMER',1,NULL,NULL,NULL,0,'2026-11-21 19:20:00','2026-11-21 19:20:00',NULL),(5,'SV202611298617',1,'BUYER',2,5,NULL,NULL,1,'2026-11-29 00:01:23','2026-11-29 00:01:23',NULL);
/*!40000 ALTER TABLE `service_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级ID，0为根',
  `name` varchar(64) NOT NULL COMMENT '商户名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dept`
--

LOCK TABLES `sys_dept` WRITE;
/*!40000 ALTER TABLE `sys_dept` DISABLE KEYS */;
INSERT INTO `sys_dept` VALUES (1,0,'商户中心',0,'2026-11-28 23:44:15'),(2,1,'绿野农场',1,'2026-11-28 23:44:15'),(3,1,'鲜果合作社',2,'2026-11-28 23:44:15'),(4,1,'云岭茶庄',3,'2026-11-28 23:44:15');
/*!40000 ALTER TABLE `sys_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_permission`
--

DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级权限ID，0为根',
  `name` varchar(64) NOT NULL COMMENT '权限名称',
  `code` varchar(100) NOT NULL COMMENT '权限标识，如 sys:user:query',
  `type` varchar(16) NOT NULL DEFAULT 'BUTTON' COMMENT '类型：MENU 菜单 / BUTTON 操作',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_permission`
--

LOCK TABLES `sys_permission` WRITE;
/*!40000 ALTER TABLE `sys_permission` DISABLE KEYS */;
INSERT INTO `sys_permission` VALUES (1,0,'用户管理','sys:user:menu','MENU',1,'2026-11-28 23:44:15'),(2,1,'用户查询','sys:user:query','BUTTON',1,'2026-11-28 23:44:15'),(3,1,'用户新增','sys:user:create','BUTTON',2,'2026-11-28 23:44:15'),(4,1,'用户编辑','sys:user:update','BUTTON',3,'2026-11-28 23:44:15'),(5,1,'用户删除','sys:user:delete','BUTTON',4,'2026-11-28 23:44:15'),(6,1,'重置密码','sys:user:reset-password','BUTTON',5,'2026-11-28 23:44:15'),(7,0,'角色权限','sys:role:menu','MENU',2,'2026-11-28 23:44:15'),(8,7,'角色查询','sys:role:query','BUTTON',1,'2026-11-28 23:44:15'),(9,7,'角色授权','sys:role:update','BUTTON',2,'2026-11-28 23:44:15'),(10,0,'AI 对话','ai:chat:menu','MENU',3,'2026-11-28 23:44:15'),(11,10,'AI 对话调用','ai:chat:use','BUTTON',1,'2026-11-28 23:44:15'),(12,0,'文件上传','file:upload:menu','MENU',4,'2026-11-28 23:44:15'),(13,12,'文件上传','file:upload','BUTTON',1,'2026-11-28 23:44:15'),(14,12,'文件删除','file:delete','BUTTON',2,'2026-11-28 23:44:15'),(15,0,'商品浏览','product:view','BUTTON',5,'2026-11-28 23:44:15'),(16,0,'商品管理','product:manage','BUTTON',6,'2026-11-28 23:44:15'),(17,0,'下单','order:create','BUTTON',7,'2026-11-28 23:44:15'),(18,0,'订单管理','order:manage','BUTTON',8,'2026-11-28 23:44:15'),(19,0,'入驻申请提交','farmerApply:submit','BUTTON',9,'2026-11-28 23:44:15'),(20,0,'我的入驻申请','farmerApply:my','BUTTON',10,'2026-11-28 23:44:15'),(21,0,'入驻申请列表','farmerApply:list','BUTTON',11,'2026-11-28 23:44:15'),(22,0,'入驻申请详情','farmerApply:detail','BUTTON',12,'2026-11-28 23:44:15'),(23,0,'入驻申请审核','farmerApply:audit','BUTTON',13,'2026-11-28 23:44:15'),(24,0,'沟通咨询','service:list','MENU',5,'2026-11-28 23:44:15'),(25,24,'发起咨询','service:create','BUTTON',1,'2026-11-28 23:44:15'),(26,24,'会话消息','service:message','BUTTON',2,'2026-11-28 23:44:15'),(27,24,'发送消息','service:send','BUTTON',3,'2026-11-28 23:44:15'),(28,24,'标记已读','service:read','BUTTON',4,'2026-11-28 23:44:15'),(29,0,'客服工作台','service:adminList','MENU',14,'2026-11-28 23:44:15'),(30,29,'客服接入会话','service:accept','BUTTON',1,'2026-11-28 23:44:15'),(31,29,'客服结束会话','service:close','BUTTON',2,'2026-11-28 23:44:15'),(32,10,'销量统计','ai:salesStat','MENU',2,'2026-11-28 23:44:15'),(33,32,'销售可行性分析','ai:analysis','BUTTON',1,'2026-11-28 23:44:15'),(34,0,'AI 对话监控','ai:adminSession','MENU',40,'2026-11-28 23:44:15'),(35,34,'AI 会话全文','ai:adminMessage','BUTTON',1,'2026-11-28 23:44:15'),(36,0,'AI 提示词管理','ai:promptList','MENU',41,'2026-11-28 23:44:15'),(37,36,'新增提示词版本','ai:promptAdd','BUTTON',1,'2026-11-28 23:44:15'),(38,36,'启用指定版本','ai:promptEnable','BUTTON',2,'2026-11-28 23:44:15'),(39,0,'我的订单','order:list','BUTTON',62,'2026-11-28 23:44:15'),(40,0,'订单详情','order:detail','BUTTON',63,'2026-11-28 23:44:15'),(41,0,'支付订单','order:pay','BUTTON',64,'2026-11-28 23:44:15'),(42,0,'取消订单','order:cancel','BUTTON',65,'2026-11-28 23:44:15'),(43,0,'确认收货','order:confirm','BUTTON',66,'2026-11-28 23:44:15'),(44,0,'商家订单列表','order:farmerList','BUTTON',67,'2026-11-28 23:44:15'),(45,0,'订单发货','order:ship','BUTTON',68,'2026-11-28 23:44:15'),(46,0,'后台订单查询','order:adminList','BUTTON',69,'2026-11-28 23:44:15'),(47,0,'建立委托关系','entrust:add','BUTTON',70,'2026-11-28 23:44:15'),(48,0,'委托关系列表','entrust:list','BUTTON',71,'2026-11-28 23:44:15'),(49,0,'委托关系详情','entrust:detail','BUTTON',72,'2026-11-28 23:44:15'),(50,0,'确认委托','entrust:confirm','BUTTON',73,'2026-11-28 23:44:15'),(51,0,'终止委托','entrust:terminate','BUTTON',74,'2026-11-28 23:44:15'),(52,0,'委托销售统计','entrust:stat','BUTTON',75,'2026-11-28 23:44:15'),(53,0,'委托结算单','entrust:settlement','BUTTON',76,'2026-11-28 23:44:15'),(54,0,'生成结算单','entrust:settle','BUTTON',77,'2026-11-28 23:44:15'),(55,0,'商家经营概览','dashboard:view','BUTTON',78,'2026-11-28 23:44:15'),(56,0,'助农短视频','video:list','MENU',80,'2026-11-28 23:44:15'),(57,0,'短视频详情','video:detail','BUTTON',81,'2026-11-28 23:44:15'),(58,0,'短视频点赞','video:like','BUTTON',82,'2026-11-28 23:44:15'),(59,0,'短视频评论列表','video:commentList','BUTTON',83,'2026-11-28 23:44:15'),(60,0,'发表短视频评论','video:comment','BUTTON',84,'2026-11-28 23:44:15'),(61,0,'删除短视频评论','video:commentDelete','BUTTON',85,'2026-11-28 23:44:15'),(62,0,'关注商家','video:follow','BUTTON',86,'2026-11-28 23:44:15'),(63,0,'商家主页','video:farmerProfile','BUTTON',87,'2026-11-28 23:44:15'),(64,0,'发布短视频','video:add','BUTTON',88,'2026-11-28 23:44:15'),(65,0,'我的短视频','video:my','BUTTON',89,'2026-11-28 23:44:15'),(66,0,'删除短视频','video:delete','BUTTON',90,'2026-11-28 23:44:15'),(67,0,'短视频列表(管理端)','video:adminList','BUTTON',91,'2026-11-28 23:44:15'),(68,0,'短视频审核','video:audit','BUTTON',92,'2026-11-28 23:44:15'),(69,0,'农产品列表','product:list','BUTTON',50,'2026-11-28 23:48:17'),(70,0,'农产品详情','product:detail','BUTTON',51,'2026-11-28 23:48:17'),(71,0,'我的农产品','product:my','BUTTON',61,'2026-11-28 23:48:17'),(72,0,'上架农产品','product:add','BUTTON',52,'2026-11-28 23:48:17'),(73,0,'编辑农产品','product:edit','BUTTON',53,'2026-11-28 23:48:17'),(74,0,'商品上下架','product:shelf','BUTTON',54,'2026-11-28 23:48:17'),(75,0,'删除农产品','product:delete','BUTTON',55,'2026-11-28 23:48:17'),(76,0,'购物车列表','cart:list','BUTTON',56,'2026-11-28 23:48:17'),(77,0,'加入购物车','cart:add','BUTTON',57,'2026-11-28 23:48:17'),(78,0,'修改购物车','cart:edit','BUTTON',58,'2026-11-28 23:48:17'),(79,0,'删除购物车行','cart:delete','BUTTON',59,'2026-11-28 23:48:17'),(80,0,'清空购物车','cart:clear','BUTTON',60,'2026-11-28 23:48:17'),(81,0,'农家乐列表','project:list','BUTTON',30,'2026-11-28 23:48:17'),(82,0,'农家乐详情','project:detail','BUTTON',31,'2026-11-28 23:48:17'),(83,0,'申请农家乐项目','project:add','BUTTON',32,'2026-11-28 23:48:17'),(84,0,'我的农家乐项目','project:my','BUTTON',33,'2026-11-28 23:48:17'),(85,0,'编辑农家乐项目','project:edit','BUTTON',34,'2026-11-28 23:48:17'),(86,0,'项目上下架','project:shelf','BUTTON',35,'2026-11-28 23:48:17'),(87,0,'农家乐项目审核','project:audit','BUTTON',36,'2026-11-28 23:48:17'),(88,0,'通知公告','sys:notice:menu','MENU',93,'2026-11-28 23:48:17'),(89,0,'新增通知','sys:notice:create','BUTTON',94,'2026-11-28 23:48:17'),(90,0,'编辑通知','sys:notice:update','BUTTON',95,'2026-11-28 23:48:17'),(91,0,'删除通知','sys:notice:delete','BUTTON',96,'2026-11-28 23:48:17'),(92,0,'发布通知','sys:notice:publish','BUTTON',97,'2026-11-28 23:48:17'),(93,0,'撤回通知','sys:notice:revoke','BUTTON',98,'2026-11-28 23:48:17'),(94,0,'政策管理','cms:policy:menu','MENU',99,'2026-11-28 23:48:17'),(95,0,'新增政策','cms:policy:create','BUTTON',100,'2026-11-28 23:48:17'),(96,0,'编辑政策','cms:policy:update','BUTTON',101,'2026-11-28 23:48:17'),(97,0,'删除政策','cms:policy:delete','BUTTON',102,'2026-11-28 23:48:17'),(98,0,'政策发布下架','cms:policy:publish','BUTTON',103,'2026-11-28 23:48:17'),(99,0,'资讯管理','cms:news:menu','MENU',104,'2026-11-28 23:48:17'),(100,0,'新增资讯','cms:news:create','BUTTON',105,'2026-11-28 23:48:17'),(101,0,'编辑资讯','cms:news:update','BUTTON',106,'2026-11-28 23:48:17'),(102,0,'删除资讯','cms:news:delete','BUTTON',107,'2026-11-28 23:48:17'),(103,0,'资讯发布下架','cms:news:publish','BUTTON',108,'2026-11-28 23:48:17'),(104,0,'提交售后申请','afterSale:add','BUTTON',109,'2026-11-28 23:48:17'),(105,0,'我的售后列表','afterSale:list','BUTTON',110,'2026-11-28 23:48:17'),(106,0,'售后详情','afterSale:detail','BUTTON',111,'2026-11-28 23:48:18'),(107,0,'商家售后列表','afterSale:farmerList','BUTTON',112,'2026-11-28 23:48:18'),(108,0,'处理售后','afterSale:handle','BUTTON',113,'2026-11-28 23:48:18'),(109,0,'平台介入处理','afterSale:intervene','BUTTON',114,'2026-11-28 23:48:18');
/*!40000 ALTER TABLE `sys_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '角色名称',
  `code` varchar(64) NOT NULL COMMENT '角色编码',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'管理员','ADMIN',1,'2026-11-28 23:44:15'),(2,'买家','BUYER',3,'2026-11-28 23:44:15'),(3,'农户商家','FARMER',2,'2026-11-28 23:44:15');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_permission`
--

DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_permission`
--

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
INSERT INTO `sys_role_permission` VALUES (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),(1,11),(1,12),(1,13),(1,14),(1,15),(1,16),(1,17),(1,18),(1,19),(1,20),(1,21),(1,22),(1,23),(1,24),(1,25),(1,26),(1,27),(1,28),(1,29),(1,30),(1,31),(1,32),(1,33),(1,34),(1,35),(1,36),(1,37),(1,38),(1,39),(1,40),(1,41),(1,42),(1,43),(1,44),(1,45),(1,46),(1,47),(1,48),(1,49),(1,50),(1,51),(1,52),(1,53),(1,54),(1,55),(1,56),(1,57),(1,58),(1,59),(1,60),(1,61),(1,62),(1,63),(1,64),(1,65),(1,66),(1,67),(1,68),(1,69),(1,70),(1,71),(1,72),(1,73),(1,74),(1,75),(1,76),(1,77),(1,78),(1,79),(1,80),(1,81),(1,82),(1,83),(1,84),(1,85),(1,86),(1,87),(1,88),(1,89),(1,90),(1,91),(1,92),(1,93),(1,94),(1,95),(1,96),(1,97),(1,98),(1,99),(1,100),(1,101),(1,102),(1,103),(1,104),(1,105),(1,106),(1,107),(1,108),(1,109),(2,13),(2,15),(2,17),(2,19),(2,20),(2,24),(2,25),(2,26),(2,27),(2,28),(2,39),(2,40),(2,41),(2,42),(2,43),(2,56),(2,57),(2,58),(2,59),(2,60),(2,61),(2,62),(2,63),(2,69),(2,70),(2,76),(2,77),(2,78),(2,79),(2,80),(2,81),(2,82),(2,104),(2,105),(2,106),(3,10),(3,11),(3,12),(3,13),(3,14),(3,15),(3,17),(3,18),(3,20),(3,24),(3,25),(3,26),(3,27),(3,28),(3,32),(3,33),(3,40),(3,44),(3,45),(3,47),(3,48),(3,49),(3,50),(3,51),(3,52),(3,53),(3,55),(3,56),(3,57),(3,58),(3,59),(3,60),(3,61),(3,62),(3,63),(3,64),(3,65),(3,66),(3,69),(3,70),(3,71),(3,72),(3,73),(3,74),(3,75),(3,81),(3,82),(3,83),(3,84),(3,85),(3,86),(3,106),(3,107),(3,108);
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码（BCrypt）',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(512) DEFAULT NULL COMMENT '头像地址',
  `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `gender` tinyint DEFAULT '0' COMMENT '性别：0未知 1男 2女',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `dept_id` bigint DEFAULT NULL COMMENT '商户ID',
  `role_id` bigint DEFAULT NULL COMMENT '角色ID',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除 1已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'buyer01','$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W','小娟','/img/avatar/u1.png','13905041001','chenxj@qq.com',2,1,1,2,0,'2026-08-15 10:00:00','2026-11-28 23:44:15'),(2,'buyer02','$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W','阿强','/img/avatar/u2.png','13905041002','linzq@qq.com',1,1,1,2,0,'2026-08-20 14:20:00','2026-11-28 23:44:15'),(3,'buyer03','$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W','丽丽','/img/avatar/u3.png','13905041003','huangli@163.com',2,1,1,2,0,'2026-09-01 09:30:00','2026-11-28 23:44:15'),(4,'farmer01','$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W','吴叔山货铺','/img/avatar/u4.png','13905042001','wugp@qq.com',1,1,2,3,0,'2026-09-10 09:05:00','2026-11-28 23:44:15'),(5,'farmer02','$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W','秀兰农场','/img/avatar/u5.png','13905042002','zhengxl@qq.com',2,1,3,3,0,'2026-09-13 14:35:00','2026-11-28 23:44:15'),(6,'farmer03','$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W','蔡伯土产','/img/avatar/u6.png','13905042003',NULL,1,1,4,3,0,'2026-10-06 16:25:00','2026-11-28 23:44:15'),(7,'admin01','$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W','系统管理员','/img/avatar/a1.png','13905043001','admin@zhongshan.gov.cn',1,1,1,1,0,'2026-08-01 09:00:00','2026-11-28 23:44:15'),(8,'admin02','$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W','内容审核员','/img/avatar/a2.png','13905043002','audit@zhongshan.gov.cn',2,1,1,1,0,'2026-08-01 09:05:00','2026-11-28 23:44:15'),(9,'buyer04','$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W','小雨','/img/avatar/u9.png','13905041009',NULL,2,1,1,2,0,'2026-11-18 10:00:00','2026-11-28 23:44:15'),(10,'buyer05','$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W','小伟','/img/avatar/u10.png','13905041010',NULL,1,1,1,2,0,'2026-11-20 11:00:00','2026-11-28 23:44:15'),(11,'admin','$2a$10$.6Km.luLvoLmumLxnlnLkeGzx.4LpayD1eDZu3XQKZvrlrtfYVm3W','管理员',NULL,'13905043000','admin@nougyou.local',1,1,1,1,0,'2026-08-01 09:00:00','2026-11-28 23:44:15');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `v_entrust_sale_stat`
--

DROP TABLE IF EXISTS `v_entrust_sale_stat`;
/*!50001 DROP VIEW IF EXISTS `v_entrust_sale_stat`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_entrust_sale_stat` AS SELECT 
 1 AS `entrust_id`,
 1 AS `entrust_no`,
 1 AS `entrust_user_id`,
 1 AS `entrust_user_name`,
 1 AS `agent_user_id`,
 1 AS `agent_user_name`,
 1 AS `product_id`,
 1 AS `product_name`,
 1 AS `stock`,
 1 AS `total_sale_count`,
 1 AS `split_ratio`,
 1 AS `entrust_status`,
 1 AS `sale_qty`,
 1 AS `sale_amount`,
 1 AS `refund_qty`,
 1 AS `refund_amount`,
 1 AS `entrust_income`,
 1 AS `agent_income`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `video`
--

DROP TABLE IF EXISTS `video`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '视频ID',
  `farmer_id` bigint NOT NULL COMMENT '发布农户商家ID',
  `title` varchar(150) NOT NULL COMMENT '视频标题',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图',
  `video_url` varchar(255) NOT NULL COMMENT '视频地址',
  `duration` int NOT NULL DEFAULT '0' COMMENT '时长（秒）',
  `description` varchar(500) DEFAULT NULL COMMENT '简介',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态 0待审核 1通过 2驳回（FR-021/035）',
  `audit_user_id` bigint DEFAULT NULL COMMENT '审核管理员ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核意见',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `comment_count` int NOT NULL DEFAULT '0' COMMENT '评论数',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '播放量',
  `publish_time` datetime DEFAULT NULL COMMENT '审核通过后的展示时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1正常 0已下架',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_farmer` (`farmer_id`),
  KEY `idx_audit` (`audit_status`),
  KEY `idx_publish` (`status`,`publish_time`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短视频表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `video`
--

LOCK TABLES `video` WRITE;
/*!40000 ALTER TABLE `video` DISABLE KEYS */;
INSERT INTO `video` VALUES (1,4,'兴化米粉手工制作全过程','/api/v1/media/cover/v1.svg','/api/v1/media/video/v1.mp4',10,'从浸米、磨浆到上锅蒸、切丝、晒干，一双手做了三十年。',1,8,'2026-09-25 10:00:00','内容真实、无违规，通过',128,3,2150,'2026-09-25 10:05:00',1,'2026-09-24 20:10:00','2026-11-28 23:44:15',0),(2,5,'九鲤湖边的农家乐一日','/api/v1/media/cover/v2.svg','/api/v1/media/video/v2.mp4',10,'带你看看我们家的农家菜、客房和后山果园。',1,8,'2026-10-08 09:20:00','通过',96,2,1780,'2026-10-08 09:30:00',1,'2026-10-07 18:40:00','2026-11-28 23:44:15',0),(3,6,'蔡伯的地瓜干为什么这么甜','/api/v1/media/cover/v3.svg','/api/v1/media/video/v3.mp4',5,'三蒸三晒，不加一粒糖，甜是地瓜自己的甜。',1,8,'2026-10-22 15:40:00','审核通过；建议补充字幕便于年长用户观看',64,2,905,'2026-10-22 15:45:00',1,'2026-10-21 16:30:00','2026-11-28 23:44:15',0),(4,5,'山茶油开榨啦','/api/v1/media/cover/v4.svg','/api/v1/media/video/v4.mp4',6,'今年第一批茶籽下榨，记录榨油全过程。',0,NULL,NULL,NULL,0,0,0,NULL,1,'2026-11-21 10:20:00','2026-11-28 23:44:15',0),(5,4,'米粉里吃出异物，别买别家','/api/v1/media/cover/v6.svg','/api/v1/media/video/v5.mp4',10,'（标题含贬损同行表述）',2,8,'2026-11-10 11:00:00','标题与内容含贬损竞争对手表述，驳回修改后重新提交',0,0,0,NULL,0,'2026-11-09 21:00:00','2026-11-28 23:44:15',0),(6,6,'村里的老屋和晒谷场','/api/v1/media/cover/v5.svg','/api/v1/media/video/v5.mp4',10,'太阳落山前，老屋前的晒谷场最热闹。',1,8,'2026-11-23 09:00:00','生活记录类内容，通过',12,1,270,'2026-11-23 09:10:00',1,'2026-11-22 18:00:00','2026-11-29 00:00:21',0);
/*!40000 ALTER TABLE `video` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `video_comment`
--

DROP TABLE IF EXISTS `video_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `video_id` bigint NOT NULL COMMENT '视频ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父评论ID，0为一级评论',
  `content` varchar(500) NOT NULL COMMENT '评论内容',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '评论点赞数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1正常 0已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_video` (`video_id`,`status`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短视频评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `video_comment`
--

LOCK TABLES `video_comment` WRITE;
/*!40000 ALTER TABLE `video_comment` DISABLE KEYS */;
INSERT INTO `video_comment` VALUES (1,1,1,0,'看着就香，已经下单两袋了。',5,1,'2026-09-26 08:12:00'),(2,1,4,1,'谢谢支持，明天一早就发。',2,1,'2026-09-26 08:20:00'),(3,1,3,0,'这种手工米粉煮之前要泡多久？',3,1,'2026-09-27 19:02:00'),(4,2,3,0,'国庆去过，卤面很好吃！',8,1,'2026-10-10 20:45:00'),(5,2,5,4,'谢谢！下次来提前说，给你留靠窗的位子。',1,1,'2026-10-10 21:10:00'),(6,3,2,0,'老人家手艺真好，已下单。',4,1,'2026-10-23 10:05:00'),(7,3,6,6,'谢谢，欢迎来西林村看看。',1,1,'2026-10-23 11:00:00'),(8,6,1,0,'老屋真有味道，周末想带孩子去看看。',2,1,'2026-11-23 09:30:00');
/*!40000 ALTER TABLE `video_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `video_follow`
--

DROP TABLE IF EXISTS `video_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '关注人（买家）用户ID',
  `farmer_id` bigint NOT NULL COMMENT '被关注农户商家ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_farmer` (`user_id`,`farmer_id`),
  KEY `idx_farmer` (`farmer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关注农户商家表（FR-012）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `video_follow`
--

LOCK TABLES `video_follow` WRITE;
/*!40000 ALTER TABLE `video_follow` DISABLE KEYS */;
INSERT INTO `video_follow` VALUES (1,1,4,'2026-09-26 08:11:00'),(2,1,5,'2026-10-09 09:16:00'),(3,3,5,'2026-10-10 20:41:00'),(4,2,4,'2026-10-23 10:06:00'),(5,9,6,'2026-10-24 11:21:00'),(6,1,6,'2026-11-23 09:21:00');
/*!40000 ALTER TABLE `video_follow` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `video_like`
--

DROP TABLE IF EXISTS `video_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `video_id` bigint NOT NULL COMMENT '视频ID',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_user` (`video_id`,`user_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='短视频点赞表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `video_like`
--

LOCK TABLES `video_like` WRITE;
/*!40000 ALTER TABLE `video_like` DISABLE KEYS */;
INSERT INTO `video_like` VALUES (1,1,1,'2026-09-26 08:10:00'),(2,1,2,'2026-09-26 12:30:00'),(3,1,3,'2026-09-27 19:00:00'),(4,2,1,'2026-10-09 09:15:00'),(5,2,3,'2026-10-10 20:40:00'),(6,3,2,'2026-10-23 10:00:00'),(7,3,9,'2026-10-24 11:20:00'),(8,6,1,'2026-11-23 09:20:00'),(9,6,3,'2026-11-23 10:05:00');
/*!40000 ALTER TABLE `video_like` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'nougyou'
--

--
-- Final view structure for view `v_entrust_sale_stat`
--

/*!50001 DROP VIEW IF EXISTS `v_entrust_sale_stat`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_entrust_sale_stat` AS select `e`.`id` AS `entrust_id`,`e`.`entrust_no` AS `entrust_no`,`e`.`entrust_user_id` AS `entrust_user_id`,`eu`.`nickname` AS `entrust_user_name`,`e`.`agent_user_id` AS `agent_user_id`,`au`.`nickname` AS `agent_user_name`,`e`.`product_id` AS `product_id`,`p`.`name` AS `product_name`,`p`.`stock` AS `stock`,`p`.`sale_count` AS `total_sale_count`,`e`.`split_ratio` AS `split_ratio`,`e`.`status` AS `entrust_status`,ifnull(`s`.`sale_qty`,0) AS `sale_qty`,ifnull(`s`.`sale_amount`,0) AS `sale_amount`,ifnull(`r`.`refund_qty`,0) AS `refund_qty`,ifnull(`r`.`refund_amount`,0) AS `refund_amount`,round((((ifnull(`s`.`sale_amount`,0) - ifnull(`r`.`refund_amount`,0)) * (100 - `e`.`split_ratio`)) / 100),2) AS `entrust_income`,round((((ifnull(`s`.`sale_amount`,0) - ifnull(`r`.`refund_amount`,0)) * `e`.`split_ratio`) / 100),2) AS `agent_income` from (((((`entrust_sale` `e` join `product` `p` on((`p`.`id` = `e`.`product_id`))) left join `sys_user` `eu` on((`eu`.`id` = `e`.`entrust_user_id`))) left join `sys_user` `au` on((`au`.`id` = `e`.`agent_user_id`))) left join (select `oi`.`entrust_id` AS `entrust_id`,sum(`oi`.`quantity`) AS `sale_qty`,sum(`oi`.`subtotal`) AS `sale_amount` from (`order_item` `oi` join `orders` `o` on(((`o`.`id` = `oi`.`order_id`) and (`o`.`order_status` <> 4) and (`o`.`pay_status` = 1)))) where (`oi`.`entrust_id` is not null) group by `oi`.`entrust_id`) `s` on((`s`.`entrust_id` = `e`.`id`))) left join (select `oi`.`entrust_id` AS `entrust_id`,sum(`oi`.`quantity`) AS `refund_qty`,sum(`a`.`refund_amount`) AS `refund_amount` from (`after_sale` `a` join `order_item` `oi` on((`oi`.`id` = `a`.`order_item_id`))) where ((`a`.`status` = 2) and (`oi`.`entrust_id` is not null)) group by `oi`.`entrust_id`) `r` on((`r`.`entrust_id` = `e`.`id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-29  0:12:02
