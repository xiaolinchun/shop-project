package com.shop.online_shop.dto.buyer;

import lombok.Data;

import java.util.List;

/**
 * 创建订单请求（二选一）
 * 方式一：购物车勾选下单 —— cartItemIds 非空
 * 方式二：商品详情页直接下单 —— productId + quantity
 * v4.0：addressId 为收货地址，不传时自动使用默认地址；地址写入订单快照。
 */
@Data
public class CreateOrderDTO {

    /** 方式一：购物车勾选的条目 ID 列表 */
    private List<Integer> cartItemIds;

    /** 方式二：直接下单的商品 ID */
    private Integer productId;

    /** 方式二：直接下单的数量，≥1 */
    private Integer quantity;

    /** 收货地址 ID；不传自动使用默认地址 */
    private Integer addressId;
}
