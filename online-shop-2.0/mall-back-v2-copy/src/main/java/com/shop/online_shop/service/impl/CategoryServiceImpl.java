package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.AddCategoryDTO;
import com.shop.online_shop.entity.Category;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.mapper.CategoryMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.service.CategoryService;
import com.shop.online_shop.vo.CategoryTreeVO;
import com.shop.online_shop.vo.CategoryWithCountVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private ProductMapper productMapper;

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

    @Override
    public List<CategoryWithCountVO> getTreeWithProductCount() {
        List<Category> categories = list(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getId));

        Map<Integer, Long> productCountMap = countProductsByCategory();

        Map<Integer, CategoryWithCountVO> voMap = new LinkedHashMap<>();
        for (Category category : categories) {
            CategoryWithCountVO vo = new CategoryWithCountVO();
            vo.setId(category.getId());
            vo.setParentId(category.getParentId());
            vo.setCategoryName(category.getCategoryName());
            vo.setProductCount(productCountMap.getOrDefault(category.getId(), 0L).intValue());
            voMap.put(category.getId(), vo);
        }

        List<CategoryWithCountVO> roots = new ArrayList<>();
        for (CategoryWithCountVO vo : voMap.values()) {
            if (vo.getParentId() == null || vo.getParentId() == 0) {
                roots.add(vo);
            } else {
                CategoryWithCountVO parent = voMap.get(vo.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                }
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public void addCategory(AddCategoryDTO dto) {
        if (dto.getParentId() == null) {
            dto.setParentId(0);
        }
        if (dto.getParentId() != 0) {
            Category parent = getById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException("父分类不存在");
            }
            if (parent.getParentId() != null && parent.getParentId() != 0) {
                throw new BusinessException("只能在一级分类下新增二级分类");
            }
        }

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, dto.getParentId())
                .eq(Category::getCategoryName, dto.getCategoryName().trim());
        if (count(wrapper) > 0) {
            throw new BusinessException("同级分类名称已存在");
        }

        Category category = new Category();
        category.setParentId(dto.getParentId());
        category.setCategoryName(dto.getCategoryName().trim());
        save(category);
    }

    @Override
    @Transactional
    public void renameCategory(Integer id, String categoryName) {
        Category category = getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, category.getParentId())
                .eq(Category::getCategoryName, categoryName.trim())
                .ne(Category::getId, id);
        if (count(wrapper) > 0) {
            throw new BusinessException("同级分类名称已存在");
        }

        category.setCategoryName(categoryName.trim());
        updateById(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        Category category = getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        Set<Integer> categoryIdsToCheck = new HashSet<>();
        categoryIdsToCheck.add(id);
        collectChildIds(id, categoryIdsToCheck);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Product::getCategoryId, categoryIdsToCheck);
        long productCount = productMapper.selectCount(wrapper);
        if (productCount > 0) {
            throw new BusinessException("该分类下存在商品，请先将商品移除或改挂到其他分类");
        }

        LambdaQueryWrapper<Category> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.in(Category::getId, categoryIdsToCheck);
        remove(childWrapper);
    }

    private void collectChildIds(Integer parentId, Set<Integer> result) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, parentId);
        List<Category> children = list(wrapper);
        for (Category child : children) {
            result.add(child.getId());
            collectChildIds(child.getId(), result);
        }
    }

    private Map<Integer, Long> countProductsByCategory() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Product::getCategoryId);
        List<Product> products = productMapper.selectList(wrapper);

        Map<Integer, Long> countMap = new HashMap<>();
        for (Product product : products) {
            countMap.merge(product.getCategoryId(), 1L, Long::sum);
        }
        return countMap;
    }
}
