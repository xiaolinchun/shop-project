package com.shop.online_shop.service;

import java.util.Collection;
import java.util.Map;

/**
 * 库存派生口径：预占量 / 可卖量相关计算（不落库，实时派生）
 */
public interface ReservedQuantityService {

    /** 预占量 = 已支付且未成交订单（PAID / CONFIRMED / STOCKED / SHIPPING）的明细量之和 */
    int getReservedQuantity(Integer productId);

    Map<Integer, Integer> getReservedQuantities(Collection<Integer> productIds);

    /** 已发货量（展示口径） */
    int getShippedQuantity(Integer productId);
}
