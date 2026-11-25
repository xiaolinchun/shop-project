package com.shop.online_shop.dto.buyer;

import lombok.Data;

/**
 * 发起支付请求
 */
@Data
public class PayRequestDTO {

    /** 支付渠道：BANK_CARD / CREDIT_CARD / ALIPAY / WECHAT_PAY */
    private String channel;
}
