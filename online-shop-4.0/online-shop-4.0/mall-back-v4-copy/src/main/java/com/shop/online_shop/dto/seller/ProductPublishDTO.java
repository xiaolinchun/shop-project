package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发布商品请求
 */
@Data
public class ProductPublishDTO {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负")
    @Max(value = 99999, message = "库存不能超过99999")
    private Integer stock;

    @NotNull(message = "请选择二级分类")
    private Integer categoryId;

    private String description;

    private List<String> imageUrls;
}
