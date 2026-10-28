package com.shop.online_shop.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryWithCountVO {

    private Integer id;

    private Integer parentId;

    private String categoryName;

    private Integer productCount;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<CategoryWithCountVO> children = new ArrayList<>();
}
