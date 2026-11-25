package com.shop.online_shop.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 卖家端客户列表行
 */
@Data
public class CustomerVO {

    private String username;
    private String phone;
    private String defaultTradeLocation;
    private LocalDateTime createdAt;
}
