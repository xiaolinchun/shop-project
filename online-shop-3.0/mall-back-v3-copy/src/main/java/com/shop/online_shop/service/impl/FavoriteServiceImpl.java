package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.entity.Favorite;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.ProductStatus;
import com.shop.online_shop.mapper.FavoriteMapper;
import com.shop.online_shop.service.FavoriteService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.vo.FavoriteVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Resource
    private ProductService productService;

    @Override
    @Transactional
    public void addFavorite(Integer buyerAccountId, Integer productId) {
        Product product = productService.getById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 幂等去重：已收藏则跳过
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getBuyerAccountId, buyerAccountId)
                .eq(Favorite::getProductId, productId);
        if (count(wrapper) > 0) {
            return;
        }

        Favorite favorite = new Favorite();
        favorite.setBuyerAccountId(buyerAccountId);
        favorite.setProductId(productId);
        favorite.setCreatedAt(LocalDateTime.now());
        save(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Integer buyerAccountId, Integer productId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getBuyerAccountId, buyerAccountId)
                .eq(Favorite::getProductId, productId);
        remove(wrapper);
    }

    @Override
    public List<FavoriteVO> listFavorites(Integer buyerAccountId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getBuyerAccountId, buyerAccountId)
                .orderByDesc(Favorite::getCreatedAt)
                .orderByDesc(Favorite::getId);
        List<Favorite> favorites = list(wrapper);

        List<FavoriteVO> result = new ArrayList<>();
        for (Favorite favorite : favorites) {
            Product product = productService.getProductWithFill(favorite.getProductId());
            FavoriteVO vo = new FavoriteVO();
            vo.setProductId(favorite.getProductId());
            vo.setCreatedAt(favorite.getCreatedAt());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setMainImageUrl(product.getMainImageUrl());
                vo.setPrice(product.getPrice());
                vo.setCurrentPrice(product.getCurrentPrice());
                vo.setOnSale(ProductStatus.ONLINE == product.getStatus());
            } else {
                vo.setProductName("商品已删除");
                vo.setOnSale(false);
            }
            result.add(vo);
        }
        return result;
    }
}
