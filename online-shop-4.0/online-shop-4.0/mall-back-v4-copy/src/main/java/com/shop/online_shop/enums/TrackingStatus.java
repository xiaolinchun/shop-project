package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 物流跟踪状态（展示口径，不参与订单状态推进）
 */
public enum TrackingStatus {

    PICKED_UP("PICKED_UP", "揽收"),
    IN_TRANSIT("IN_TRANSIT", "运输中"),
    OUT_FOR_DELIVERY("OUT_FOR_DELIVERY", "派送中"),
    DELIVERED("DELIVERED", "已签收");

    @EnumValue
    private final String code;
    private final String desc;

    TrackingStatus(String code, String desc) {
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
