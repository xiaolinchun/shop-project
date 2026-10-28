package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.seller.PublishProductDTO;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.vo.PageResult;
import com.shop.online_shop.vo.ProductListItemVO;

import java.util.List;

public interface ProductService extends IService<Product> {

    Product getCurrentOnlineProduct();

    PageResult<ProductListItemVO> listProducts(Integer categoryId, String sort, Integer page, Integer size);

    PageResult<ProductListItemVO> searchProducts(String keyword, Integer page, Integer size);

    Product getProductDetail(Integer id);

    void publish(PublishProductDTO dto, String sellerId);

    PageResult<Product> listSellerProducts(String sellerId, String status, Integer page, Integer size);

    List<Product> listSellerHistory(String sellerId);

    Product getSellerProduct(Integer id, String sellerId);

    void updateStock(Integer id, Integer stock, String sellerId);

    void changeCategory(Integer id, Integer categoryId, String sellerId);

    void freeze(Integer id, String sellerId);

    void unfreeze(Integer id, String sellerId);

    void offShelf(Integer id, String sellerId);

    void onShelf(Integer id, String sellerId);
}
