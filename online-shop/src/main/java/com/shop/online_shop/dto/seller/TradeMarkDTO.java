package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TradeMarkDTO {
    @NotNull
    private Integer intentId;
    /**
     * SUCCESS成功 / FAILURE作废 / RE_QUEUE重新排队
     */
    @NotNull
    private String operate;
}