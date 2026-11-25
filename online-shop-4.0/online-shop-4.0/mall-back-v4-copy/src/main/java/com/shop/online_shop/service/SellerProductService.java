package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.seller.ProductPublishDTO;
import com.shop.online_shop.dto.seller.ProductSaleDTO;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.vo.PageResult;

import java.util.List;

public interface SellerProductService extends IService<Product> {

    /** 3.4 发布商品 */
    void publish(String sellerId, ProductPublishDTO dto);

    /** 3.5 当前商品列表（在售 / 已下架） */
    PageResult<Product> list(String sellerId, String keyword, Integer categoryId,
                             String status, Integer page, Integer size);

    /** 3.6 卖家商品详情 */
    Product detail(String sellerId, Integer id);

    /** 3.7 历史商品（已下架） */
    List<Product> history(String sellerId);

    /** 3.8 修改库存 */
    void updateStock(String sellerId, Integer id, Integer stock);

    /** 3.9 修改分类 */
    void reCategory(String sellerId, Integer id, Integer categoryId);

    /** 3.10 下架商品 */
    void offShelf(String sellerId, Integer id);

    /** 3.11 上架商品 */
    void onShelf(String sellerId, Integer id);

    /** 3.24 设置限时活动价 */
    void setSalePrice(String sellerId, Integer id, ProductSaleDTO dto);
}
