package com.shop.online_shop.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品评价页：聚合评分（实时计算，不落库）+ 分页评价列表
 */
@Data
public class ProductReviewPageVO {

    private Integer productId;
    private Double averageRating;
    private Integer reviewCount;
    private List<ReviewVO> list;
    private Integer total;
    private Integer page;
    private Integer size;
}
