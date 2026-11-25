package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.CategoryCreateDTO;
import com.shop.online_shop.entity.Category;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.mapper.CategoryMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.service.SellerCategoryService;
import com.shop.online_shop.vo.CategoryWithCountVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SellerCategoryServiceImpl implements SellerCategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private ProductMapper productMapper;

    @Override
    public List<CategoryWithCountVO> tree(String sellerId) {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getId));

        // 该卖家各分类商品数（全部状态口径）
        Map<Integer, Integer> productCounts = new HashMap<>();
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, sellerId)
                .select(Product::getCategoryId));
        for (Product product : products) {
            if (product.getCategoryId() != null) {
                productCounts.merge(product.getCategoryId(), 1, Integer::sum);
            }
        }

        Map<Integer, CategoryWithCountVO> voMap = new LinkedHashMap<>();
        for (Category category : categories) {
            CategoryWithCountVO vo = new CategoryWithCountVO();
            vo.setId(category.getId());
            vo.setParentId(category.getParentId());
            vo.setCategoryName(category.getCategoryName());
            vo.setProductCount(productCounts.getOrDefault(category.getId(), 0));
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
    public Integer add(CategoryCreateDTO dto) {
        int parentId = dto.getParentId() == null ? 0 : dto.getParentId();
        if (parentId != 0) {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException("父分类不存在");
            }
            if (parent.getParentId() != null && parent.getParentId() != 0) {
                throw new BusinessException("最多支持两级分类");
            }
        }
        String name = requireName(dto.getCategoryName());
        if (countByParentAndName(parentId, name, null) > 0) {
            throw new BusinessException("分类已存在");
        }
        Category category = new Category();
        category.setParentId(parentId);
        category.setCategoryName(name);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional
    public void rename(Integer id, String categoryName) {
        Category category = requireCategory(id);
        String name = requireName(categoryName);
        if (countByParentAndName(category.getParentId() == null ? 0 : category.getParentId(), name, id) > 0) {
            throw new BusinessException("分类已存在");
        }
        category.setCategoryName(name);
        categoryMapper.updateById(category);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        requireCategory(id);
        if (categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id)) > 0) {
            throw new BusinessException("存在子分类，不可删除");
        }
        if (productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id)) > 0) {
            throw new BusinessException("分类下有商品，不可删除");
        }
        categoryMapper.deleteById(id);
    }

    private Category requireCategory(Integer id) {
        Category category = id == null ? null : categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return category;
    }

    private String requireName(String categoryName) {
        if (!StringUtils.hasText(categoryName)) {
            throw new BusinessException("分类名称不能为空");
        }
        return categoryName.trim();
    }

    private long countByParentAndName(Integer parentId, String name, Integer excludeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, parentId)
                .eq(Category::getCategoryName, name)
                .ne(excludeId != null, Category::getId, excludeId);
        return categoryMapper.selectCount(wrapper);
    }
}
