package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.entity.CartItem;
import com.shop.online_shop.vo.CartItemVO;

import java.util.List;

public interface CartService extends IService<CartItem> {

    /** 2.7 加入购物车，返回条目 ID */
    Integer addToCart(Integer buyerAccountId, Integer productId, Integer quantity);

    /** 2.8 购物车列表 */
    List<CartItemVO> listCart(Integer buyerAccountId);

    /** 2.9 修改购物车数量（减到 0 等同移除） */
    void updateQuantity(Integer buyerAccountId, Integer id, Integer quantity);

    /** 2.10 移除购物车条目 */
    void removeItem(Integer buyerAccountId, Integer id);

    /** 2.11 购物车批量转收藏，返回成功转入条数 */
    Integer cartToFavorite(Integer buyerAccountId, List<Integer> cartItemIds);
}
