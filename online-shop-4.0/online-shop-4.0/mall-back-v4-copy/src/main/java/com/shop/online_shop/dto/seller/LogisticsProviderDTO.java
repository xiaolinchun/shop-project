package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增物流服务商请求
 */
@Data
public class LogisticsProviderDTO {

    @NotBlank(message = "物流服务商名称不能为空")
    private String name;
}
