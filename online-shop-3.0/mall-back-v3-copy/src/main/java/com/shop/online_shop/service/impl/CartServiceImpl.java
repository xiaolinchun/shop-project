package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.entity.CartItem;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.ProductStatus;
import com.shop.online_shop.mapper.CartItemMapper;
import com.shop.online_shop.service.CartService;
import com.shop.online_shop.service.FavoriteService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.vo.CartItemVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl extends ServiceImpl<CartItemMapper, CartItem> implements CartService {

    @Resource
    private ProductService productService;

    @Resource
    private FavoriteService favoriteService;

    @Override
    @Transactional
    public Integer addToCart(Integer buyerAccountId, Integer productId, Integer quantity) {
        int qty = quantity == null ? 1 : quantity;
        if (qty < 1 || qty > 999) {
            throw new BusinessException("数量必须在1~999之间");
        }

        Product product = productService.getById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (ProductStatus.OFFLINE == product.getStatus()) {
            throw new BusinessException("商品已下架");
        }

        // 同商品重复加购不新增条目，数量累加
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getBuyerAccountId, buyerAccountId)
                .eq(CartItem::getProductId, productId);
        CartItem existing = getOne(wrapper, false);

        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setQuantity(Math.min(existing.getQuantity() + qty, 999));
            existing.setUpdatedAt(now);
            updateById(existing);
            return existing.getId();
        }

        CartItem item = new CartItem();
        item.setBuyerAccountId(buyerAccountId);
        item.setProductId(productId);
        item.setQuantity(qty);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        save(item);
        return item.getId();
    }

    @Override
    public List<CartItemVO> listCart(Integer buyerAccountId) {
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getBuyerAccountId, buyerAccountId)
                .orderByDesc(CartItem::getUpdatedAt)
                .orderByDesc(CartItem::getId);
        List<CartItem> items = list(wrapper);

        List<CartItemVO> result = new ArrayList<>();
        for (CartItem item : items) {
            Product product = productService.getProductWithFill(item.getProductId());
            CartItemVO vo = new CartItemVO();
            vo.setId(item.getId());
            vo.setProductId(item.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setMainImageUrl(product.getMainImageUrl());
                vo.setUnitPrice(product.getCurrentPrice() != null ? product.getCurrentPrice() : product.getPrice());
                vo.setOnSale(ProductStatus.ONLINE == product.getStatus());
            } else {
                vo.setProductName("商品已删除");
                vo.setUnitPrice(BigDecimal.ZERO);
                vo.setOnSale(false);
            }
            vo.setQuantity(item.getQuantity());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void updateQuantity(Integer buyerAccountId, Integer id, Integer quantity) {
        if (quantity == null || quantity < 0 || quantity > 999) {
            throw new BusinessException("数量必须在0~999之间");
        }
        CartItem item = getOwnedItem(buyerAccountId, id);

        if (quantity == 0) {
            removeById(id);
            return;
        }
        item.setQuantity(quantity);
        item.setUpdatedAt(LocalDateTime.now());
        updateById(item);
    }

    @Override
    @Transactional
    public void removeItem(Integer buyerAccountId, Integer id) {
        getOwnedItem(buyerAccountId, id);
        removeById(id);
    }

    @Override
    @Transactional
    public Integer cartToFavorite(Integer buyerAccountId, List<Integer> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new BusinessException("请至少选择一条记录");
        }

        int successCount = 0;
        for (Integer cartItemId : cartItemIds) {
            CartItem item = getOwnedItem(buyerAccountId, cartItemId);
            // 已下架商品也可转收藏
            favoriteService.addFavorite(buyerAccountId, item.getProductId());
            removeById(cartItemId);
            successCount++;
        }
        return successCount;
    }

    private CartItem getOwnedItem(Integer buyerAccountId, Integer id) {
        CartItem item = getById(id);
        if (item == null) {
            throw new BusinessException("条目不存在");
        }
        if (!buyerAccountId.equals(item.getBuyerAccountId())) {
            throw new BusinessException("无权访问");
        }
        return item;
    }
}
