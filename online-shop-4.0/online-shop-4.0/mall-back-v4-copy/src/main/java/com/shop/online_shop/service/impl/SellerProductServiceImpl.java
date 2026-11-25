package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.ProductPublishDTO;
import com.shop.online_shop.dto.seller.ProductSaleDTO;
import com.shop.online_shop.entity.Category;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.OffReason;
import com.shop.online_shop.enums.ProductStatus;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.service.CategoryService;
import com.shop.online_shop.service.ReservedQuantityService;
import com.shop.online_shop.service.ReviewService;
import com.shop.online_shop.service.SellerProductService;
import com.shop.online_shop.vo.PageResult;
import com.shop.online_shop.vo.RatingStats;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SellerProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements SellerProductService {

    private static final int MAX_IMAGES = 9;
    private static final int MAX_STOCK = 99999;

    @Resource
    private CategoryService categoryService;

    @Resource
    private ReservedQuantityService reservedQuantityService;

    @Resource
    private ReviewService reviewService;

    @Override
    @Transactional
    public void publish(String sellerId, ProductPublishDTO dto) {
        Category category = requireSecondLevelCategory(dto.getCategoryId());
        if (!CollectionUtils.isEmpty(dto.getImageUrls()) && dto.getImageUrls().size() > MAX_IMAGES) {
            throw new BusinessException("商品图片最多9张");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("价格必须大于0");
        }
        if (dto.getStock() == null || dto.getStock() < 0 || dto.getStock() > MAX_STOCK) {
            throw new BusinessException("库存必须在0~99999之间");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException("商品名称不能为空");
        }

        Product product = new Product();
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setImageUrls(CollectionUtils.isEmpty(dto.getImageUrls()) ? null : dto.getImageUrls());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCategoryId(category.getId());
        product.setSellerId(sellerId);
        product.setStatus(ProductStatus.ONLINE);
        product.setPublishedAt(LocalDateTime.now());
        save(product);
    }

    @Override
    public PageResult<Product> list(String sellerId, String keyword, Integer categoryId,
                                    String status, Integer page, Integer size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getSellerId, sellerId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword.trim());
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(status)) {
            if ("ONLINE".equalsIgnoreCase(status)) {
                wrapper.eq(Product::getStatus, ProductStatus.ONLINE);
            } else if ("OFFLINE".equalsIgnoreCase(status)) {
                wrapper.eq(Product::getStatus, ProductStatus.OFFLINE);
            } else {
                throw new BusinessException("不支持的商品状态");
            }
        }
        wrapper.orderByDesc(Product::getPublishedAt).orderByDesc(Product::getId);

        Page<Product> productPage = page(new Page<>(page, size), wrapper);
        productPage.getRecords().forEach(this::fillTransientFields);
        return new PageResult<>(productPage.getRecords(),
                (int) productPage.getTotal(),
                (int) productPage.getCurrent(),
                (int) productPage.getSize());
    }

    @Override
    public Product detail(String sellerId, Integer id) {
        Product product = requireOwned(sellerId, id, false);
        fillTransientFields(product);
        return product;
    }

    @Override
    public List<Product> history(String sellerId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getSellerId, sellerId)
                .eq(Product::getStatus, ProductStatus.OFFLINE)
                .orderByDesc(Product::getClosedAt)
                .orderByDesc(Product::getId);
        List<Product> products = list(wrapper);
        products.forEach(this::fillTransientFields);
        return products;
    }

    @Override
    @Transactional
    public void updateStock(String sellerId, Integer id, Integer stock) {
        if (stock == null || stock < 0 || stock > MAX_STOCK) {
            throw new BusinessException("库存必须在0~99999之间");
        }
        Product product = requireOwned(sellerId, id, true);
        if (stock == 0) {
            int reserved = reservedQuantityService.getReservedQuantity(product.getId());
            if (reserved > 0) {
                throw new BusinessException("存在已支付订单，暂不可修改库存为 0");
            }
            product.setStock(0);
            product.setStatus(ProductStatus.OFFLINE);
            product.setOffReason(OffReason.STOCK_OUT);
            if (product.getClosedAt() == null) {
                product.setClosedAt(LocalDateTime.now());
            }
        } else {
            product.setStock(stock);
        }
        updateById(product);
    }

    @Override
    @Transactional
    public void reCategory(String sellerId, Integer id, Integer categoryId) {
        Category category = requireSecondLevelCategory(categoryId);
        Product product = requireOwned(sellerId, id, false);
        product.setCategoryId(category.getId());
        updateById(product);
    }

    @Override
    @Transactional
    public void offShelf(String sellerId, Integer id) {
        Product product = requireOwned(sellerId, id, true);
        int reserved = reservedQuantityService.getReservedQuantity(product.getId());
        if (reserved > 0) {
            throw new BusinessException("存在已支付订单，暂不可下架");
        }
        if (product.getStatus() == ProductStatus.OFFLINE) {
            return;
        }
        product.setStatus(ProductStatus.OFFLINE);
        product.setOffReason(OffReason.MANUAL);
        product.setClosedAt(LocalDateTime.now());
        updateById(product);
    }

    @Override
    @Transactional
    public void onShelf(String sellerId, Integer id) {
        Product product = requireOwned(sellerId, id, true);
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new BusinessException("库存为0，无法上架");
        }
        if (product.getStatus() == ProductStatus.ONLINE) {
            return;
        }
        product.setStatus(ProductStatus.ONLINE);
        product.setOffReason(null);
        product.setClosedAt(null);
        updateById(product);
    }

    @Override
    @Transactional
    public void setSalePrice(String sellerId, Integer id, ProductSaleDTO dto) {
        Product product = requireOwned(sellerId, id, false);
        if (dto.getSalePrice() == null || dto.getSalePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("活动价必须大于0");
        }
        if (dto.getSaleStartAt() == null || dto.getSaleEndAt() == null) {
            throw new BusinessException("活动开始时间与结束时间不能为空");
        }
        if (!dto.getSaleEndAt().isAfter(dto.getSaleStartAt())) {
            throw new BusinessException("活动结束时间必须晚于开始时间");
        }
        product.setSalePrice(dto.getSalePrice());
        product.setSaleStartAt(dto.getSaleStartAt());
        product.setSaleEndAt(dto.getSaleEndAt());
        updateById(product);
    }

    private Category requireSecondLevelCategory(Integer categoryId) {
        if (categoryId == null) {
            throw new BusinessException("请选择二级分类");
        }
        Category category = categoryService.getById(categoryId);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        if (category.getParentId() == null || category.getParentId() == 0) {
            throw new BusinessException("请选择二级分类");
        }
        return category;
    }

    private Product requireOwned(String sellerId, Integer id, boolean forUpdate) {
        Product product = forUpdate ? baseMapper.selectByIdForUpdate(id) : getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!sellerId.equals(product.getSellerId())) {
            throw new BusinessException("无权访问");
        }
        return product;
    }

    private void fillTransientFields(Product product) {
        if (product == null) {
            return;
        }
        product.setCurrentPrice(currentPrice(product));
        product.setMainImageUrl(firstImage(product));
        product.setCategoryName(categoryService.getFullName(product.getCategoryId()));

        int stock = product.getStock() == null ? 0 : product.getStock();
        int reserved = reservedQuantityService.getReservedQuantity(product.getId());
        product.setReservedQuantity(reserved);
        product.setAvailableQuantity(stock - reserved);
        product.setShippedQuantity(reservedQuantityService.getShippedQuantity(product.getId()));
        product.setAvailableAlert(stock - reserved <= 0);

        RatingStats stats = reviewService.getStats(product.getId());
        product.setAverageRating(stats.getAverageRating());
        product.setReviewCount(stats.getReviewCount());
    }

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
