package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.entity.Favorite;
import com.shop.online_shop.vo.FavoriteVO;

import java.util.List;

public interface FavoriteService extends IService<Favorite> {

    /** 2.12 收藏商品（幂等去重） */
    void addFavorite(Integer buyerAccountId, Integer productId);

    /** 2.13 取消收藏（未收藏亦返回成功） */
    void removeFavorite(Integer buyerAccountId, Integer productId);

    /** 2.14 收藏列表（按收藏时间倒序） */
    List<FavoriteVO> listFavorites(Integer buyerAccountId);
}
