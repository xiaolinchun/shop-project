package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 订单状态机（v3.0 新增）
 * CREATED 客户下单 → CONFIRMED 商家确认 → STOCKED 备货完成 → SHIPPING 开始发货 → COMPLETED 交易完成
 * 前四态任一可转 CANCELLED 已取消。严格按序、不可跳过、不可回退、不可删除。
 */
public enum OrderStatus {

    CREATED("CREATED", "客户下单"),
    CONFIRMED("CONFIRMED", "商家确认"),
    STOCKED("STOCKED", "备货完成"),
    SHIPPING("SHIPPING", "开始发货"),
    COMPLETED("COMPLETED", "交易完成"),
    CANCELLED("CANCELLED", "已取消");

    @EnumValue
    private final String code;
    private final String desc;

    OrderStatus(String code, String desc) {
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
