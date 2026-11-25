package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.ProductStatus;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.service.CategoryService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.service.ReservedQuantityService;
import com.shop.online_shop.service.ReviewService;
import com.shop.online_shop.vo.PageResult;
import com.shop.online_shop.vo.ProductListItemVO;
import com.shop.online_shop.vo.RatingStats;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private static final String SORT_TIME_DESC = "TIME_DESC";
    private static final String SORT_TIME_ASC = "TIME_ASC";
    private static final String SORT_CATEGORY = "CATEGORY";

    @Resource
    private CategoryService categoryService;

    @Resource
    private ReservedQuantityService reservedQuantityService;

    @Resource
    private ReviewService reviewService;

    @Override
    public Product getCurrentOnlineProduct() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatus.ONLINE)
                .orderByDesc(Product::getPublishedAt)
                .orderByDesc(Product::getId)
                .last("LIMIT 1");
        Product product = getOne(wrapper, false);
        fillTransientFields(product);
        return product;
    }

    @Override
    public PageResult<ProductListItemVO> listProducts(String keyword, Integer categoryId, String sort,
                                                       Integer page, Integer size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatus.ONLINE);

        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            wrapper.like(Product::getName, trimmed);
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        applySort(wrapper, sort);

        Page<Product> productPage = page(new Page<>(page, size), wrapper);
        return toPageResult(productPage);
    }

    @Override
    public PageResult<ProductListItemVO> searchProducts(String keyword, Integer page, Integer size) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException("关键词不能为空");
        }
        String trimmedKeyword = keyword.trim();

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatus.ONLINE)
                .like(Product::getName, trimmedKeyword)
                .orderByDesc(Product::getPublishedAt)
                .orderByDesc(Product::getId);

        Page<Product> productPage = page(new Page<>(page, size), wrapper);
        return toPageResult(productPage);
    }

    @Override
    public Product getBuyerProductDetail(Integer id) {
        Product product = getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (ProductStatus.OFFLINE == product.getStatus()) {
            throw new BusinessException("商品已下架");
        }
        fillTransientFields(product);
        return product;
    }

    @Override
    public Product getProductWithFill(Integer id) {
        Product product = getById(id);
        fillTransientFields(product);
        return product;
    }

    private void applySort(LambdaQueryWrapper<Product> wrapper, String sort) {
        if (SORT_TIME_ASC.equals(sort)) {
            wrapper.orderByAsc(Product::getPublishedAt).orderByAsc(Product::getId);
        } else if (SORT_CATEGORY.equals(sort)) {
            wrapper.orderByAsc(Product::getCategoryId)
                    .orderByDesc(Product::getPublishedAt)
                    .orderByDesc(Product::getId);
        } else {
            wrapper.orderByDesc(Product::getPublishedAt).orderByDesc(Product::getId);
        }
    }

    private PageResult<ProductListItemVO> toPageResult(Page<Product> productPage) {
        Map<Integer, String> categoryNames = categoryService.getFullNameMap();
        List<Integer> productIds = productPage.getRecords().stream().map(Product::getId).toList();
        Map<Integer, Integer> reservedMap = reservedQuantityService.getReservedQuantities(productIds);
        Map<Integer, RatingStats> ratingMap = reviewService.getStatsBatch(productIds);

        List<ProductListItemVO> list = productPage.getRecords().stream()
                .map(product -> toListItem(product, categoryNames, reservedMap, ratingMap))
                .toList();
        return new PageResult<>(list,
                (int) productPage.getTotal(),
                (int) productPage.getCurrent(),
                (int) productPage.getSize());
    }

    private ProductListItemVO toListItem(Product product, Map<Integer, String> categoryNames,
                                         Map<Integer, Integer> reservedMap,
                                         Map<Integer, RatingStats> ratingMap) {
        ProductListItemVO vo = new ProductListItemVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setPrice(product.getPrice());
        vo.setCurrentPrice(currentPrice(product));
        vo.setSalePrice(product.getSalePrice());
        vo.setStock(product.getStock());
        int reserved = reservedMap.getOrDefault(product.getId(), 0);
        vo.setAvailableQuantity((product.getStock() == null ? 0 : product.getStock()) - reserved);
        RatingStats stats = ratingMap.get(product.getId());
        vo.setAverageRating(stats == null ? 0.0 : stats.getAverageRating());
        vo.setReviewCount(stats == null ? 0 : stats.getReviewCount());
        vo.setMainImageUrl(firstImage(product));
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(categoryNames.get(product.getCategoryId()));
        vo.setStatus(product.getStatus() != null ? product.getStatus().getCode() : null);
        vo.setPublishedAt(product.getPublishedAt());
        return vo;
    }

    private void fillTransientFields(Product product) {
        if (product == null) {
            return;
        }
        product.setCurrentPrice(currentPrice(product));
        product.setMainImageUrl(firstImage(product));
        product.setCategoryName(categoryService.getFullName(product.getCategoryId()));

        // v4.0 库存三层口径：预占 / 可卖（派生不落库）
        int stock = product.getStock() == null ? 0 : product.getStock();
        int reserved = reservedQuantityService.getReservedQuantity(product.getId());
        product.setReservedQuantity(reserved);
        product.setAvailableQuantity(stock - reserved);
        product.setShippedQuantity(reservedQuantityService.getShippedQuantity(product.getId()));
        product.setAvailableAlert(stock - reserved <= 0);

        // v4.0 聚合评分：实时计算不落库
        RatingStats stats = reviewService.getStats(product.getId());
        product.setAverageRating(stats.getAverageRating());
        product.setReviewCount(stats.getReviewCount());
    }

    /**
     * 适用价：活动期内 = 活动价，否则 = 发布价。
     * 以服务端时间判定起止边界。
     */
    private BigDecimal currentPrice(Product product) {
        if (product.getSalePrice() != null
                && product.getSaleStartAt() != null
                && product.getSaleEndAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (!now.isBefore(product.getSaleStartAt()) && !now.isAfter(product.getSaleEndAt())) {
                return product.getSalePrice();
            }
        }
        return product.getPrice();
    }

    private String firstImage(Product product) {
        if (product.getImageUrls() == null || product.getImageUrls().isEmpty()) {
            return null;
        }
        return product.getImageUrls().get(0);
    }
}
