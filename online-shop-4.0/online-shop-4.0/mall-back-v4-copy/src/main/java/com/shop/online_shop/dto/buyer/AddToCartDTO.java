package com.shop.online_shop.dto.buyer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加入购物车请求
 */
@Data
public class AddToCartDTO {

    @NotNull(message = "商品ID不能为空")
    private Integer productId;

    /** 数量，默认 1，范围 1~999 */
    private Integer quantity;
}
