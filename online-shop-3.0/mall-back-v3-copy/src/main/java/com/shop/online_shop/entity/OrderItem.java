package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
}
