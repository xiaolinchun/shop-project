package com.shop.online_shop.dto.buyer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitIntentDTO {
    @NotBlank(message = "姓名不能为空")
    private String buyerName;
    @NotBlank(message = "联系电话不能为空")
    private String phone;
}