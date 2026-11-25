package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.online_shop.enums.PaymentChannel;
import com.shop.online_shop.enums.RefundStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录（取消退款与售后退款同一口径：全额原路）
 */
@Data
@TableName("refund_record")
public class RefundRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String refundNo;

    private String paymentNo;

    /** 关联售后单号；取消退款为 null */
    private String afterSalesNo;

    private Integer orderId;

    private BigDecimal amount;

    private PaymentChannel channel;

    private RefundStatus status;

    private LocalDateTime createdAt;
}
