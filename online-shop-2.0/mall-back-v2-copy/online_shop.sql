/*
 * 在线商城 v2.0 数据库脚本（买家端接口所需表结构 + 演示数据）
 * MySQL 8.x
 *
 * 使用方式：
 *   1. 在 MySQL 中执行本脚本；
 *   2. 按需修改 application.yml 中的数据库账号 / 密码。
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `online_shop`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `online_shop`;

DROP TABLE IF EXISTS `intent`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `buyer_account`;
DROP TABLE IF EXISTS `seller`;

-- ----------------------------
-- 卖家（买家端只做商品展示，保留该表与演示账号）
-- ----------------------------
CREATE TABLE `seller` (
    `username`      varchar(64)  NOT NULL COMMENT '唯一卖家账号',
    `password_hash` varchar(255) NOT NULL COMMENT 'BCrypt 加密密码',
    `token`         varchar(64)  NULL COMMENT '当前登录令牌',
    PRIMARY KEY (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO `seller` (`username`, `password_hash`) VALUES ('seller01',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi');

-- ----------------------------
-- 二级商品分类
-- ----------------------------
CREATE TABLE `category` (
    `id`            int         NOT NULL AUTO_INCREMENT,
    `parent_id`     int         NOT NULL DEFAULT 0 COMMENT '父分类 ID，一级分类为 0',
    `category_name` varchar(128) NOT NULL COMMENT '分类名称',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_parent_name` (`parent_id`, `category_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO `category` (`id`, `parent_id`, `category_name`) VALUES
    (1, 0, '手机数码'),
    (2, 0, '电脑办公'),
    (3, 0, '家用电器'),
    (5, 1, '手机'),
    (6, 2, '笔记本电脑'),
    (7, 1, '平板电脑'),
    (8, 1, '耳机'),
    (9, 3, '冰箱');

-- ----------------------------
-- 买家账号
-- ----------------------------
CREATE TABLE `buyer_account` (
    `id`                     int          NOT NULL AUTO_INCREMENT,
    `username`               varchar(64)  NOT NULL COMMENT '用户名，唯一',
    `password_hash`          varchar(255) NOT NULL COMMENT 'BCrypt 加密密码，任何接口均不返回',
    `phone`                  varchar(64)  NOT NULL COMMENT '手机号，不唯一',
    `default_trade_location` varchar(255) NULL COMMENT '默认交易地点，可为空',
    `token`                  varchar(64)  NULL COMMENT '当前登录令牌，48 位随机串',
    `created_at`             datetime     NOT NULL COMMENT '注册时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_token` (`token`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 演示买家账号：buyer01 / 12345678
INSERT INTO `buyer_account`
    (`id`, `username`, `password_hash`, `phone`, `default_trade_location`, `token`, `created_at`)
VALUES
    (1, 'buyer01', '$2a$10$B7ouTG/9ejNcGFdagr5qwuAgZsPMrsAlqSDhEk7CsigQCJTaBScjO',
     '13800138000', '图书馆正门', NULL, '2026-10-10 20:15:00');

-- ----------------------------
-- 商品
-- ----------------------------
CREATE TABLE `product` (
    `id`           int           NOT NULL AUTO_INCREMENT,
    `name`         varchar(255)  NOT NULL COMMENT '商品名称',
    `description`  text          NULL COMMENT '富文本描述（HTML 片段）',
    `image_urls`   text          NULL COMMENT '图片地址 JSON 数组，第一条为主图，至多 9 条',
    `price`        decimal(10,2) NOT NULL COMMENT '价格，保留 2 位小数',
    `status`       varchar(20)   NOT NULL COMMENT 'ONLINE / FROZEN / OFFLINE',
    `stock`        int           NOT NULL DEFAULT 0 COMMENT '库存，非负整数',
    `published_at` datetime      NOT NULL COMMENT '上架时间',
    `closed_at`    datetime      NULL COMMENT '下架时间',
    `seller_id`    varchar(64)   NOT NULL COMMENT '卖家用户名',
    `category_id`  int           NOT NULL COMMENT '所属二级分类 ID',
    PRIMARY KEY (`id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status_published` (`status`, `published_at`),
    CONSTRAINT `fk_product_seller` FOREIGN KEY (`seller_id`) REFERENCES `seller` (`username`),
    CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO `product`
    (`id`, `name`, `description`, `image_urls`, `price`, `status`, `stock`,
     `published_at`, `closed_at`, `seller_id`, `category_id`)
VALUES
    (1, 'iPhone 15',
     '<p>全新未拆封，支持全国联保。</p><ul><li>256G</li><li>原色钛金属</li></ul>',
     '["https://cdn.example.com/iphone15-1.jpg","https://cdn.example.com/iphone15-2.jpg"]',
     5999.00, 'ONLINE', 10, '2026-10-01 10:00:00', NULL, 'seller01', 5),
    (2, '小米14 Ultra',
     '<p>徕卡影像旗舰，骁龙 8 Gen3，16+512G。</p>',
     '["https://cdn.example.com/mi14-1.jpg"]',
     6499.00, 'ONLINE', 5, '2026-10-02 14:30:00', NULL, 'seller01', 5),
    (3, 'MacBook Air M3',
     '<p>13 英寸，M3 芯片，16G+512G，午夜色。</p>',
     '["https://cdn.example.com/mba-1.jpg","https://cdn.example.com/mba-2.jpg"]',
     8999.00, 'ONLINE', 3, '2026-10-03 09:00:00', NULL, 'seller01', 6),
    (4, 'iPad Air 5',
     '<p>M1 芯片，64G WiFi 版，带笔，轻微使用痕迹。</p>',
     '["https://cdn.example.com/ipad-1.jpg"]',
     3899.00, 'OFFLINE', 0, '2026-09-01 10:00:00', '2026-09-30 22:00:00', 'seller01', 7),
    (5, '华为 Mate 60 Pro',
     '<p>麒麟 9000S，卫星通话，已激活一个月。</p>',
     '["https://cdn.example.com/mate60-1.jpg"]',
     5999.00, 'FROZEN', 2, '2026-09-20 09:00:00', NULL, 'seller01', 5),
    (6, 'AirPods Pro 2',
     '<p>主动降噪蓝牙耳机，在保。</p>',
     NULL,
     1599.00, 'ONLINE', 20, '2026-10-05 20:00:00', NULL, 'seller01', 8);

-- ----------------------------
-- 购买意向
-- ----------------------------
CREATE TABLE `intent` (
    `id`                int          NOT NULL AUTO_INCREMENT,
    `buyer_name`        varchar(128) NULL COMMENT '存量匿名意向保留原值；新意向为 NULL',
    `phone`             varchar(64)  NULL COMMENT '存量匿名意向保留原值；新意向为 NULL',
    `status`            varchar(20)  NOT NULL COMMENT 'QUEUED / TRADING / SUCCESS / CANCELLED / FAILED',
    `created_at`        datetime     NOT NULL COMMENT '创建时间，排队先后基准',
    `updated_at`        datetime     NOT NULL COMMENT '更新时间',
    `result_at`         datetime     NULL COMMENT '交易结果时间',
    `product_id`        int          NOT NULL COMMENT '关联商品 ID',
    `product_name`      varchar(255) NOT NULL COMMENT '商品名快照',
    `buyer_account_id`  int          NULL COMMENT '关联买家账号；存量匿名意向为 NULL',
    PRIMARY KEY (`id`),
    KEY `idx_product_status_time` (`product_id`, `status`, `created_at`),
    KEY `idx_buyer_time` (`buyer_account_id`, `created_at`),
    CONSTRAINT `fk_intent_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
    CONSTRAINT `fk_intent_buyer` FOREIGN KEY (`buyer_account_id`) REFERENCES `buyer_account` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 演示意向：2 条已出结果（历史记录可见）、1 条排队中、1 条存量匿名归档
INSERT INTO `intent`
    (`id`, `buyer_name`, `phone`, `status`, `created_at`, `updated_at`, `result_at`,
     `product_id`, `product_name`, `buyer_account_id`)
VALUES
    (1, NULL, NULL, 'SUCCESS', '2026-10-08 09:30:00', '2026-10-08 15:20:00', '2026-10-08 15:20:00',
     1, 'iPhone 15', 1),
    (2, NULL, NULL, 'FAILED', '2026-10-09 10:00:00', '2026-10-09 11:00:00', '2026-10-09 11:00:00',
     2, '小米14 Ultra', 1),
    (3, NULL, NULL, 'QUEUED', '2026-10-23 09:00:00', '2026-10-23 09:00:00', NULL,
     3, 'MacBook Air M3', 1),
    (4, '张三', '13900139000', 'CANCELLED', '2026-09-15 08:00:00', '2026-09-15 09:00:00', NULL,
     4, 'iPad Air 5', NULL);

SET FOREIGN_KEY_CHECKS = 1;
