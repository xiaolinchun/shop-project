package com.shop.online_shop.dto.buyer;

import lombok.Data;

import java.util.List;

/**
 * 发起售后申请请求
 */
@Data
public class AfterSalesApplyDTO {

    private String title;
    private String description;
    private List<String> images;
}
