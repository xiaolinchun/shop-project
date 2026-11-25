package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.shop.online_shop.enums.OffReason;
import com.shop.online_shop.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "product", autoResultMap = true)
public class Product {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String description;

    @TableField(value = "image_urls", typeHandler = JacksonTypeHandler.class)
    private List<String> imageUrls;

    private BigDecimal price;

    /** 限时活动价，未设置时为 null */
    private BigDecimal salePrice;

    /** 活动开始时间 */
    private LocalDateTime saleStartAt;

    /** 活动结束时间 */
    private LocalDateTime saleEndAt;

    private ProductStatus status;

    /** 下架原因：MANUAL / STOCK_OUT */
    private OffReason offReason;

    /** 账面库存（非负整数），唯一库存数 */
    private Integer stock;

    private LocalDateTime publishedAt;

    private LocalDateTime closedAt;

    private String sellerId;

    private Integer categoryId;

    /** 适用价（活动期内 = 活动价，否则 = 发布价），计算字段 */
    @TableField(exist = false)
    private BigDecimal currentPrice;

    @TableField(exist = false)
    private String mainImageUrl;

    @TableField(exist = false)
    private String categoryName;

    // ===================== v4.0 库存三层口径（均为派生值，不落库） =====================

    /** 预占量 = 已支付且未成交订单明细量之和 */
    @TableField(exist = false)
    private Integer reservedQuantity;

    /** 可卖量 = 账面库存 − 预占量（下单与支付校验对象） */
    @TableField(exist = false)
    private Integer availableQuantity;

    /** 已发货量（降级为纯展示，不再参与发货校验） */
    @TableField(exist = false)
    private Integer shippedQuantity;

    /** 可卖量 ≤ 0 时为 true（前端提示缺货风险） */
    @TableField(exist = false)
    private Boolean availableAlert;

    // ===================== v4.0 聚合评分（实时计算，不落库） =====================

    @TableField(exist = false)
    private Double averageRating;

    @TableField(exist = false)
    private Integer reviewCount;
}
