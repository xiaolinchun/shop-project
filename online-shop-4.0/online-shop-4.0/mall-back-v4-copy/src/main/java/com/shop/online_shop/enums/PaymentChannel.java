package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 支付渠道（4 渠道全支持）
 */
public enum PaymentChannel {

    BANK_CARD("BANK_CARD", "银行卡"),
    CREDIT_CARD("CREDIT_CARD", "信用卡"),
    ALIPAY("ALIPAY", "支付宝"),
    WECHAT_PAY("WECHAT_PAY", "微信支付");

    @EnumValue
    private final String code;
    private final String desc;

    PaymentChannel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static PaymentChannel fromCode(String code) {
        if (code != null) {
            for (PaymentChannel channel : values()) {
                if (channel.code.equals(code)) {
                    return channel;
                }
            }
        }
        return null;
    }
}
