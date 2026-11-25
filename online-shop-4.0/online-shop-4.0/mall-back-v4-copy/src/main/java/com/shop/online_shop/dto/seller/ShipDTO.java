package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 开始发货请求：物流服务商 + 运单号
 */
@Data
public class ShipDTO {

    @NotNull(message = "请选择物流服务商")
    private Integer logisticsProviderId;

    @NotBlank(message = "运单号不能为空")
    private String waybillNo;
}
