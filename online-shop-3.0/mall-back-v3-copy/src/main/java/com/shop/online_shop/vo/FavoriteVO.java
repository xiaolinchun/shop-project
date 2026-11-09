package com.shop.online_shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏条目 VO
 */
@Data
public class FavoriteVO {

    private Integer productId;
    private String productName;
    private String mainImageUrl;
    /** 发布价（划线用） */
    private BigDecimal price;
    /** 适用价 */
    private BigDecimal currentPrice;
    /** 是否在售（已下架置灰不可点，但保留显示） */
    private Boolean onSale;
    /** 收藏时间（列表按此倒序） */
    private LocalDateTime createdAt;
}
