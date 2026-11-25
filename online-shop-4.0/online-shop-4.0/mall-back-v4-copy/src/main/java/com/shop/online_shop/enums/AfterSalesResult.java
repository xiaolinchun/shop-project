package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 售后结果（仅四种）
 */
public enum AfterSalesResult {

    REFUND_AND_RETURN("REFUND_AND_RETURN", "退货退款"),
    REFUND_ONLY("REFUND_ONLY", "仅退款"),
    EXCHANGE("EXCHANGE", "换货"),
    REJECTED("REJECTED", "驳回");

    @EnumValue
    private final String code;
    private final String desc;

    AfterSalesResult(String code, String desc) {
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

    /** 钱已退回：该明细视同未成交，不可评价 */
    public boolean isRefunded() {
        return this == REFUND_AND_RETURN || this == REFUND_ONLY;
    }
}
