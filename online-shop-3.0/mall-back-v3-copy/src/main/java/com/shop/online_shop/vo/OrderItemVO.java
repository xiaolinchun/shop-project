package com.shop.online_shop.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细（下单时快照，后续商品变更不回写）
 */
@Data
public class OrderItemVO {

    private Integer productId;
    private String productName;
    /** 单价快照（= 下单瞬间适用价） */
    private BigDecimal unitPrice;
    private Integer quantity;
}
