/*
 * 在线商城 v4.0 数据库脚本（挑战需求 C · 买家端接口所需表结构 + 演示数据）
 * MySQL 8.x
 *
 * v4.0 变更：
 *   - 订单状态机扩展为 7 个推进态 + 已取消（CREATED / PENDING_PAYMENT / PAID / CONFIRMED / STOCKED / SHIPPING / COMPLETED / CANCELLED）
 *   - 库存升级为「账面库存 + 预占量」两层：预占量不落库，由「已支付且未成交」订单明细实时计算
 *   - 订单扩展支付、收货地址快照、物流字段
 *   - 新增：地址簿 address、支付记录 payment_record、退款记录 refund_record、
 *          售后单 after_sales_order、买家评价 review、物流服务商 logistics_provider、
 *          物流跟踪 logistics_tracking、模拟渠道记录 mock_channel_txn
 *
 * 使用方式：
 *   1. 在 MySQL 中执行本脚本（会重建 online_shop_v4 库）；
 *   2. 按需修改 application.yml 中的数据库账号 / 密码。
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `online_shop_v4`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `online_shop_v4`;

DROP TABLE IF EXISTS `mock_channel_txn`;
DROP TABLE IF EXISTS `refund_record`;
DROP TABLE IF EXISTS `payment_record`;
DROP TABLE IF EXISTS `review`;
DROP TABLE IF EXISTS `after_sales_order`;
DROP TABLE IF EXISTS `logistics_tracking`;
DROP TABLE IF EXISTS `logistics_provider`;
DROP TABLE IF EXISTS `address`;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `cart_item`;
DROP TABLE IF EXISTS `favorite`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `buyer_account`;
DROP TABLE IF EXISTS `seller`;

-- ----------------------------
-- 卖家
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
    `id`            int          NOT NULL AUTO_INCREMENT,
    `parent_id`     int          NOT NULL DEFAULT 0 COMMENT '父分类 ID，一级分类为 0',
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
    `default_trade_location` varchar(255) NULL COMMENT '默认交易地点，可为空（不入订单）',
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
-- 商品（状态仍为 ONLINE / OFFLINE 两态）
-- ----------------------------
CREATE TABLE `product` (
    `id`             int           NOT NULL AUTO_INCREMENT,
    `name`           varchar(255)  NOT NULL COMMENT '商品名称',
    `description`    text          NULL COMMENT '富文本描述（HTML 片段）',
    `image_urls`     text          NULL COMMENT '图片地址 JSON 数组，第一条为主图，至多 9 条',
    `price`          decimal(10,2) NOT NULL COMMENT '发布价，保留 2 位小数',
    `sale_price`     decimal(10,2) NULL COMMENT '限时活动价，未设置时为 NULL',
    `sale_start_at`  datetime      NULL COMMENT '活动开始时间',
    `sale_end_at`    datetime      NULL COMMENT '活动结束时间',
    `status`         varchar(20)   NOT NULL COMMENT 'ONLINE 在售 / OFFLINE 已下架',
    `off_reason`     varchar(20)   NULL COMMENT '下架原因：MANUAL 手动 / STOCK_OUT 库存归零自动下架',
    `stock`          int           NOT NULL DEFAULT 0 COMMENT '账面库存，非负整数（唯一库存数）',
    `published_at`   datetime      NOT NULL COMMENT '发布时间',
    `closed_at`      datetime      NULL COMMENT '下架时间',
    `seller_id`      varchar(64)   NOT NULL COMMENT '卖家用户名',
    `category_id`    int           NOT NULL COMMENT '所属分类 ID',
    PRIMARY KEY (`id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status_published` (`status`, `published_at`),
    CONSTRAINT `fk_product_seller` FOREIGN KEY (`seller_id`) REFERENCES `seller` (`username`),
    CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO `product`
    (`id`, `name`, `description`, `image_urls`, `price`, `sale_price`, `sale_start_at`, `sale_end_at`,
     `status`, `off_reason`, `stock`, `published_at`, `closed_at`, `seller_id`, `category_id`)
VALUES
    (1, 'iPhone 15',
     '<p>全新未拆封，支持全国联保。</p><ul><li>256G</li><li>原色钛金属</li></ul>',
     '["https://cdn.example.com/iphone15-1.jpg","https://cdn.example.com/iphone15-2.jpg"]',
     5999.00, NULL, NULL, NULL,
     'ONLINE', NULL, 10, '2026-10-01 10:00:00', NULL, 'seller01', 5),
    (2, '小米14 Ultra',
     '<p>徕卡影像旗舰，骁龙 8 Gen3，16+512G。</p>',
     '["https://cdn.example.com/mi14-1.jpg"]',
     6499.00, NULL, NULL, NULL,
     'ONLINE', NULL, 5, '2026-10-02 14:30:00', NULL, 'seller01', 5),
    (3, 'MacBook Air M3',
     '<p>13 英寸，M3 芯片，16G+512G，午夜色。</p>',
     '["https://cdn.example.com/mba-1.jpg","https://cdn.example.com/mba-2.jpg"]',
     8999.00, NULL, NULL, NULL,
     'ONLINE', NULL, 3, '2026-10-03 09:00:00', NULL, 'seller01', 6),
    (4, 'iPad Air 5',
     '<p>M1 芯片，64G WiFi 版，带笔，轻微使用痕迹。</p>',
     '["https://cdn.example.com/ipad-1.jpg"]',
     3899.00, NULL, NULL, NULL,
     'OFFLINE', 'STOCK_OUT', 0, '2026-09-01 10:00:00', '2026-09-30 22:00:00', 'seller01', 7),
    (5, '二手山地自行车',
     '<p>九成新，变速灵敏。</p>',
     '["/uploads/a1b2c3.png","/uploads/d4e5f6.png"]',
     199.00, 159.00, '2026-11-01 00:00:00', '2026-11-30 23:59:59',
     'ONLINE', NULL, 5, '2026-11-01 10:00:00', NULL, 'seller01', 5),
    (6, 'AirPods Pro 2',
     '<p>主动降噪蓝牙耳机，在保。</p>',
     NULL,
     1599.00, NULL, NULL, NULL,
     'ONLINE', NULL, 20, '2026-10-05 20:00:00', NULL, 'seller01', 8);

-- ----------------------------
-- 购物车条目
-- ----------------------------
CREATE TABLE `cart_item` (
    `id`                int      NOT NULL AUTO_INCREMENT,
    `buyer_account_id`  int      NOT NULL COMMENT '买家账号 ID',
    `product_id`        int      NOT NULL COMMENT '商品 ID',
    `quantity`          int      NOT NULL DEFAULT 1 COMMENT '数量（1~999）',
    `created_at`        datetime NOT NULL COMMENT '加入时间',
    `updated_at`        datetime NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_buyer_product` (`buyer_account_id`, `product_id`),
    KEY `idx_buyer_id` (`buyer_account_id`),
    CONSTRAINT `fk_cart_buyer` FOREIGN KEY (`buyer_account_id`) REFERENCES `buyer_account` (`id`),
    CONSTRAINT `fk_cart_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ----------------------------
-- 收藏
-- ----------------------------
CREATE TABLE `favorite` (
    `id`                int      NOT NULL AUTO_INCREMENT,
    `buyer_account_id`  int      NOT NULL COMMENT '买家账号 ID',
    `product_id`        int      NOT NULL COMMENT '商品 ID',
    `created_at`        datetime NOT NULL COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_buyer_product` (`buyer_account_id`, `product_id`),
    KEY `idx_buyer_id` (`buyer_account_id`),
    CONSTRAINT `fk_favorite_buyer` FOREIGN KEY (`buyer_account_id`) REFERENCES `buyer_account` (`id`),
    CONSTRAINT `fk_favorite_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ----------------------------
-- 收货地址簿（每账号至多 1 个默认地址，数量上限 20）
-- ----------------------------
CREATE TABLE `address` (
    `id`                int          NOT NULL AUTO_INCREMENT,
    `buyer_account_id`  int          NOT NULL COMMENT '买家账号 ID',
    `name`              varchar(64)  NOT NULL COMMENT '收货人姓名',
    `phone`             varchar(32)  NOT NULL COMMENT '联系电话',
    `province`          varchar(64)  NOT NULL COMMENT '省',
    `city`              varchar(64)  NOT NULL COMMENT '市',
    `district`          varchar(64)  NOT NULL COMMENT '区',
    `detail`            varchar(255) NOT NULL COMMENT '详细地址',
    `is_default`        tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否默认地址',
    `created_at`        datetime     NOT NULL COMMENT '创建时间',
    `updated_at`        datetime     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_addr_buyer` (`buyer_account_id`),
    CONSTRAINT `fk_address_buyer` FOREIGN KEY (`buyer_account_id`) REFERENCES `buyer_account` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO `address`
    (`id`, `buyer_account_id`, `name`, `phone`, `province`, `city`, `district`, `detail`, `is_default`, `created_at`, `updated_at`)
VALUES
    (1, 1, '张三', '13800138000', '广东', '深圳', '南山区', '科技园路 1 号', 1, '2026-10-11 09:00:00', '2026-10-11 09:00:00'),
    (2, 1, '李四', '13900139000', '北京', '北京', '海淀区', '中关村大街 5 号', 0, '2026-10-12 10:00:00', '2026-10-12 10:00:00');

-- ----------------------------
-- 订单（v4.0：支付 / 地址快照 / 物流字段；表名 orders 避开 SQL 保留字）
-- ----------------------------
CREATE TABLE `orders` (
    `id`                  int           NOT NULL AUTO_INCREMENT,
    `order_no`            varchar(32)   NOT NULL COMMENT '订单号（日期-4位流水）',
    `status`              varchar(20)   NOT NULL COMMENT 'CREATED/PENDING_PAYMENT/PAID/CONFIRMED/STOCKED/SHIPPING/COMPLETED/CANCELLED',
    `total_amount`        decimal(10,2) NOT NULL COMMENT '总金额 = Σ(明细快照价×数量)，无运费',
    `buyer_account_id`    int           NOT NULL COMMENT '买家账号 ID',
    `payment_channel`     varchar(20)   NULL COMMENT 'BANK_CARD/CREDIT_CARD/ALIPAY/WECHAT_PAY',
    `paid_at`             datetime      NULL COMMENT '支付成功时间',
    `address_name`        varchar(64)   NOT NULL COMMENT '收货人姓名快照',
    `address_phone`       varchar(32)   NOT NULL COMMENT '联系电话快照',
    `address_province`    varchar(64)   NOT NULL COMMENT '省快照',
    `address_city`        varchar(64)   NOT NULL COMMENT '市快照',
    `address_district`    varchar(64)   NOT NULL COMMENT '区快照',
    `address_detail`      varchar(255)  NOT NULL COMMENT '详细地址快照',
    `logistics_provider`  varchar(128)  NULL COMMENT '物流服务商名称快照',
    `waybill_no`          varchar(64)   NULL COMMENT '运单号，一单一运单',
    `created_at`          datetime      NOT NULL COMMENT '下单时间',
    `updated_at`          datetime      NOT NULL COMMENT '状态更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_time` (`buyer_account_id`, `created_at`),
    KEY `idx_status` (`status`),
    KEY `idx_created` (`created_at`),
    CONSTRAINT `fk_order_buyer` FOREIGN KEY (`buyer_account_id`) REFERENCES `buyer_account` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ----------------------------
-- 订单明细（下单快照；售后 / 评价按明细挂载）
-- ----------------------------
CREATE TABLE `order_item` (
    `id`           int           NOT NULL AUTO_INCREMENT,
    `order_id`     int           NOT NULL COMMENT '订单 ID',
    `product_id`   int           NOT NULL COMMENT '商品 ID',
    `product_name` varchar(255)  NOT NULL COMMENT '商品名快照',
    `unit_price`   decimal(10,2) NOT NULL COMMENT '单价快照（= 下单瞬间适用价）',
    `quantity`     int           NOT NULL COMMENT '数量',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`),
    CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
    CONSTRAINT `fk_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ----------------------------
-- 支付记录（同一订单同时仅一笔有效支付）
-- ----------------------------
CREATE TABLE `payment_record` (
    `id`              int           NOT NULL AUTO_INCREMENT,
    `payment_no`      varchar(32)   NOT NULL COMMENT '支付单号',
    `order_no`        varchar(32)   NOT NULL COMMENT '订单号',
    `order_id`        int           NOT NULL COMMENT '订单 ID',
    `channel`         varchar(20)   NOT NULL COMMENT '四渠道枚举',
    `amount`          decimal(10,2) NOT NULL COMMENT '金额（明细快照价合计）',
    `status`          varchar(20)   NOT NULL COMMENT 'PROCESSING/SUCCESS/FAIL/REFUNDED',
    `channel_txn_no`  varchar(64)   NULL COMMENT '渠道流水号（对账依据）',
    `paid_at`         datetime      NULL COMMENT '支付时间',
    `created_at`      datetime      NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    UNIQUE KEY `uk_channel_txn_no` (`channel_txn_no`),
    KEY `idx_pay_order` (`order_id`),
    KEY `idx_pay_status` (`status`),
    CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ----------------------------
-- 退款记录（取消退款 / 售后退款共用；全额原路，快照价）
-- ----------------------------
CREATE TABLE `refund_record` (
    `id`              int           NOT NULL AUTO_INCREMENT,
    `refund_no`       varchar(32)   NOT NULL COMMENT '退款单号',
    `payment_no`      varchar(32)   NOT NULL COMMENT '关联支付单号',
    `after_sales_no`  varchar(32)   NULL COMMENT '关联售后单号（取消退款为 NULL）',
    `order_id`        int           NOT NULL COMMENT '订单 ID',
    `amount`          decimal(10,2) NOT NULL COMMENT '退款金额（下单快照价全额）',
    `channel`         varchar(20)   NOT NULL COMMENT '原支付渠道',
    `status`          varchar(20)   NOT NULL COMMENT 'PROCESSING/SUCCESS/FAIL',
    `created_at`      datetime      NOT NULL COMMENT '退款时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    KEY `idx_refund_payment` (`payment_no`),
    KEY `idx_refund_order` (`order_id`),
    CONSTRAINT `fk_refund_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ----------------------------
-- 售后单（挂在订单明细上的平行单据）
-- ----------------------------
CREATE TABLE `after_sales_order` (
    `id`                    int           NOT NULL AUTO_INCREMENT,
    `after_sales_no`        varchar(32)   NOT NULL COMMENT '售后单号',
    `order_id`              int           NOT NULL COMMENT '订单 ID',
    `order_item_id`         int           NOT NULL COMMENT '订单明细 ID',
    `buyer_account_id`      int           NOT NULL COMMENT '买家账号 ID',
    `product_id`            int           NOT NULL COMMENT '商品 ID',
    `product_name`          varchar(255)  NOT NULL COMMENT '商品名快照',
    `title`                 varchar(128)  NOT NULL COMMENT '申请标题',
    `description`           varchar(1000) NOT NULL COMMENT '问题描述',
    `images`                text          NULL COMMENT '图片 URL JSON 数组（0~3 张）',
    `status`                varchar(20)   NOT NULL COMMENT 'APPLYING/NEGOTIATING/CLOSED',
    `result`                varchar(32)   NULL COMMENT 'REFUND_AND_RETURN/REFUND_ONLY/EXCHANGE/REJECTED',
    `result_at`             datetime      NULL COMMENT '结果时间',
    `return_waybill_no`     varchar(64)   NULL COMMENT '退货运单号',
    `exchange_waybill_no`   varchar(64)   NULL COMMENT '换货寄出运单号',
    `created_at`            datetime      NOT NULL COMMENT '申请时间',
    `updated_at`            datetime      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_after_sales_no` (`after_sales_no`),
    KEY `idx_as_buyer_time` (`buyer_account_id`, `created_at`),
    KEY `idx_as_item` (`order_item_id`),
    KEY `idx_as_status` (`status`),
    CONSTRAINT `fk_as_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
    CONSTRAINT `fk_as_item` FOREIGN KEY (`order_item_id`) REFERENCES `order_item` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ----------------------------
-- 买家评价（每明细一次；一次性终态，无修改 / 删除）
-- ----------------------------
CREATE TABLE `review` (
    `id`                int          NOT NULL AUTO_INCREMENT,
    `order_id`          int          NOT NULL COMMENT '订单 ID',
    `order_item_id`     int          NOT NULL COMMENT '订单明细 ID（唯一）',
    `product_id`        int          NOT NULL COMMENT '商品 ID',
    `buyer_account_id`  int          NOT NULL COMMENT '买家账号 ID',
    `rating`            tinyint      NOT NULL COMMENT '评分 1~5',
    `content`           varchar(200) NULL COMMENT '文字评价 ≤200 字',
    `images`            text         NULL COMMENT '图片 URL JSON 数组（0~3 张）',
    `created_at`        datetime     NOT NULL COMMENT '提交时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_item` (`order_item_id`),
    KEY `idx_review_product` (`product_id`),
    KEY `idx_review_buyer` (`buyer_account_id`),
    CONSTRAINT `fk_review_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
    CONSTRAINT `fk_review_item` FOREIGN KEY (`order_item_id`) REFERENCES `order_item` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ----------------------------
-- 物流服务商（卖家维护，数量不限；买家端只读引用）
-- ----------------------------
CREATE TABLE `logistics_provider` (
    `id`         int          NOT NULL AUTO_INCREMENT,
    `name`       varchar(128) NOT NULL COMMENT '服务商名称（不可重名）',
    `created_at` datetime     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO `logistics_provider` (`id`, `name`, `created_at`) VALUES
    (1, '顺丰速运', '2026-11-01 10:00:00'),
    (2, '京东物流', '2026-11-01 10:05:00');

-- ----------------------------
-- 物流跟踪（仅展示，不推进订单状态）
-- ----------------------------
CREATE TABLE `logistics_tracking` (
    `id`          int          NOT NULL AUTO_INCREMENT,
    `order_id`    int          NOT NULL COMMENT '订单 ID',
    `waybill_no`  varchar(64)  NOT NULL COMMENT '运单号',
    `status`      varchar(32)  NOT NULL COMMENT 'PICKED_UP/IN_TRANSIT/OUT_FOR_DELIVERY/DELIVERED',
    `description` varchar(255) NULL COMMENT '跟踪描述',
    `tracked_at`  datetime     NOT NULL COMMENT '发生时间',
    `created_at`  datetime     NOT NULL COMMENT '写入时间',
    PRIMARY KEY (`id`),
    KEY `idx_tracking_order` (`order_id`),
    CONSTRAINT `fk_tracking_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ----------------------------
-- 模拟渠道记录（可对账的渠道记录；channelTxnNo 唯一）
-- ----------------------------
CREATE TABLE `mock_channel_txn` (
    `id`              int           NOT NULL AUTO_INCREMENT,
    `channel_txn_no`  varchar(64)   NOT NULL COMMENT '渠道流水号（唯一）',
    `payment_no`      varchar(32)   NOT NULL COMMENT '支付单号',
    `channel`         varchar(20)   NOT NULL COMMENT '支付渠道',
    `amount`          decimal(10,2) NOT NULL COMMENT '金额',
    `mock_result`     varchar(20)   NOT NULL COMMENT 'SUCCESS / FAIL',
    `callback_at`     datetime      NOT NULL COMMENT '回调时间',
    `created_at`      datetime      NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_txn_no` (`channel_txn_no`),
    KEY `idx_txn_payment` (`payment_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ----------------------------
-- 演示数据：一笔交易完成订单（含评价）+ 一笔开始发货订单（含物流跟踪）
-- ----------------------------
INSERT INTO `orders`
    (`id`, `order_no`, `status`, `total_amount`, `buyer_account_id`, `payment_channel`, `paid_at`,
     `address_name`, `address_phone`, `address_province`, `address_city`, `address_district`, `address_detail`,
     `logistics_provider`, `waybill_no`, `created_at`, `updated_at`)
VALUES
    (1, '20261101-0001', 'COMPLETED', 159.00, 1, 'ALIPAY', '2026-11-01 09:05:00',
     '张三', '13800138000', '广东', '深圳', '南山区', '科技园路 1 号',
     '顺丰速运', 'SF1000000001', '2026-11-01 09:00:00', '2026-11-03 18:00:00'),
    (2, '20261102-0001', 'SHIPPING', 1599.00, 1, 'WECHAT_PAY', '2026-11-02 10:05:00',
     '李四', '13900139000', '北京', '北京', '海淀区', '中关村大街 5 号',
     '顺丰速运', 'SF1000000002', '2026-11-02 10:00:00', '2026-11-04 09:00:00');

INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `product_name`, `unit_price`, `quantity`) VALUES
    (1, 1, 5, '二手山地自行车', 159.00, 1),
    (2, 2, 6, 'AirPods Pro 2', 1599.00, 1);

INSERT INTO `payment_record`
    (`id`, `payment_no`, `order_no`, `order_id`, `channel`, `amount`, `status`, `channel_txn_no`, `paid_at`, `created_at`)
VALUES
    (1, 'PAY202611010001', '20261101-0001', 1, 'ALIPAY', 159.00, 'SUCCESS', 'MOCK20261101090500', '2026-11-01 09:05:00', '2026-11-01 09:04:00'),
    (2, 'PAY202611020001', '20261102-0001', 2, 'WECHAT_PAY', 1599.00, 'SUCCESS', 'MOCK20261102100500', '2026-11-02 10:05:00', '2026-11-02 10:04:00');

INSERT INTO `review`
    (`id`, `order_id`, `order_item_id`, `product_id`, `buyer_account_id`, `rating`, `content`, `images`, `created_at`)
VALUES
    (1, 1, 1, 5, 1, 5, '骑行顺畅，成色不错，卖家发货也快', '[]', '2026-11-03 20:00:00');

INSERT INTO `logistics_tracking` (`order_id`, `waybill_no`, `status`, `description`, `tracked_at`, `created_at`) VALUES
    (1, 'SF1000000001', 'DELIVERED', '快件已签收', '2026-11-03 17:40:00', '2026-11-03 17:41:00'),
    (1, 'SF1000000001', 'OUT_FOR_DELIVERY', '快件正在派送', '2026-11-03 09:00:00', '2026-11-03 09:01:00'),
    (1, 'SF1000000001', 'IN_TRANSIT', '快件已到达深圳转运中心', '2026-11-02 09:12:00', '2026-11-02 09:13:00'),
    (1, 'SF1000000001', 'PICKED_UP', '顺丰速运已揽收', '2026-11-01 18:30:00', '2026-11-01 18:31:00'),
    (2, 'SF1000000002', 'IN_TRANSIT', '快件已到达北京转运中心', '2026-11-05 09:12:00', '2026-11-05 09:13:00'),
    (2, 'SF1000000002', 'PICKED_UP', '顺丰速运已揽收', '2026-11-04 18:30:00', '2026-11-04 18:31:00');

SET FOREIGN_KEY_CHECKS = 1;
