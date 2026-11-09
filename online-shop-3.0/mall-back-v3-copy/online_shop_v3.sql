/*
 Navicat Premium Dump SQL

 Source Server         : MySQL
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : online_shop_v3

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 08/11/2026 21:50:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for buyer_account
-- ----------------------------
DROP TABLE IF EXISTS `buyer_account`;
CREATE TABLE `buyer_account`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名，唯一',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt 加密密码，任何接口均不返回',
  `phone` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号，不唯一',
  `default_trade_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '默认交易地点，可为空（不入订单）',
  `token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '当前登录令牌，48 位随机串',
  `created_at` datetime NOT NULL COMMENT '注册时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_token`(`token` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of buyer_account
-- ----------------------------
INSERT INTO `buyer_account` VALUES (1, 'buyer01', '$2a$10$B7ouTG/9ejNcGFdagr5qwuAgZsPMrsAlqSDhEk7CsigQCJTaBScjO', '13800138000', '图书馆正门', 'GRPv54pPXNti512Wylu1g4GTCl36iiABzddWa9xu0CV3O2N0', '2026-10-10 20:15:00');

-- ----------------------------
-- Table structure for cart_item
-- ----------------------------
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `buyer_account_id` int NOT NULL COMMENT '买家账号 ID',
  `product_id` int NOT NULL COMMENT '商品 ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量（1~999）',
  `created_at` datetime NOT NULL COMMENT '加入时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_buyer_product`(`buyer_account_id` ASC, `product_id` ASC) USING BTREE,
  INDEX `idx_buyer_id`(`buyer_account_id` ASC) USING BTREE,
  INDEX `fk_cart_product`(`product_id` ASC) USING BTREE,
  CONSTRAINT `fk_cart_buyer` FOREIGN KEY (`buyer_account_id`) REFERENCES `buyer_account` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_cart_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cart_item
-- ----------------------------

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `parent_id` int NOT NULL DEFAULT 0 COMMENT '父分类 ID，一级分类为 0',
  `category_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_parent_name`(`parent_id` ASC, `category_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of category
-- ----------------------------
INSERT INTO `category` VALUES (3, 0, '家用电器');
INSERT INTO `category` VALUES (1, 0, '手机数码');
INSERT INTO `category` VALUES (2, 0, '电脑办公');
INSERT INTO `category` VALUES (7, 1, '平板电脑');
INSERT INTO `category` VALUES (5, 1, '手机');
INSERT INTO `category` VALUES (8, 1, '耳机');
INSERT INTO `category` VALUES (6, 2, '笔记本电脑');
INSERT INTO `category` VALUES (9, 3, '冰箱');

-- ----------------------------
-- Table structure for favorite
-- ----------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `buyer_account_id` int NOT NULL COMMENT '买家账号 ID',
  `product_id` int NOT NULL COMMENT '商品 ID',
  `created_at` datetime NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_buyer_product`(`buyer_account_id` ASC, `product_id` ASC) USING BTREE,
  INDEX `idx_buyer_id`(`buyer_account_id` ASC) USING BTREE,
  INDEX `fk_favorite_product`(`product_id` ASC) USING BTREE,
  CONSTRAINT `fk_favorite_buyer` FOREIGN KEY (`buyer_account_id`) REFERENCES `buyer_account` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_favorite_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of favorite
-- ----------------------------

-- ----------------------------
-- Table structure for order_item
-- ----------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL COMMENT '订单 ID',
  `product_id` int NOT NULL COMMENT '商品 ID',
  `product_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名快照',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '单价快照（= 下单瞬间适用价）',
  `quantity` int NOT NULL COMMENT '数量',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_item
-- ----------------------------

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号（日期+4位流水）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'CREATED/CONFIRMED/STOCKED/SHIPPING/COMPLETED/CANCELLED',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '总金额 = Σ(明细单价×数量)',
  `buyer_account_id` int NOT NULL COMMENT '买家账号 ID',
  `created_at` datetime NOT NULL COMMENT '下单时间',
  `updated_at` datetime NOT NULL COMMENT '状态更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_buyer_time`(`buyer_account_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_order_buyer` FOREIGN KEY (`buyer_account_id`) REFERENCES `buyer_account` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders
-- ----------------------------

-- ----------------------------
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '富文本描述（HTML 片段）',
  `image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '图片地址 JSON 数组，第一条为主图，至多 9 条',
  `price` decimal(10, 2) NOT NULL COMMENT '发布价，保留 2 位小数',
  `sale_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '限时活动价，未设置时为 NULL',
  `sale_start_at` datetime NULL DEFAULT NULL COMMENT '活动开始时间',
  `sale_end_at` datetime NULL DEFAULT NULL COMMENT '活动结束时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ONLINE 在售 / OFFLINE 已下架',
  `off_reason` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '下架原因：MANUAL 手动 / STOCK_OUT 库存归零自动下架',
  `stock` int NOT NULL DEFAULT 0 COMMENT '库存，非负整数',
  `published_at` datetime NOT NULL COMMENT '发布时间',
  `closed_at` datetime NULL DEFAULT NULL COMMENT '下架时间',
  `seller_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卖家用户名',
  `category_id` int NOT NULL COMMENT '所属分类 ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_seller_id`(`seller_id` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_status_published`(`status` ASC, `published_at` ASC) USING BTREE,
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_product_seller` FOREIGN KEY (`seller_id`) REFERENCES `seller` (`username`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (1, 'iPhone 15', '<p>全新未拆封，支持全国联保。</p><ul><li>256G</li><li>原色钛金属</li></ul>', '[\"https://miaobi-lite.bj.bcebos.com/miaobi/5mao/b%27aW9zOOWOn%2BeUn%2BWjgee6uF8xNzI2ODYzNTc1LjkxNzM1NTg%3D%27/0.png\"]', 5999.00, NULL, NULL, NULL, 'ONLINE', NULL, 10, '2026-10-01 10:00:00', NULL, 'seller01', 5);
INSERT INTO `product` VALUES (2, '小米14 Ultra', '<p>徕卡影像旗舰，骁龙 8 Gen3，16+512G。</p>', '[\"https://cdn.example.com/mi14-1.jpg\"]', 6499.00, NULL, NULL, NULL, 'ONLINE', NULL, 5, '2026-10-02 14:30:00', NULL, 'seller01', 5);
INSERT INTO `product` VALUES (3, 'MacBook Air M3', '<p>13 英寸，M3 芯片，16G+512G，午夜色。</p>', '[\"https://cdn.example.com/mba-1.jpg\",\"https://cdn.example.com/mba-2.jpg\"]', 8999.00, NULL, NULL, NULL, 'ONLINE', NULL, 3, '2026-10-03 09:00:00', NULL, 'seller01', 6);
INSERT INTO `product` VALUES (4, 'iPad Air 5', '<p>M1 芯片，64G WiFi 版，带笔，轻微使用痕迹。</p>', '[\"https://cdn.example.com/ipad-1.jpg\"]', 3899.00, NULL, NULL, NULL, 'OFFLINE', 'STOCK_OUT', 0, '2026-09-01 10:00:00', '2026-09-30 22:00:00', 'seller01', 7);
INSERT INTO `product` VALUES (5, '二手山地自行车', '<p>九成新，变速灵敏。</p>', '[\"/uploads/a1b2c3.png\",\"/uploads/d4e5f6.png\"]', 199.00, 159.00, '2026-11-01 00:00:00', '2026-11-11 23:59:59', 'ONLINE', NULL, 3, '2026-11-01 10:00:00', NULL, 'seller01', 5);
INSERT INTO `product` VALUES (6, 'AirPods Pro 2', '<p>主动降噪蓝牙耳机，在保。</p>', NULL, 1599.00, NULL, NULL, NULL, 'ONLINE', NULL, 20, '2026-10-05 20:00:00', NULL, 'seller01', 8);

-- ----------------------------
-- Table structure for seller
-- ----------------------------
DROP TABLE IF EXISTS `seller`;
CREATE TABLE `seller`  (
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '唯一卖家账号',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt 加密密码',
  `token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '当前登录令牌',
  PRIMARY KEY (`username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of seller
-- ----------------------------
INSERT INTO `seller` VALUES ('seller01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'k1bHc0lwBgUsgCzNI7dnSUwqgQsHd9RggDAxVXRhWQJuU4W7');

SET FOREIGN_KEY_CHECKS = 1;
