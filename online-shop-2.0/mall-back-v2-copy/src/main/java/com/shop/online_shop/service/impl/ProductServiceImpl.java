package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.PublishProductDTO;
import com.shop.online_shop.entity.Category;
import com.shop.online_shop.entity.Intent;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.IntentStatus;
import com.shop.online_shop.enums.ProductStatus;
import com.shop.online_shop.mapper.IntentMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.service.CategoryService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.vo.PageResult;
import com.shop.online_shop.vo.ProductListItemVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private IntentMapper intentMapper;

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
    public PageResult<ProductListItemVO> listProducts(Integer categoryId, String sort, Integer page, Integer size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatus.ONLINE);
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
                .and(w -> w.like(Product::getName, trimmedKeyword)
                        .or()
                        .like(Product::getDescription, trimmedKeyword))
                .orderByDesc(Product::getPublishedAt)
                .orderByDesc(Product::getId);

        Page<Product> productPage = page(new Page<>(page, size), wrapper);
        return toPageResult(productPage);
    }

    @Override
    public Product getProductDetail(Integer id) {
        Product product = getById(id);
        if (product == null || ProductStatus.OFFLINE == product.getStatus()) {
            throw new BusinessException("商品不存在或已下架");
        }
        fillTransientFields(product);
        return product;
    }

    @Override
    @Transactional
    public void publish(PublishProductDTO dto, String sellerId) {
        Category category = categoryService.getById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        if (category.getParentId() == null || category.getParentId() == 0) {
            throw new BusinessException("商品必须选择二级分类");
        }
        if (dto.getImageUrls() != null && dto.getImageUrls().size() > 9) {
            throw new BusinessException("图片最多9张");
        }

        Product product = new Product();
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setImageUrls(dto.getImageUrls());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setStatus(ProductStatus.ONLINE);
        product.setPublishedAt(LocalDateTime.now());
        product.setClosedAt(null);
        product.setSellerId(sellerId);
        product.setCategoryId(dto.getCategoryId());
        save(product);
    }

    @Override
    public PageResult<Product> listSellerProducts(String sellerId, String status, Integer page, Integer size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getSellerId, sellerId);
        if (StringUtils.hasText(status)) {
            ProductStatus productStatus;
            try {
                productStatus = ProductStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的商品状态");
            }
            wrapper.eq(Product::getStatus, productStatus);
        }
        wrapper.orderByDesc(Product::getPublishedAt)
                .orderByDesc(Product::getId);

        Page<Product> productPage = page(new Page<>(page, size), wrapper);
        fillTransientFieldsInPage(productPage);

        return new PageResult<>(productPage.getRecords(),
                (int) productPage.getTotal(),
                (int) productPage.getCurrent(),
                (int) productPage.getSize());
    }

    @Override
    public List<Product> listSellerHistory(String sellerId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getSellerId, sellerId)
                .eq(Product::getStatus, ProductStatus.OFFLINE)
                .orderByDesc(Product::getClosedAt)
                .orderByDesc(Product::getId);

        List<Product> products = list(wrapper);
        for (Product product : products) {
            fillTransientFields(product);
        }
        return products;
    }

    @Override
    public Product getSellerProduct(Integer id, String sellerId) {
        Product product = getById(id);
        if (product == null || !sellerId.equals(product.getSellerId())) {
            throw new BusinessException("无权限操作该商品");
        }
        fillTransientFields(product);
        return product;
    }

    @Override
    @Transactional
    public void updateStock(Integer id, Integer stock, String sellerId) {
        Product product = getSellerProduct(id, sellerId);
        if (stock < 0) {
            throw new BusinessException("库存不能为负");
        }
        product.setStock(stock);
        if (stock == 0 && ProductStatus.ONLINE == product.getStatus()) {
            product.setStatus(ProductStatus.OFFLINE);
            product.setClosedAt(LocalDateTime.now());
        }
        updateById(product);
    }

    @Override
    @Transactional
    public void changeCategory(Integer id, Integer categoryId, String sellerId) {
        Product product = getSellerProduct(id, sellerId);
        Category category = categoryService.getById(categoryId);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        if (category.getParentId() == null || category.getParentId() == 0) {
            throw new BusinessException("必须选择二级分类");
        }
        product.setCategoryId(categoryId);
        updateById(product);
    }

    @Override
    @Transactional
    public void freeze(Integer id, String sellerId) {
        Product product = getSellerProduct(id, sellerId);
        if (ProductStatus.ONLINE != product.getStatus()) {
            throw new BusinessException("商品当前不是在售状态");
        }
        product.setStatus(ProductStatus.FROZEN);
        updateById(product);
    }

    @Override
    @Transactional
    public void unfreeze(Integer id, String sellerId) {
        Product product = getSellerProduct(id, sellerId);
        if (ProductStatus.FROZEN != product.getStatus()) {
            throw new BusinessException("商品当前不是已冻结状态");
        }
        product.setStatus(ProductStatus.ONLINE);
        updateById(product);
    }

    @Override
    @Transactional
    public void offShelf(Integer id, String sellerId) {
        Product product = getSellerProduct(id, sellerId);
        if (ProductStatus.ONLINE != product.getStatus()) {
            throw new BusinessException("商品当前不是在售状态");
        }
        if (hasTradingIntent(id)) {
            throw new BusinessException("该商品存在进行中的交易，无法下架");
        }
        product.setStatus(ProductStatus.OFFLINE);
        product.setClosedAt(LocalDateTime.now());
        updateById(product);

        cancelAllQueuedIntents(id);
    }

    @Override
    @Transactional
    public void onShelf(Integer id, String sellerId) {
        Product product = getSellerProduct(id, sellerId);
        if (ProductStatus.OFFLINE != product.getStatus()) {
            throw new BusinessException("商品当前不是已下架状态");
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new BusinessException("库存为 0，请先补充库存");
        }
        product.setStatus(ProductStatus.ONLINE);
        product.setClosedAt(null);
        updateById(product);
    }

    private boolean hasTradingIntent(Integer productId) {
        LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Intent::getProductId, productId)
                .eq(Intent::getStatus, IntentStatus.TRADING);
        return intentMapper.selectCount(wrapper) > 0;
    }

    private void cancelAllQueuedIntents(Integer productId) {
        LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Intent::getProductId, productId)
                .in(Intent::getStatus, IntentStatus.QUEUED, IntentStatus.TRADING);
        List<Intent> intents = intentMapper.selectList(wrapper);
        LocalDateTime now = LocalDateTime.now();
        for (Intent intent : intents) {
            intent.setStatus(IntentStatus.FAILED);
            intent.setUpdatedAt(now);
            intent.setResultAt(now);
            intentMapper.updateById(intent);
        }
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
        List<ProductListItemVO> list = productPage.getRecords().stream()
                .map(product -> toListItem(product, categoryNames))
                .toList();
        return new PageResult<>(list,
                (int) productPage.getTotal(),
                (int) productPage.getCurrent(),
                (int) productPage.getSize());
    }

    private ProductListItemVO toListItem(Product product, Map<Integer, String> categoryNames) {
        ProductListItemVO vo = new ProductListItemVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setPrice(product.getPrice());
        vo.setMainImageUrl(firstImage(product));
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(categoryNames.get(product.getCategoryId()));
        vo.setStatus(product.getStatus());
        vo.setPublishedAt(product.getPublishedAt());
        return vo;
    }

    private void fillTransientFields(Product product) {
        if (product == null) {
            return;
        }
        product.setMainImageUrl(firstImage(product));
        product.setCategoryName(categoryService.getFullName(product.getCategoryId()));
    }

    private void fillTransientFieldsInPage(Page<Product> productPage) {
        for (Product product : productPage.getRecords()) {
            fillTransientFields(product);
        }
    }

    private String firstImage(Product product) {
        if (product.getImageUrls() == null || product.getImageUrls().isEmpty()) {
            return null;
        }
        return product.getImageUrls().get(0);
    }
}
