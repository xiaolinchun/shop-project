package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 支付单状态
 */
public enum PaymentStatus {

    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "支付成功"),
    FAIL("FAIL", "支付失败"),
    REFUNDED("REFUNDED", "已退款");

    @EnumValue
    private final String code;
    private final String desc;

    PaymentStatus(String code, String desc) {
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
}
