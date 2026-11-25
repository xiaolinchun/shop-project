package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 商品状态（v3.0 收敛为两态：在售 / 已下架）
 */
public enum ProductStatus {

    ONLINE("ONLINE", "在售"),
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
