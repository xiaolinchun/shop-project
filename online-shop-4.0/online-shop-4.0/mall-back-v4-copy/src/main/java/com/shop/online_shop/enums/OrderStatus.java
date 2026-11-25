package com.shop.online_shop.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 订单状态机（v4.0：7 个推进态 + 已取消）
 * CREATED 客户下单 → PENDING_PAYMENT 待支付（自动） → PAID 已支付 → CONFIRMED 商家确认
 * → STOCKED 备货完成 → SHIPPING 开始发货 → COMPLETED 交易完成；CANCELLED 已取消为独立终态。
 * 严格按序、不可跳过、不可回退、不可删除。
 */
public enum OrderStatus {

    CREATED("CREATED", "客户下单"),
    PENDING_PAYMENT("PENDING_PAYMENT", "待支付"),
    PAID("PAID", "已支付"),
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

    /** 已支付且未成交：该集合内的订单明细计入预占量 */
    public boolean isReserving() {
        return this == PAID || this == CONFIRMED || this == STOCKED || this == SHIPPING;
    }
}
