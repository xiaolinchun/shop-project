package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IntentStatusEnum {
    QUEUED("QUEUED", "排队中"),
    TRADING("TRADING", "交易中"),
    SUCCESS("SUCCESS", "交易成功"),
    FAILURE("FAILURE", "交易作废"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消"),
    RE_QUEUE("RE_QUEUE", "重新排队");

    @EnumValue
    
    private final String code;
    private final String desc;

    IntentStatusEnum(String code, String desc) {
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
