package com.shop.online_shop.dto.seller;

import lombok.Data;

/**
 * 确认收到退回件请求（寄回运单号选填）
 */
@Data
public class ReturnReceivedDTO {

    private String returnWaybillNo;
}
