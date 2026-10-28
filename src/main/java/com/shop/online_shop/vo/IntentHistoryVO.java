package com.shop.online_shop.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IntentHistoryVO {

    private String productName;
    private LocalDateTime createdAt;
    private String result;
    private LocalDateTime resultAt;
}
