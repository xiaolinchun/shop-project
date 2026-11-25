package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.entity.Category;
import com.shop.online_shop.vo.CategoryTreeVO;

import java.util.List;
import java.util.Map;

public interface CategoryService extends IService<Category> {

    /** 2.18 商品分类树 */
    List<CategoryTreeVO> getTree();

    /** 获取分类全名映射（id → "父 / 子"） */
    Map<Integer, String> getFullNameMap();

    String getFullName(Integer categoryId);
}
