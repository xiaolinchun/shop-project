package com.shop.online_shop.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 评价聚合统计（实时计算值）
 */
@Data
public class RatingStats {

    private Double averageRating = 0.0;
    private Integer reviewCount = 0;

    public static RatingStats empty() {
        return new RatingStats();
    }

    public RatingStats(Double averageRating, Integer reviewCount) {
        this.averageRating = averageRating == null ? 0.0 : averageRating;
        this.reviewCount = reviewCount == null ? 0 : reviewCount;
    }

    public RatingStats() {
    }
}
