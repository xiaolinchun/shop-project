package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模拟渠道记录（对账依据；channelTxnNo 唯一）
 */
@Data
@TableName("mock_channel_txn")
public class MockChannelTxn {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String channelTxnNo;

    private String paymentNo;

    private String channel;

    private BigDecimal amount;

    /** SUCCESS / FAIL */
    private String mockResult;

    private LocalDateTime callbackAt;

    private LocalDateTime createdAt;
}
