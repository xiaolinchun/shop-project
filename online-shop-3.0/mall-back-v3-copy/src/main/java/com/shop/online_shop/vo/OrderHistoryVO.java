package com.shop.online_shop.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单历史行（买家订单历史 / 卖家订单视图行，不做独立详情页）
 */
@Data
public class OrderHistoryVO {

    /** 订单主键（行级操作使用，不参与展示） */
    private Integer id;
    private String orderNo;
    private List<OrderItemVO> items;
    private BigDecimal totalAmount;
    /** 订单状态（已取消统一显示「已取消」，不区分取消方） */
    private String status;
    private LocalDateTime createdAt;

    // —— 以下为卖家视图字段，买家端可忽略 ——
    /** 卖家视图：发货被拒标记 */
    private Boolean shipBlocked;
    private Integer needQty;
    private Integer currentStock;
    private Integer shippedQty;
    /** 当前状态停留天数（仅展示） */
    private Integer stayDays;
}
