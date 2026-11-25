package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 退款单状态
 */
public enum RefundStatus {

    PROCESSING("PROCESSING", "退款中"),
    SUCCESS("SUCCESS", "退款成功"),
    FAIL("FAIL", "退款失败");

    @EnumValue
    private final String code;
    private final String desc;

    RefundStatus(String code, String desc) {
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
