package com.shop.online_shop.enums;

public enum ProductStatusEnum {
    ONLINE("在售"),
    FROZEN("已冻结"),
    OFFLINE("已下架");
    private final String desc;
    ProductStatusEnum(String desc) { this.desc = desc; }
    public String getDesc(){return desc;}
}