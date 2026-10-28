package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TradeMarkDTO {

    @NotNull(message = "意向ID不能为空")
    private Integer intentId;

    @NotBlank(message = "操作类型不能为空")
    private String operate;
}
