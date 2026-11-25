package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增分类请求
 */
@Data
public class CategoryCreateDTO {

    /** 一级分类约定传 0 */
    @NotNull(message = "父分类不能为空")
    private Integer parentId;

    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
}
