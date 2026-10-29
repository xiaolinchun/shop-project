package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.shop.online_shop.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "product", autoResultMap = true)
public class Product {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String description;

    @TableField(value = "image_urls", typeHandler = JacksonTypeHandler.class)
    private List<String> imageUrls;

    private BigDecimal price;

    private ProductStatus status;

    private Integer stock;

    private LocalDateTime publishedAt;

    private LocalDateTime closedAt;

    private String sellerId;

    private Integer categoryId;

    @TableField(exist = false)
    private String mainImageUrl;

    @TableField(exist = false)
    private String categoryName;
}
