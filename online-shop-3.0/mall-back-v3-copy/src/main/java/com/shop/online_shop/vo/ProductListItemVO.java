package com.shop.online_shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项（v3.0 新增活动价字段）
 */
@Data
public class ProductListItemVO {

    private Integer id;
    private String name;
    private BigDecimal price;
    /** 适用价（活动价生效时用于展示与计价） */
    private BigDecimal currentPrice;
    private BigDecimal salePrice;
    private String mainImageUrl;
    private Integer categoryId;
    private String categoryName;
    private String status;
    private LocalDateTime publishedAt;
}
