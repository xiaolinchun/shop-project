package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.online_shop.enums.PaymentChannel;
import com.shop.online_shop.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录（同一订单同时仅一笔有效支付）
 */
@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String paymentNo;

    private String orderNo;

    private Integer orderId;

    private PaymentChannel channel;

    /** 金额 = 订单明细快照价合计（无运费） */
    private BigDecimal amount;

    private PaymentStatus status;

    /** 渠道流水号（对账依据） */
    private String channelTxnNo;

    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
}
