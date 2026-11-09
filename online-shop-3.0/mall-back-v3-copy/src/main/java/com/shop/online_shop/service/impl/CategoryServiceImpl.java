package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.entity.Category;
import com.shop.online_shop.mapper.CategoryMapper;
import com.shop.online_shop.service.CategoryService;
import com.shop.online_shop.vo.CategoryTreeVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<CategoryTreeVO> getTree() {
        List<Category> categories = list(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getId));

        Map<Integer, CategoryTreeVO> voMap = new LinkedHashMap<>();
        for (Category category : categories) {
            CategoryTreeVO vo = new CategoryTreeVO();
            vo.setId(category.getId());
            vo.setParentId(category.getParentId());
            vo.setCategoryName(category.getCategoryName());
            voMap.put(category.getId(), vo);
        }

        List<CategoryTreeVO> roots = new ArrayList<>();
        for (CategoryTreeVO vo : voMap.values()) {
            if (vo.getParentId() == null || vo.getParentId() == 0) {
                roots.add(vo);
            } else {
                CategoryTreeVO parent = voMap.get(vo.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                }
            }
        }
        return roots;
    }

    @Override
    public Map<Integer, String> getFullNameMap() {
        List<Category> categories = list(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getId));

        Map<Integer, Category> categoryMap = new HashMap<>();
        for (Category category : categories) {
            categoryMap.put(category.getId(), category);
        }

        Map<Integer, String> fullNameMap = new HashMap<>();
        for (Category category : categories) {
            String fullName = category.getCategoryName();
            Category parent = category.getParentId() == null
                    ? null
                    : categoryMap.get(category.getParentId());
            if (parent != null) {
                fullName = parent.getCategoryName() + " / " + fullName;
            }
            fullNameMap.put(category.getId(), fullName);
        }
        return fullNameMap;
    }

    @Override
    public String getFullName(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        return getFullNameMap().get(categoryId);
    }
}
