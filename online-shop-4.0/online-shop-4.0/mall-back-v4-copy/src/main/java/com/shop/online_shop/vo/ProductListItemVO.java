package com.shop.online_shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项（v4.0：新增库存 / 可卖量与聚合评分）
 */
@Data
public class ProductListItemVO {

    private Integer id;
    private String name;
    private BigDecimal price;
    /** 适用价（活动价生效时用于展示与计价） */
    private BigDecimal currentPrice;
    private BigDecimal salePrice;
    /** 账面库存 */
    private Integer stock;
    /** 可卖量 = 账面库存 − 预占量 */
    private Integer availableQuantity;
    private Double averageRating;
    private Integer reviewCount;
    private String mainImageUrl;
    private Integer categoryId;
    private String categoryName;
    private String status;
    private LocalDateTime publishedAt;
}
