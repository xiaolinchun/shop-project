package com.shop.online_shop.vo;

import com.shop.online_shop.enums.PaymentChannel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单历史行（买家订单历史；不做独立详情页，明细行内展开）
 */
@Data
public class OrderHistoryVO {

    private Integer id;
    private String orderNo;
    private List<OrderItemVO> items;
    private BigDecimal totalAmount;
    /** 订单状态：7 态 + 已取消 */
    private String status;
    private LocalDateTime createdAt;

    // ↓ v4.0 新增：支付 / 地址 / 物流
    private PaymentChannel paymentChannel;
    private LocalDateTime paidAt;
    private AddressSnapshotVO addressSnapshot;
    private String logisticsProvider;
    private String waybillNo;

    // ↓ v4.0 新增：入口标记
    private Boolean canPay;
    private Boolean canConfirmReceipt;
    private Boolean shippingQuery;
    private Boolean canAfterSales;
    private Boolean canReview;

    // ↓ v4.0 卖家端：商品订单动态指标
    /** 该商品当前预占数量 */
    private Integer reservedQuantity;
    /** 该商品当前可卖数量 */
    private Integer availableQuantity;
    /** 当前状态停留天数 */
    private Integer stayDays;
}
