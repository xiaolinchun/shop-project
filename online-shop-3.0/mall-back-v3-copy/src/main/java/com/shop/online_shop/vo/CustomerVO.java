package com.shop.online_shop.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 5.9 顾客（卖家客户列表项）
 */
@Data
public class CustomerVO {

    private String username;
    private String phone;
    private String defaultTradeLocation;
    private LocalDateTime createdAt;
}
