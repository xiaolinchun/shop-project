package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 换货寄出登记请求：寄回旧件运单号 + 寄出新件运单号
 */
@Data
public class ExchangeShipDTO {

    @NotBlank(message = "寄回旧件运单号不能为空")
    private String returnWaybillNo;

    @NotBlank(message = "寄出新件运单号不能为空")
    private String exchangeWaybillNo;
}
