package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.buyer.ReviewSubmitDTO;
import com.shop.online_shop.entity.Review;
import com.shop.online_shop.vo.ProductReviewPageVO;
import com.shop.online_shop.vo.RatingStats;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ReviewService extends IService<Review> {

    /** 2.29 按订单明细提交评价，返回评价 ID */
    Integer submit(Integer buyerAccountId, Integer itemId, ReviewSubmitDTO dto);

    /** 2.30 商品评价列表（聚合评分 + 分页列表） */
    ProductReviewPageVO pageByProduct(Integer productId, Integer page, Integer size);

    RatingStats getStats(Integer productId);

    Map<Integer, RatingStats> getStatsBatch(Collection<Integer> productIds);

    /** 已评价的订单明细 ID 集合（订单历史行计算字段） */
    Set<Integer> getReviewedItemIds(Collection<Integer> orderItemIds);
}
