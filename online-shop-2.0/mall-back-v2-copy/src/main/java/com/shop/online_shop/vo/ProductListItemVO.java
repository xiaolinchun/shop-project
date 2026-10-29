package com.shop.online_shop.vo;

import com.shop.online_shop.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductListItemVO {

    private Integer id;
    private String name;
    private BigDecimal price;
    private String mainImageUrl;
    private Integer categoryId;
    private String categoryName;
    private ProductStatus status;
    private LocalDateTime publishedAt;
}
