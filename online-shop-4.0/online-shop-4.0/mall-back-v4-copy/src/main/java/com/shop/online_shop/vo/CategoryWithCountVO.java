package com.shop.online_shop.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 卖家端分类树节点（含该分类下商品数）
 */
@Data
public class CategoryWithCountVO {

    private Integer id;
    private Integer parentId;
    private String categoryName;
    private Integer productCount;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<CategoryWithCountVO> children = new ArrayList<>();
}
