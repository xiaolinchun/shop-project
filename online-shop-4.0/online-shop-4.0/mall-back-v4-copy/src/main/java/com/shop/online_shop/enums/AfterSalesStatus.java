package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 售后单状态：申请中 → 协商中 → 已闭环
 */
public enum AfterSalesStatus {

    APPLYING("APPLYING", "申请中"),
    NEGOTIATING("NEGOTIATING", "协商中"),
    CLOSED("CLOSED", "已闭环");

    @EnumValue
    private final String code;
    private final String desc;

    AfterSalesStatus(String code, String desc) {
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
