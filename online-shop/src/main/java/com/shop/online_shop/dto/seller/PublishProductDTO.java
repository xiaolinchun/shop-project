package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PublishProductDTO {
    @NotBlank(message = "商品名称必填")
    private String name;
    private String description;
    private String imageUrl;
    @NotNull
    @DecimalMin(value = "0.01",message = "价格必须大于0")
    private BigDecimal price;
}