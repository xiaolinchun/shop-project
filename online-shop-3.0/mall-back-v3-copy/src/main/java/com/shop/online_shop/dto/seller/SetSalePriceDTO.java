package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 3.24 设置限时活动价请求
 */
@Data
public class SetSalePriceDTO {

    @NotNull(message = "活动价必须大于0")
    @DecimalMin(value = "0.01", message = "活动价必须大于0")
    private BigDecimal salePrice;

    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime saleStartAt;

    @NotNull(message = "活动结束时间不能为空")
    private LocalDateTime saleEndAt;
}
