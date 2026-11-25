package com.shop.online_shop.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价 VO（商品页展示，买家昵称脱敏）
 */
@Data
public class ReviewVO {

    private Integer reviewId;
    private Integer productId;
    private Integer itemId;
    private Integer rating;
    private String content;
    private List<String> images;
    private String buyerMaskedName;
    private LocalDateTime createdAt;
}
