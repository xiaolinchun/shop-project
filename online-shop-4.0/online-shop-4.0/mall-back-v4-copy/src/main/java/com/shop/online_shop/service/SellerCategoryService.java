package com.shop.online_shop.service;

import com.shop.online_shop.dto.seller.CategoryCreateDTO;
import com.shop.online_shop.vo.CategoryWithCountVO;

import java.util.List;

public interface SellerCategoryService {

    /** 3.18 分类树（含商品数） */
    List<CategoryWithCountVO> tree(String sellerId);

    /** 3.19 新增分类 */
    Integer add(CategoryCreateDTO dto);

    /** 3.20 重命名分类 */
    void rename(Integer id, String categoryName);

    /** 3.21 删除分类 */
    void delete(Integer id);
}
