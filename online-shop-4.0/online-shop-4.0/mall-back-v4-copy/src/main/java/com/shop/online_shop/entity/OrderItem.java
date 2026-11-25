package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细（下单时快照，后续商品变更不回写）
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer orderId;

    private Integer productId;

    /** 商品名快照 */
    private String productName;

    /** 单价快照（= 下单瞬间适用价） */
    private BigDecimal unitPrice;

    private Integer quantity;

    /** 是否已评价（计算字段，控制评价入口） */
    @TableField(exist = false)
    private Boolean reviewed;

    /** 是否已退款闭环（退货退款 / 仅退款，计算字段，控制评价入口） */
    @TableField(exist = false)
    private Boolean refundClosed;

    /** 该明细是否已有进行中的售后单（计算字段，控制售后入口） */
    @TableField(exist = false)
    private Boolean afterSalesOngoing;
}
