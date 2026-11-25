package com.shop.online_shop.vo;

import lombok.Data;

/**
 * 支付回调处理结果
 */
@Data
public class PaymentNotifyResultVO {

    private String orderNo;
    private String status;
    /** 渠道已扣款但未能预占时，是否已自动原路退款 */
    private Boolean refunded;
    /** 幂等 / 可卖量不足等提示信息（成功支付时为 null） */
    private String message;

    public PaymentNotifyResultVO(String orderNo, String status, Boolean refunded, String message) {
        this.orderNo = orderNo;
        this.status = status;
        this.refunded = refunded;
        this.message = message;
    }

    public PaymentNotifyResultVO() {
    }
}
