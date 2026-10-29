package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProductStatus {

    ONLINE("ONLINE", "在售"),
    FROZEN("FROZEN", "已冻结"),
    OFFLINE("OFFLINE", "已下架");

    @EnumValue
    private final String code;
    private final String desc;

    ProductStatus(String code, String desc) {
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
