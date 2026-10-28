package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.seller.AddCategoryDTO;
import com.shop.online_shop.entity.Category;
import com.shop.online_shop.vo.CategoryTreeVO;
import com.shop.online_shop.vo.CategoryWithCountVO;

import java.util.List;
import java.util.Map;

public interface CategoryService extends IService<Category> {

    List<CategoryTreeVO> getTree();

    Map<Integer, String> getFullNameMap();

    String getFullName(Integer categoryId);

    List<CategoryWithCountVO> getTreeWithProductCount();

    void addCategory(AddCategoryDTO dto);

    void renameCategory(Integer id, String categoryName);

    void deleteCategory(Integer id);
}
