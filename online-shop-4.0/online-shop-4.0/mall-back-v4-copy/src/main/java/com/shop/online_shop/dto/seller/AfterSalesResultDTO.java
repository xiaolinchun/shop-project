package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 录入售后结果请求
 */
@Data
public class AfterSalesResultDTO {

    @NotBlank(message = "售后结果不能为空")
    private String result;

    private String remark;
}
