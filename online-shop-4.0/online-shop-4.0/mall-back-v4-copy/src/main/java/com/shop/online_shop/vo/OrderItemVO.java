package com.shop.online_shop.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细 VO（下单时快照 + v4.0 入口控制计算字段）
 */
@Data
public class OrderItemVO {

    private Integer id;
    private Integer productId;
    private String productName;
    /** 单价快照（= 下单瞬间适用价） */
    private BigDecimal unitPrice;
    private Integer quantity;
    /** 是否已评价 */
    private Boolean reviewed;
    /** 是否已退款闭环（退货退款 / 仅退款） */
    private Boolean refundClosed;
    /** 是否已有进行中的售后单 */
    private Boolean afterSalesOngoing;
}
