package com.shop.online_shop.dto.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付渠道回调请求（4.1 / 模拟渠道）
 */
@Data
public class PaymentNotifyDTO {

    private String paymentNo;
    private String channel;
    private String channelTxnNo;

    /** SUCCESS / FAIL */
    private String result;

    private LocalDateTime paidAt;

    /** 前端模拟页回传金额（可选） */
    private BigDecimal amount;
}
