package com.shop.online_shop.vo;

import lombok.Data;

/**
 * 商品维度的评价聚合行（Mapper 批量查询用）
 */
@Data
public class ProductRatingRow {

    private Integer productId;
    private Double averageRating;
    private Integer reviewCount;
}
