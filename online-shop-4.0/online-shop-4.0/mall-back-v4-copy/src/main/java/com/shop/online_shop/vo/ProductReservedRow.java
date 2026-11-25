package com.shop.online_shop.vo;

import lombok.Data;

/**
 * 商品维度的预占量聚合行（Mapper 批量查询用）
 */
@Data
public class ProductReservedRow {

    private Integer productId;
    private Integer quantity;
}
