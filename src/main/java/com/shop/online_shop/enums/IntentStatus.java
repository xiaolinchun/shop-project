package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IntentStatus {

    QUEUED("排队中"),
    TRADING("交易中"),
    SUCCESS("成功"),
    CANCELLED("撤销"),
    FAILED("失败");

    @EnumValue
    private final String code;
    private final String desc;

    @JsonValue
    public String getCode() {
        return code;
    }

    IntentStatus(String desc) {
        this.code = name();
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
