package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PublishProductDTO {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String description;

    private List<String> imageUrls;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "分类不能为空")
    @Min(value = 1, message = "分类ID无效")
    private Integer categoryId;

    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存至少为1")
    private Integer stock;
}
