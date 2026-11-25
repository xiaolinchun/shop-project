package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.buyer.ReviewSubmitDTO;
import com.shop.online_shop.entity.AfterSalesOrder;
import com.shop.online_shop.entity.BuyerAccount;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.entity.OrderItem;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.entity.Review;
import com.shop.online_shop.enums.AfterSalesResult;
import com.shop.online_shop.enums.AfterSalesStatus;
import com.shop.online_shop.enums.OrderStatus;
import com.shop.online_shop.enums.ProductStatus;
import com.shop.online_shop.mapper.AfterSalesOrderMapper;
import com.shop.online_shop.mapper.BuyerAccountMapper;
import com.shop.online_shop.mapper.OrderItemMapper;
import com.shop.online_shop.mapper.OrderMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.mapper.ReviewMapper;
import com.shop.online_shop.service.ReviewService;
import com.shop.online_shop.vo.ProductRatingRow;
import com.shop.online_shop.vo.ProductReviewPageVO;
import com.shop.online_shop.vo.RatingStats;
import com.shop.online_shop.vo.ReviewVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    private static final int MAX_CONTENT_LENGTH = 200;
    private static final int MAX_IMAGES = 3;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private BuyerAccountMapper buyerAccountMapper;

    @Resource
    private AfterSalesOrderMapper afterSalesOrderMapper;

    @Override
    @Transactional
    public Integer submit(Integer buyerAccountId, Integer itemId, ReviewSubmitDTO dto) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("订单明细不存在");
        }
        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null || !buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BusinessException("无权访问");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("当前状态不可评价");
        }
        if (countByItemId(itemId) > 0) {
            throw new BusinessException("该明细已评价");
        }
        if (isRefundClosed(itemId)) {
            throw new BusinessException("该明细已退款闭环，不可评价");
        }

        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new BusinessException("评分必须为1~5");
        }
        if (dto.getContent() != null && dto.getContent().length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("评价文字不能超过200字");
        }
        if (dto.getImages() != null && dto.getImages().size() > MAX_IMAGES) {
            throw new BusinessException("最多上传3张图片");
        }

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setOrderItemId(itemId);
        review.setProductId(item.getProductId());
        review.setBuyerAccountId(buyerAccountId);
        review.setRating(dto.getRating());
        review.setContent(StringUtils.hasText(dto.getContent()) ? dto.getContent().trim() : null);
        review.setImages(dto.getImages() == null ? new ArrayList<>() : dto.getImages());
        review.setCreatedAt(LocalDateTime.now());
        save(review);
        return review.getId();
    }

    @Override
    public ProductReviewPageVO pageByProduct(Integer productId, Integer page, Integer size) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (ProductStatus.OFFLINE == product.getStatus()) {
            throw new BusinessException("商品已下架");
        }

        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getProductId, productId)
                .orderByDesc(Review::getCreatedAt)
                .orderByDesc(Review::getId);
        Page<Review> reviewPage = page(new Page<>(pageNo, pageSize), wrapper);

        Map<Integer, String> maskedNames = new HashMap<>();
        List<ReviewVO> list = new ArrayList<>();
        for (Review review : reviewPage.getRecords()) {
            ReviewVO vo = new ReviewVO();
            vo.setReviewId(review.getId());
            vo.setProductId(review.getProductId());
            vo.setItemId(review.getOrderItemId());
            vo.setRating(review.getRating());
            vo.setContent(review.getContent());
            vo.setImages(review.getImages());
            vo.setCreatedAt(review.getCreatedAt());
            vo.setBuyerMaskedName(maskedNames.computeIfAbsent(review.getBuyerAccountId(),
                    this::maskBuyerName));
            list.add(vo);
        }

        RatingStats stats = getStats(productId);
        ProductReviewPageVO result = new ProductReviewPageVO();
        result.setProductId(productId);
        result.setAverageRating(stats.getAverageRating());
        result.setReviewCount(stats.getReviewCount());
        result.setList(list);
        result.setTotal((int) reviewPage.getTotal());
        result.setPage((int) reviewPage.getCurrent());
        result.setSize((int) reviewPage.getSize());
        return result;
    }

    @Override
    public RatingStats getStats(Integer productId) {
        if (productId == null) {
            return RatingStats.empty();
        }
        Double average = baseMapper.averageRating(productId);
        int count = baseMapper.countByProduct(productId);
        return new RatingStats(round1(average), count);
    }

    @Override
    public Map<Integer, RatingStats> getStatsBatch(Collection<Integer> productIds) {
        Map<Integer, RatingStats> result = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return result;
        }
        List<ProductRatingRow> rows = baseMapper.statsByProductIds(List.copyOf(productIds));
        for (ProductRatingRow row : rows) {
            result.put(row.getProductId(), new RatingStats(round1(row.getAverageRating()),
                    row.getReviewCount() == null ? 0 : row.getReviewCount()));
        }
        return result;
    }

    @Override
    public Set<Integer> getReviewedItemIds(Collection<Integer> orderItemIds) {
        if (orderItemIds == null || orderItemIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Review::getOrderItemId)
                .in(Review::getOrderItemId, orderItemIds);
        Set<Integer> result = new HashSet<>();
        for (Review review : list(wrapper)) {
            result.add(review.getOrderItemId());
        }
        return result;
    }

    private int countByItemId(Integer itemId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getOrderItemId, itemId);
        return (int) count(wrapper);
    }

    private boolean isRefundClosed(Integer itemId) {
        LambdaQueryWrapper<AfterSalesOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSalesOrder::getOrderItemId, itemId)
                .eq(AfterSalesOrder::getStatus, AfterSalesStatus.CLOSED)
                .in(AfterSalesOrder::getResult,
                        AfterSalesResult.REFUND_AND_RETURN, AfterSalesResult.REFUND_ONLY);
        return afterSalesOrderMapper.selectCount(wrapper) > 0;
    }

    private String maskBuyerName(Integer buyerAccountId) {
        BuyerAccount account = buyerAccountMapper.selectById(buyerAccountId);
        if (account == null || !StringUtils.hasText(account.getUsername())) {
            return "匿名买家";
        }
        String username = account.getUsername().trim();
        return username.substring(0, 1) + "**";
    }

    private Double round1(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.round(value * 10) / 10.0;
    }
}
