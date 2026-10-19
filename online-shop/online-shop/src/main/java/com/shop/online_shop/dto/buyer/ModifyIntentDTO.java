package com.shop.online_shop.dto.buyer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModifyIntentDTO {
    @NotBlank
    private String token;
    @NotBlank
    private String buyerName;
    @NotBlank
    private String phone;
}