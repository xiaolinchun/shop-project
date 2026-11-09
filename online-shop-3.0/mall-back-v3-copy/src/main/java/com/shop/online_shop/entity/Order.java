package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.online_shop.enums.OrderStatus;
import com.shop.online_shop.vo.OrderItemVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 订单号（日期 + 4 位流水） */
    private String orderNo;

    private OrderStatus status;

    /** 总金额 = Σ(明细单价 × 数量) */
    private BigDecimal totalAmount;

    private Integer buyerAccountId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 买家用户名（计算字段） */
    @TableField(exist = false)
    private String buyerUsername;

    /** 商品清单（行内展开） */
    @TableField(exist = false)
    private List<OrderItemVO> items;
}
