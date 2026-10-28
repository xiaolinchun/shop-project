package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddCategoryDTO {

    @NotNull(message = "父分类ID不能为空")
    @Min(value = 0, message = "父分类ID无效")
    private Integer parentId;

    @NotNull(message = "分类名称不能为空")
    private String categoryName;
}
