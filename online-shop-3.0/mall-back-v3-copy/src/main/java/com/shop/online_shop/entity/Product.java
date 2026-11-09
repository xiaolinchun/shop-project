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

    /** 有效在途量（展示预警口径，派生字段不落库） */
    @TableField(exist = false)
    private Integer pendingQuantity;

    /** 已发货量（发货校验口径，派生字段不落库） */
    @TableField(exist = false)
    private Integer shippedQuantity;

    /** 在途量 ≥ 库存时为 true（前端飘红） */
    @TableField(exist = false)
    private Boolean pendingAlert;
}
