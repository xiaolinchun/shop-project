package com.shop.online_shop.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车条目 VO
 */
@Data
public class CartItemVO {

    private Integer id;
    private Integer productId;
    private String productName;
    private String mainImageUrl;
    /** 当前适用价（活动期内为活动价） */
    private BigDecimal unitPrice;
    private Integer quantity;
    /** 商品是否在售（已下架为 false，置灰不可勾选） */
    private Boolean onSale;
}
