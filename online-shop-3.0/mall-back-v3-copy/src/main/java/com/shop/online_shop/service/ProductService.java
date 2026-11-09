package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.vo.PageResult;
import com.shop.online_shop.vo.ProductListItemVO;

public interface ProductService extends IService<Product> {

    /** 2.1 浏览当前最新在售商品 */
    Product getCurrentOnlineProduct();

    /** 2.2 商品列表（搜索 + 分类 + 排序 + 分页），仅返回在售商品 */
    PageResult<ProductListItemVO> listProducts(String keyword, Integer categoryId, String sort, Integer page, Integer size);

    /** 2.3 商品搜索（强制 keyword 的兼容入口） */
    PageResult<ProductListItemVO> searchProducts(String keyword, Integer page, Integer size);

    /** 2.4 商品详情（已下架商品买家不可访问） */
    Product getBuyerProductDetail(Integer id);

    /** 根据 ID 获取商品（含计算字段填充） */
    Product getProductWithFill(Integer id);

    /** 填充适用价 / 主图 / 分类名等计算字段（活动价口径统一在此处维护） */
    void fillTransientFields(Product product);
}
