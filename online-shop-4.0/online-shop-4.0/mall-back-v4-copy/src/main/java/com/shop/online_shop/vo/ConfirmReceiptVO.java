package com.shop.online_shop.vo;

import lombok.Data;

/**
 * 确认收货结果
 */
@Data
public class ConfirmReceiptVO {

    private String orderNo;
    private String status;
    private Boolean stockDeducted;

    public ConfirmReceiptVO(String orderNo, String status, Boolean stockDeducted) {
        this.orderNo = orderNo;
        this.status = status;
        this.stockDeducted = stockDeducted;
    }

    public ConfirmReceiptVO() {
    }
}
