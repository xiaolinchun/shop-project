package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.seller.PublishProductDTO;
import com.shop.online_shop.entity.Product;

public interface ProductService extends IService<Product> {
    /** 获取当前在售/冻结的商品，同一时间最多一个 */
    Product getCurrentActiveProduct();
    /** 发布商品 FR07 */
    void publish(PublishProductDTO dto);
    /** 手动冻结 */
    void manualFreeze(Integer productId);
    /** 手动解冻，仅限手动冻结状态，不能解冻交易冻结 */
    void manualUnFreeze(Integer productId);
    /** 手动下架 FR12 */
    void manualOffShelf(Integer productId);
}