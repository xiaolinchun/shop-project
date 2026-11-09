package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 商品下架原因（v3.0 新增）
 */
public enum OffReason {

    MANUAL("MANUAL", "手动下架"),
    STOCK_OUT("STOCK_OUT", "库存归零自动下架");

    @EnumValue
    private final String code;
    private final String desc;

    OffReason(String code, String desc) {
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
