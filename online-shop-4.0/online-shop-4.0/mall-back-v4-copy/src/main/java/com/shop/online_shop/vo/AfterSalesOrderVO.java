package com.shop.online_shop.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后单 VO（买家 / 卖家通用展示）
 */
@Data
public class AfterSalesOrderVO {

    private Integer id;
    private String afterSalesNo;
    private String orderNo;
    private Integer itemId;
    private Integer productId;
    private String productName;
    private String title;
    private String description;
    private List<String> images;
    private String status;
    private String result;
    private LocalDateTime resultAt;
    private LocalDateTime createdAt;
    /** 寄回旧件运单号（退货退款 / 换货登记） */
    private String returnWaybillNo;
    /** 寄出新件运单号（换货登记） */
    private String exchangeWaybillNo;
}
